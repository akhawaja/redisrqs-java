package com.amirkhawaja;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import redis.clients.jedis.Jedis;

import java.net.URI;

public class RedisConnectionFactory extends BasePooledObjectFactory<Jedis> {

    private final String uri;

    public RedisConnectionFactory(String uri) {
        super();
        this.uri = uri;
    }

    @Override
    public Jedis create() throws Exception {
        final URI uri = URI.create(this.uri);
        return new Jedis(uri);
    }

    @Override
    public PooledObject<Jedis> wrap(Jedis jedis) {
        if (!jedis.isConnected()) {
            jedis.connect();
        }

        return new DefaultPooledObject<>(jedis);
    }

    @Override
    public void activateObject(PooledObject<Jedis> p) throws Exception {
        if (!p.getObject().isConnected()) {
            p.getObject().connect();
        }
    }

    @Override
    public void passivateObject(PooledObject<Jedis> p) throws Exception {
        if (p.getObject().isConnected()) {
            p.getObject().close();
        }
    }

}
