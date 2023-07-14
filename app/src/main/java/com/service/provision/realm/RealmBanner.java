package com.service.provision.realm;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmBanner extends RealmObject {

    private int id;
    @PrimaryKey
    private String banner_id;
    private String title;
    private String url;
    private String tag;

    public RealmBanner() {
    }

    public RealmBanner(int id, String banner_id, String title, String url, String tag) {
        this.id = id;
        this.banner_id = banner_id;
        this.title = title;
        this.url = url;
        this.tag = tag;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBanner_id() {
        return banner_id;
    }

    public void setBanner_id(String banner_id) {
        this.banner_id = banner_id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
