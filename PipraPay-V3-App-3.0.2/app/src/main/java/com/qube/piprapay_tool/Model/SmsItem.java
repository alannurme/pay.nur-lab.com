package com.qube.piprapay_tool.Model;

import org.json.JSONException;
import org.json.JSONObject;

public class SmsItem {
    private String id;
    private String sender;
    private String message;
    private String simSlot;
    private String timestamp;

    public SmsItem(String id, String sender, String message, String simSlot, String timestamp) {
        this.id = id;
        this.sender = sender;
        this.message = message;
        this.simSlot = simSlot;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public String getSimSlot() {
        return simSlot;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("sender", sender);
            json.put("message", message);
            json.put("simSlot", simSlot);
            json.put("timestamp", timestamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static SmsItem fromJson(JSONObject json) {
        return new SmsItem(
                json.optString("id", String.valueOf(System.currentTimeMillis())),
                json.optString("sender", ""),
                json.optString("message", ""),
                json.optString("simSlot", "1"),
                json.optString("timestamp", String.valueOf(System.currentTimeMillis() / 1000))
        );
    }
}
