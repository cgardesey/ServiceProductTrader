package com.service.provision.realm;

import com.service.provision.constants.Const;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by 2CLearning on 12/16/2017.
 */

public class RealmChat extends RealmObject implements Comparable<RealmChat> {

    private int id;
    @PrimaryKey
    private String chat_id;
    private String chat_ref_id;
    private String temp_id;
    private String text;
    private String link;
    private String link_title;
    private String link_description;
    private String link_image;
    private String attachment_url;
    private String attachment_type;
    private String attachment_title;
    private int read_by_recipient;
    private int sent_by_customer;
    private String tag;
    private String instructorcourseid;
    private String customer_id;
    private String provider_id;
    private String created_at;
    private String updated_at;


    private String customer_name;
    private String customer_profile_image_url;
    private String provider_name;
    private String provider_title;
    private String provider_first_name;
    private String provider_last_name;
    private String provider_other_name;
    private String provider_image_url;


    private String user_id;
    private String name;
    private String sender_name;
    private String recipient_name;
    private String picture;
    private String sender_picture;
    private String recipient_picture;
    private String reply_name;
    private String reply_body;
    private boolean instructor;


    public RealmChat() {

    }

    public RealmChat(String chat_id, String chat_ref_id, String temp_id, String text, String link, String link_title, String link_description, String link_image, String attachment_url, String attachment_type, String attachment_title, int read_by_recipient, String instructorcourseid, String customer_id, String provider_id, String created_at, String updated_at) {
        this.chat_id = chat_id;
        this.chat_ref_id = chat_ref_id;
        this.temp_id = temp_id;
        this.text = text;
        this.link = link;
        this.link_title = link_title;
        this.link_description = link_description;
        this.link_image = link_image;
        this.attachment_url = attachment_url;
        this.attachment_type = attachment_type;
        this.attachment_title = attachment_title;
        this.read_by_recipient = read_by_recipient;
        this.instructorcourseid = instructorcourseid;
        this.customer_id = customer_id;
        this.provider_id = provider_id;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }

    public RealmChat(int id, String chat_id, String chat_ref_id, String text, String link, String link_title, String link_description, String link_image, String attachment_url, String attachment_type, String attachment_title, int read_by_recipient, int sent_by_customer, String tag, String customer_id, String provider_id, String created_at, String updated_at) {
        this.id = id;
        this.chat_id = chat_id;
        this.chat_ref_id = chat_ref_id;
        this.text = text;
        this.link = link;
        this.link_title = link_title;
        this.link_description = link_description;
        this.link_image = link_image;
        this.attachment_url = attachment_url;
        this.attachment_type = attachment_type;
        this.attachment_title = attachment_title;
        this.read_by_recipient = read_by_recipient;
        this.sent_by_customer = sent_by_customer;
        this.tag = tag;
        this.customer_id = customer_id;
        this.provider_id = provider_id;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }

    public RealmChat(String chat_id, String chat_ref_id, String temp_id, String text, String link, String link_title, String link_description, String link_image, String attachment_url, String attachment_type, String attachment_title, int read_by_recipient, int sent_by_customer, String tag, String customer_id, String provider_id, String created_at, String updated_at, String customer_name, String customer_profile_image_url, String provider_name, String provider_title, String provider_first_name, String provider_last_name, String provider_other_name, String provider_image_url) {
        this.chat_id = chat_id;
        this.chat_ref_id = chat_ref_id;
        this.temp_id = temp_id;
        this.text = text;
        this.link = link;
        this.link_title = link_title;
        this.link_description = link_description;
        this.link_image = link_image;
        this.attachment_url = attachment_url;
        this.attachment_type = attachment_type;
        this.attachment_title = attachment_title;
        this.read_by_recipient = read_by_recipient;
        this.sent_by_customer = sent_by_customer;
        this.tag = tag;
        this.customer_id = customer_id;
        this.provider_id = provider_id;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.customer_name = customer_name;
        this.customer_profile_image_url = customer_profile_image_url;
        this.provider_name = provider_name;
        this.provider_title = provider_title;
        this.provider_first_name = provider_first_name;
        this.provider_last_name = provider_last_name;
        this.provider_other_name = provider_other_name;
        this.provider_image_url = provider_image_url;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getChat_id() {
        return chat_id;
    }

    public void setChat_id(String chat_id) {
        this.chat_id = chat_id;
    }

    public String getChat_ref_id() {
        return chat_ref_id;
    }

    public void setChat_ref_id(String chat_ref_id) {
        this.chat_ref_id = chat_ref_id;
    }

    public String getTemp_id() {
        return temp_id;
    }

    public void setTemp_id(String temp_id) {
        this.temp_id = temp_id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink_title() {
        return link_title;
    }

    public void setLink_title(String link_title) {
        this.link_title = link_title;
    }

    public String getLink_description() {
        return link_description;
    }

    public void setLink_description(String link_description) {
        this.link_description = link_description;
    }

    public String getLink_image() {
        return link_image;
    }

    public void setLink_image(String link_image) {
        this.link_image = link_image;
    }

    public String getAttachment_url() {
        return attachment_url;
    }

    public void setAttachment_url(String attachment_url) {
        this.attachment_url = attachment_url;
    }

    public String getAttachment_type() {
        return attachment_type;
    }

    public void setAttachment_type(String attachment_type) {
        this.attachment_type = attachment_type;
    }

    public String getAttachment_title() {
        return attachment_title;
    }

    public void setAttachment_title(String attachment_title) {
        this.attachment_title = attachment_title;
    }

    public int getRead_by_recipient() {
        return read_by_recipient;
    }

    public void setRead_by_recipient(int read_by_recipient) {
        this.read_by_recipient = read_by_recipient;
    }

    public int getSent_by_customer() {
        return sent_by_customer;
    }

    public void setSent_by_customer(int sent_by_customer) {
        this.sent_by_customer = sent_by_customer;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getInstructorcourseid() {
        return instructorcourseid;
    }

    public void setInstructorcourseid(String instructorcourseid) {
        this.instructorcourseid = instructorcourseid;
    }

    public String getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(String customer_id) {
        this.customer_id = customer_id;
    }

    public String getProvider_id() {
        return provider_id;
    }

    public void setProvider_id(String provider_id) {
        this.provider_id = provider_id;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public String getCustomer_name() {
        return customer_name;
    }

    public void setCustomer_name(String customer_name) {
        this.customer_name = customer_name;
    }

    public String getCustomer_profile_image_url() {
        return customer_profile_image_url;
    }

    public void setCustomer_profile_image_url(String customer_profile_image_url) {
        this.customer_profile_image_url = customer_profile_image_url;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSender_name() {
        return sender_name;
    }

    public void setSender_name(String sender_name) {
        this.sender_name = sender_name;
    }

    public String getRecipient_name() {
        return recipient_name;
    }

    public void setRecipient_name(String recipient_name) {
        this.recipient_name = recipient_name;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getSender_picture() {
        return sender_picture;
    }

    public void setSender_picture(String sender_picture) {
        this.sender_picture = sender_picture;
    }

    public String getRecipient_picture() {
        return recipient_picture;
    }

    public void setRecipient_picture(String recipient_picture) {
        this.recipient_picture = recipient_picture;
    }

    public String getReply_name() {
        return reply_name;
    }

    public void setReply_name(String reply_name) {
        this.reply_name = reply_name;
    }

    public String getReply_body() {
        return reply_body;
    }

    public void setReply_body(String reply_body) {
        this.reply_body = reply_body;
    }

    public boolean isInstructor() {
        return instructor;
    }

    public void setInstructor(boolean instructor) {
        this.instructor = instructor;
    }

    @Override
    public int compareTo(RealmChat realmChat) {
        try {
            if (Const.dateTimeFormat.parse(created_at).getTime() > Const.dateTimeFormat.parse(realmChat.getCreated_at()).getTime()) {
                return 1;
            } else if (Const.dateTimeFormat.parse(created_at).getTime() < Const.dateTimeFormat.parse(realmChat.getCreated_at()).getTime()) {
                return -1;
            } else {
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
