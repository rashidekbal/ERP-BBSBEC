package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.annotations.SerializedName;

public class MarkNotificationReadRequest {

    @SerializedName("id")
    private int id;

    public MarkNotificationReadRequest(int id) {
        this.id = id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}
