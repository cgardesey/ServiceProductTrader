package com.service.provision.realm;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmTopic extends RealmObject {

    @PrimaryKey
    private String topic;


    public RealmTopic() {
    }

    public RealmTopic(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
