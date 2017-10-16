package com.amirkhawaja.models;

/**
 * Message data structure to be stored in Redis.
 */
public class QueuedMessage {

    private String topic;
    private String data;

    public QueuedMessage(String topic, String data) {
	this.data = data;
	this.topic = topic;
    }

    public String getTopic() {
	return topic;
    }

    public String getData() {
	return data;
    }

    @Override
    public String toString() {
	return "QueuedMessage [topic=" + topic + ", data=" + data + "]";
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((data == null) ? 0 : data.hashCode());
	result = prime * result + ((topic == null) ? 0 : topic.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (!(obj instanceof QueuedMessage))
	    return false;
	QueuedMessage other = (QueuedMessage) obj;
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
	return true;
    }

}
