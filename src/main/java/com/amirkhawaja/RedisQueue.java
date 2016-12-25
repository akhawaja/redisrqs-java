package com.amirkhawaja;

import com.amirkhawaja.models.Message;
import com.amirkhawaja.models.QueuedMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisDataException;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RedisQueue implements Closeable {

    private static final String WORKING_QUEUE = "queue:working";
    private static final String PENDING_QUEUE = "queue:pending";
    private static final String VALUES_QUEUE = "queue:values";

    private final Map<String, String> commandMap = new ConcurrentHashMap<>(5);
    private final GenericObjectPool<Jedis> redisPool;
    private final Gson serializer = new GsonBuilder().create();

    public RedisQueue(String uri) {
        redisPool = new GenericObjectPool<>(new RedisConnectionFactory(uri));
        loadCommands();
    }

    /**
     * Get the number of messages in the pending queue.
     *
     * @return Number of messages.
     */
    public long getPendingQueueSize() {
        Jedis redis = null;

        try {
            redis = redisPool.borrowObject();
            return redis.llen(PENDING_QUEUE);
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (redis != null) {
                redisPool.returnObject(redis);
            }
        }

        return 0;
    }

    /**
     * Get the number of messages in the working queue.
     *
     * @return Number of messages.
     */
    public long getWorkingQueueSize() {
        Jedis redis = null;

        try {
            redis = redisPool.borrowObject();
            return redis.zcount(WORKING_QUEUE, "-inf", "+inf");
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (redis != null) {
                redisPool.returnObject(redis);
            }
        }

        return 0;
    }

    /**
     * Drain all the queues.
     */
    public void drainQueues() {
        Jedis redis = null;

        try {
            redis = redisPool.borrowObject();
            final Transaction transaction = redis.multi();
            transaction.del(WORKING_QUEUE);
            transaction.del(PENDING_QUEUE);
            transaction.del(VALUES_QUEUE);
            transaction.exec();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (redis != null) {
                redisPool.returnObject(redis);
            }
        }
    }

    /**
     * Add a message to the queue.
     *
     * @param topic   The topic this message belongs to.
     * @param message The message.
     */
    public void enqueue(String topic, String message) {
        Jedis redis = null;

        try {
            redis = redisPool.borrowObject();

            final String json = serializer.toJson(new QueuedMessage(topic, message));
            redis.evalsha(commandMap.get(Commands.ENQUEUE), 1, UUID.randomUUID().toString(), json);
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (redis != null) {
                redisPool.returnObject(redis);
            }
        }
    }

    /**
     * Take a message from the queue.
     *
     * @return models.queues.Message; or null;
     */
    @SuppressWarnings("unchecked")
    public Message dequeue() {
        Jedis redis = null;

        try {
            redis = redisPool.borrowObject();

            try {
                final long now = Instant.now().atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
                final ArrayList<String> result = (ArrayList<String>) redis.evalsha(
                        commandMap.get(Commands.DEQUEUE), 1, "timestamp", String.valueOf(now));

                if (result == null) { // Nothing to pick up
                    return null;
                }

                final QueuedMessage message = serializer.fromJson(result.get(1), QueuedMessage.class);

                return new Message(UUID.fromString(result.get(0)), message.getTopic(), message.getData());
            } catch (JsonSyntaxException e) {
                throw new RuntimeException("Unable to parse the JSON data.");
            } catch (JedisDataException e) {
                return null; // The queue must be empty.
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (redis != null) {
                redisPool.returnObject(redis);
            }
        }

        return null;
    }

    /**
     * Release a message from the queue.
     *
     * @param uuid The UUID of the message to release.
     */
    public void release(UUID uuid) {
        Jedis redis = null;

        try {
            redis = redisPool.borrowObject();
            redis.evalsha(commandMap.get(Commands.RELEASE), 1, "uuid", String.valueOf(uuid));
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (redis != null) {
                redisPool.returnObject(redis);
            }
        }
    }

    /**
     * Move the unprocessed messages back to the working queue.
     */
    public void sweep() {
        final long now = Instant.now().atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
        final long interval = 60000;
        Jedis redis = null;

        try {
            redis = redisPool.borrowObject();
            redis.evalsha(commandMap.get(Commands.SWEEP), 2,
                    "timestamp", "interval", String.valueOf(now), String.valueOf(interval));
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (redis != null) {
                redisPool.returnObject(redis);
            }
        }
    }

    /**
     * Requeue a message.
     *
     * @param uuid The UUID of the message to requeue.
     */
    public void requeue(String uuid) {
        Jedis redis = null;

        try {
            redis = redisPool.borrowObject();
            redis.evalsha(commandMap.get(Commands.REQUEUE), 1, "uuid", uuid);
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (redis != null) {
                redisPool.returnObject(redis);
            }
        }
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     * <p>
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        redisPool.close();
    }

    /**
     * Load the commands into Redis.
     */
    private void loadCommands() {
        Jedis redis = null;
        try {
            redis = redisPool.borrowObject();
            commandMap.put(Commands.ENQUEUE, redis.scriptLoad(LuaScripts.ENQUEUE));
            commandMap.put(Commands.DEQUEUE, redis.scriptLoad(LuaScripts.DEQUEUE));
            commandMap.put(Commands.RELEASE, redis.scriptLoad(LuaScripts.RELEASE));
            commandMap.put(Commands.REQUEUE, redis.scriptLoad(LuaScripts.REQUEUE));
            commandMap.put(Commands.SWEEP, redis.scriptLoad(LuaScripts.SWEEP));
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (redis != null) {
                redisPool.returnObject(redis);
            }
        }
    }

    /**
     * The name of the commands we need for the queue.
     */
    private class Commands {

        static final String ENQUEUE = "enqueue";
        static final String DEQUEUE = "dequeue";
        static final String RELEASE = "release";
        static final String REQUEUE = "requeue";
        static final String SWEEP = "sweep";

    }

    private class LuaScripts {

        static final String ENQUEUE = "redis.call('LPUSH', '" + PENDING_QUEUE + "', KEYS[1])\n" +
                "redis.call('HSET', '" + VALUES_QUEUE + "', KEYS[1], ARGV[1])\n" +
                "return {KEYS[1], ARGV[1]}";
        static final String DEQUEUE = "local uuid = redis.call('RPOP', '" + PENDING_QUEUE + "')\n" +
                "if type(uuid) == 'boolean' then\n" +
                "  return nil\n" +
                "else\n" +
                "    redis.call('ZADD', '" + WORKING_QUEUE + "', ARGV[1], uuid)\n" +
                "    if redis.call('HEXISTS', '" + VALUES_QUEUE + "', uuid) == 1 then\n" +
                "      local payload = redis.call('HGET', '" + VALUES_QUEUE + "', uuid)\n" +
                "      return {uuid, payload}\n" +
                "    else\n" +
                "      return nil\n" +
                "    end\n" +
                "end";
        static final String RELEASE = "redis.call('ZREM', '" + WORKING_QUEUE + "', ARGV[1])\n" +
                "redis.call('HDEL', '" + VALUES_QUEUE + "', ARGV[1])\n" +
                "return ARGV[1]";
        static final String REQUEUE = "redis.call('ZREM', '" + WORKING_QUEUE + "', ARGV[1])\n" +
                "redis.call('LPUSH', '" + PENDING_QUEUE + "', ARGV[1])\n" +
                "return ARGV[1]";
        static final String SWEEP = "local uuids = redis.call('ZRANGEBYSCORE', '" + WORKING_QUEUE + "', 0, ARGV[1] - ARGV[2])\n" +
                "for _, key in ipairs(uuids) do\n" +
                "  redis.call('LPUSH', '" + PENDING_QUEUE + "', key)\n" +
                "  redis.call('ZREM', '" + WORKING_QUEUE + "', key)\n" +
                "end";

    }

}
