package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class NotificationsInboxResponse {

    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Data data;

    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public Data getData() { return data; }

    public static class Data {
        @SerializedName("count")
        private int count;

        @SerializedName("next")
        private String next;

        @SerializedName("previous")
        private String previous;

        @SerializedName("results")
        private List<NotificationItem> results = new ArrayList<>();

        public int getCount() { return count; }
        public String getNext() { return next; }
        public String getPrevious() { return previous; }
        public List<NotificationItem> getResults() {
            return results != null ? results : new ArrayList<>();
        }
    }

    public static class NotificationItem {
        @SerializedName("id")
        private int id;

        @SerializedName("title")
        private String title;

        @SerializedName("message")
        private String message;

        @SerializedName("category")
        private String category;

        @SerializedName("is_read")
        private boolean isRead;

        @SerializedName("created_at")
        private String createdAt;

        public NotificationItem() {}

        public NotificationItem(int id, String title, String message, String category, boolean isRead, String createdAt) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.category = category;
            this.isRead = isRead;
            this.createdAt = createdAt;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getTitle() { return title != null ? title : ""; }
        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message != null ? message : ""; }
        public void setMessage(String message) { this.message = message; }

        public String getCategory() { return category != null && !category.isEmpty() ? category : "General"; }
        public void setCategory(String category) { this.category = category; }

        public boolean isRead() { return isRead; }
        public void setRead(boolean read) { isRead = read; }

        public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}
