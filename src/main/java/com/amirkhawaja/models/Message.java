package com.amirkhawaja.models;

import java.util.UUID;

/**
 * Represents a message from the queue.
 */
public class Message {

    private UUID uuid;
    private String topic;
    private String data;

    public Message(UUID uuid, String topic, String data) {
	this.uuid = uuid;
	this.topic = topic;
	this.data = data;
    }

    public UUID getUuid() {
	return uuid;
    }

    public String getTopic() {
	return topic;
    }

    public String getData() {
	return data;
    }

    @Override
    public String toString() {
	return "Message [uuid=" + uuid + ", topic=" + topic + ", data=" + data + "]";
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((data == null) ? 0 : data.hashCode());
	result = prime * result + ((topic == null) ? 0 : topic.hashCode());
	result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (!(obj instanceof Message))
	    return false;
	Message other = (Message) obj;
	if (data == null) {
	    if (other.data != null)
		return false;
	} else if (!data.equals(other.data))
	    return false;
	if (topic == null) {
	    if (other.topic != null)
		return false;
	} else if (!topic.equals(other.topic))
	    return false;
	if (uuid == null) {
	    if (other.uuid != null)
		return false;
	} else if (!uuid.equals(other.uuid))
	    return false;
	return true;
    }

}
