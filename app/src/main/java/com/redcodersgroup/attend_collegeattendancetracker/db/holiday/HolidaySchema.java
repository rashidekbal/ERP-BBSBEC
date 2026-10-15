package com.redcodersgroup.attend_collegeattendancetracker.db.holiday;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "holiday")
public class HolidaySchema {
    @PrimaryKey(autoGenerate = true)
    private int hid;
    @ColumnInfo(name = "eventName")
    private String eventName;
    @ColumnInfo(name="startDate")
    private long startDate;
    @ColumnInfo(name="endDate")
    private long endDate;
    @ColumnInfo(name = "reminderSchedule")
    private String reminderSchedule;

    public HolidaySchema(int hid, String eventName, long startDate, long endDate,String reminderSchedule) {
        this.hid = hid;
        this.eventName = eventName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reminderSchedule=reminderSchedule;
    }
    @Ignore
    public HolidaySchema(String eventName, long startDate, long endDate,String reminderSchedule) {
        this.eventName = eventName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reminderSchedule=reminderSchedule;
    }

    public int getHid() {
        return hid;
    }

    public void setHid(int hid) {
        this.hid = hid;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public String getReminderSchedule() {
        return reminderSchedule;
    }

    public void setReminderSchedule(String reminderSchedule) {
        this.reminderSchedule = reminderSchedule;
    }
}


