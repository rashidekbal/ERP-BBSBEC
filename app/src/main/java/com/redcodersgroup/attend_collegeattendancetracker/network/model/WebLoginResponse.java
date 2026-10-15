package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.annotations.SerializedName;

public class WebLoginResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Data data;

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Data getData() {
        return data;
    }

    public static class Data {
        @SerializedName("token")
        private String token;

        @SerializedName("access")
        private String access;

        @SerializedName("refresh")
        private String refresh;

        @SerializedName("expires")
        private String expires;

        @SerializedName("profile")
        private Profile profile;

        public String getToken() {
            return token != null && !token.isEmpty() ? token : access;
        }

        public String getRefresh() {
            return refresh;
        }

        public String getExpires() {
            return expires;
        }

        public Profile getProfile() {
            return profile;
        }
    }

    public static class Profile {
        @SerializedName("id")
        private int id;

        @SerializedName("name")
        private String name;

        @SerializedName("designation")
        private String designation;

        @SerializedName("mobileno")
        private String mobileNo;

        @SerializedName("email")
        private String email;

        @SerializedName("photo")
        private String photo;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDesignation() {
            return designation;
        }

        public String getMobileNo() {
            return mobileNo;
        }

        public String getEmail() {
            return email;
        }

        public String getPhoto() {
            return photo;
        }
    }
}
