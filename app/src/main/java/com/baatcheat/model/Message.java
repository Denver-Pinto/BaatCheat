package com.baatcheat.model;

import androidx.annotation.NonNull;

import java.util.Date;

public class Message {
    private String message, senderID, receiverID;
    private Date timeStamp;

    public Message(){

    }

    public Message(String message, String senderID, String receiverID, Date timeStamp) {
        this.message = message;
        this.senderID = senderID;
        this.receiverID = receiverID;
        this.timeStamp = timeStamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderID() {
        return senderID;
    }

    public void setSenderID(String senderID) {
        this.senderID = senderID;
    }

    public String getreceiverID() {
        return receiverID;
    }

    public void setreceiverID(String receiverID) {
        this.receiverID = receiverID;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    @NonNull
    @Override
    public String toString() {
        return "Message{" +
                "message='" + message + '\'' +
                ", senderID='" + senderID + '\'' +
                ", receiverID='" + receiverID + '\'' +
                ", timeStamp=" + timeStamp +
                '}';
    }
}
