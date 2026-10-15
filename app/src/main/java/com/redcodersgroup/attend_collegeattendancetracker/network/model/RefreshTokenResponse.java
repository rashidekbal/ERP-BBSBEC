package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.annotations.SerializedName;

public class RefreshTokenResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("data")
    private Data data;

    public int getStatus() {
        return status;
    }

    public Data getData() {
        return data;
    }

    public static class Data {
        @SerializedName("access")
        private String access;

        @SerializedName("token")
        private String token;

        @SerializedName("expires")
        private String expires;

        public String getAccessToken() {
            return access != null && !access.isEmpty() ? access : token;
        }

        public String getExpires() {
            return expires;
        }
    }
}
