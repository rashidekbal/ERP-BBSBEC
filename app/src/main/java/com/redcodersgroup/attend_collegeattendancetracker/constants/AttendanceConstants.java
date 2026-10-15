package com.redcodersgroup.attend_collegeattendancetracker.constants;

public class AttendanceConstants {
    public static final String PRESENT = "PRESENT";
    public static final String ABSENT = "ABSENT";
    public static final String CANCELLED = "CANCELLED";

    // Legacy values
    public static final String ATTENDED = "ATTENDED";
    public static final String MISSED = "MISSED";

    public static boolean isPresent(String status) {
        if (status == null) return false;
        return PRESENT.equalsIgnoreCase(status) || ATTENDED.equalsIgnoreCase(status);
    }

    public static boolean isAbsent(String status) {
        if (status == null) return false;
        return ABSENT.equalsIgnoreCase(status) || MISSED.equalsIgnoreCase(status);
    }

    public static boolean isCancelled(String status) {
        if (status == null) return false;
        return CANCELLED.equalsIgnoreCase(status);
    }
}
