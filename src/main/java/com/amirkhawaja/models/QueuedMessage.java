package com.amirkhawaja.models;

import lombok.Getter;

/**
 * Message data structure to be stored in Redis.
 */
@Getter
public class QueuedMessage {

    private String topic;
    private String data;

    public QueuedMessage(String topic, String data) {
        this.data = data;
        this.topic = topic;
    }

}
