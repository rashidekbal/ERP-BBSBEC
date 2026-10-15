package com.redcodersgroup.attend_collegeattendancetracker.db.timeTable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "timeTable_day")
public class TimeTableDaySchema {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "did")
    private int did;
    @ColumnInfo(name = "dayName")
    private String dayName;

    public TimeTableDaySchema(int did, String dayName) {
        this.did = did;
        this.dayName = dayName;
    }
    @Ignore

    public TimeTableDaySchema(String dayName) {
        this.dayName = dayName;
    }

    public int getDid() {
        return did;
    }

    public void setDid(int did) {
        this.did = did;
    }

    public String getDayName() {
        return dayName;
    }

    public void setDayName(String dayName) {
        this.dayName = dayName;
    }
}
