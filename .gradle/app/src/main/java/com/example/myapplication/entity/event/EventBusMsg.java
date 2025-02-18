package com.example.myapplication.entity.event;


import com.example.myapplication.enums.EventBusEnum;

public class EventBusMsg<T> {
    private EventBusEnum tagEnum;
    private T t;

    public EventBusMsg() {
    }

    public EventBusMsg(EventBusEnum tagEnum, T t) {
        this.tagEnum = tagEnum;
        this.t = t;
    }

    public EventBusEnum getTagEnum() {
        return tagEnum;
    }

    public void setTagEnum(EventBusEnum tagEnum) {
        this.tagEnum = tagEnum;
    }

    public T getT() {
        return t;
    }

    public void setT(T t) {
        this.t = t;
    }
}
