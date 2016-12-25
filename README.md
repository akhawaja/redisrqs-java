Redis RQS (Reliable Queueing System) - Java Version
===================================================

# Introduction
A Java library designed to build a very fast queuing system that guarantees **at least once and at most once delivery**. To achieve this objective, RedisRQS uses Redis as the data store and supports a 1:1 Publisher to Subscriber relationship. When a message arrives, RedisRQS guarantees that only one Subscriber will ever receive a message for processing regardless of the number of Subscribers listening for the same topic in the queue. This should allow a system to scale out the number of processors based on work load.

The way this system handles queuing and dequeuing messages is via Lua scripts. This provides an additional guarantee that each method call to RedisRQS will be a single network call to Redis. Furthermore, RedisRQS also guarantees that messages will never be lost due to a Subscriber unable to finish processing or crashing while processing a message. The queue sweeper will periodically put messages back in to the queue for processing when it runs. You can change the frequency of the sweep in the options.

It is important to note that at this time, there is no support for poison messages. If a message is incomplete or will always crash a Subscriber, the message (poison) will be put back in the pending queue for processing by another Subscriber. This is on the roadmap and will be made available at a future date. As all things open source, your contributions are welcome.

When using this library, three lists will be created:

- Pending: named `redisrqs:pending`
- Working: named `redisrqs:working`
- Values: named `redisrqs:values`

Note: There is a [NodeJS version](https://github.com/akhawaja/redisrqs) of this library as well.

# Install
TODO

# Quick start
At this time, this library is not made available in Maven Central. You can build this locally by issuing the command:

``` bash
$> make package
```

Alternatively, you can use the following Maven command:

``` bash
$> mvn clean compile package
```

Either of these two steps will create a `jar` file you can include in your project. 

The following is an example of how you can use this library:

``` java
private final static String CONNECTION_URI = "redis://localhost:6379/1";

private static void main(String[] args) {
    final HashMap<String, String> options = new HashMap<>();
    options.put("sweepInterval", "60000"); // Sweep the queue every 1-minute
    
    final com.amirkhawaja.RedisQueue queue = new com.amirkhawaja.RedisQueue(CONNECTION_URI, options);
    final String data = "This is a test";
    final String topic = "Test Topic";

    // Queue the message
    queue.enqueue(topic, data);
    
    // Dequeue the message
    final com.amirkhawaja.models.Message message = queue.dequeue();
    
    // Do some work...then remove the message from the queue
    queue.release(message.getUuid());
     
    // Dispose the queue when you are done
    queue.close();
}
```

# Internal Redis Lists
## Pending
When a message is queued it is put in this list. This list will only contain
the UUID of the message. The actual message value lives in the Values list.

## Working
This is where the currently processed message(s) will be stored.

## Values
This is where the actual message will live. Each message is identified
by a UUID. When a message is de-queued, only the UUID is moved from the
Pending list to the Working list.

# History
- 1.0.0: Initial public release.
