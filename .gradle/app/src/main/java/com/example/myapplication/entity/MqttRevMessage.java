package com.example.myapplication.entity;

import java.io.Serializable;

public class MqttRevMessage implements Serializable {

    private String topic;
    private String msg;

    public MqttRevMessage() {
    }

    public MqttRevMessage(String topic, String msg) {
        this.topic = topic;
        this.msg = msg;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
