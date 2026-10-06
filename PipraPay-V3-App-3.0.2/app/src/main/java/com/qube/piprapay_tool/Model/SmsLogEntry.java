package com.qube.piprapay_tool.Model;

import java.io.Serializable;

public class SmsLogEntry implements Serializable {
    private long id;
    private String sender;
    private String message;
    private String simSlot;
    private String timestamp;
    private String status; // SENT, QUEUED, ERROR, FILTERED
    private String responseMessage;

    public SmsLogEntry() {}

    public SmsLogEntry(String sender, String message, String simSlot, String timestamp, String status, String responseMessage) {
        this.sender = sender;
        this.message = message;
        this.simSlot = simSlot;
        this.timestamp = timestamp;
        this.status = status;
        this.responseMessage = responseMessage;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSender() { return sender != null ? sender : ""; }
    public void setSender(String sender) { this.sender = sender; }

    public String getMessage() { return message != null ? message : ""; }
    public void setMessage(String message) { this.message = message; }

    public String getSimSlot() { return simSlot != null ? simSlot : "1"; }
    public void setSimSlot(String simSlot) { this.simSlot = simSlot; }

    public String getTimestamp() { return timestamp != null ? timestamp : ""; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status != null ? status : "SENT"; }
    public void setStatus(String status) { this.status = status; }

    public String getResponseMessage() { return responseMessage != null ? responseMessage : ""; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }
}