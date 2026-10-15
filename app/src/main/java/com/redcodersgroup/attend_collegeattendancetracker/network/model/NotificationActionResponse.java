package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.annotations.SerializedName;

public class NotificationActionResponse {

    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    public int getStatus() { return status; }
    public String getMessage() { return message; }
}
