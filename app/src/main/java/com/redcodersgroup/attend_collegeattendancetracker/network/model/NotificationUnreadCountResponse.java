package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.annotations.SerializedName;

public class NotificationUnreadCountResponse {

    @SerializedName("status")
    private int status;

    @SerializedName("data")
    private Data data;

    public int getStatus() { return status; }
    public Data getData() { return data; }

    public static class Data {
        @SerializedName("count")
        private int count;

        public int getCount() { return count; }
    }
}
