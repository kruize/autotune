package com.autotune.common.data.result;

public class RecommendationNotification {
    private String type;
    private String message;

    public RecommendationNotification(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
