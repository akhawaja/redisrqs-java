package com.amirkhawaja.models;

import lombok.Getter;

import java.util.UUID;

/**
 * Represents a message from the queue.
 */
@Getter
public class Message {

    private UUID uuid;
    private String topic;
    private String data;

    public Message(UUID uuid, String topic, String data) {
        this.uuid = uuid;
        this.topic = topic;
        this.data = data;
    }

}
