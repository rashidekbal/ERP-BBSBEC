package com.redcodersgroup.attend_collegeattendancetracker.db.attendance;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;

@Entity(tableName = "attendance",
        foreignKeys = @ForeignKey(entity = SubjectSchema.class, parentColumns = "sid", childColumns = "sid", onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "sid")})
public class AttendanceSchema {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "aid")
    private int aid;
    @ColumnInfo(name = "sid")
    private int sid;
    @ColumnInfo(name = "date")
    private String date;
    @ColumnInfo(name = "status")
    private String status;
    @ColumnInfo(name = "isLab", defaultValue = "0")
    private boolean isLab;

    @ColumnInfo(name = "erpAttendanceId", defaultValue = "0")
    private int erpAttendanceId;

    @ColumnInfo(name = "lectureSessionId", defaultValue = "0")
    private int lectureSessionId;

    @ColumnInfo(name = "hour")
    private String hour;

    @ColumnInfo(name = "rawMark")
    private String rawMark;

    public AttendanceSchema(int aid, int sid, String date, String status, boolean isLab) {
        this.aid = aid;
        this.sid = sid;
        this.date = date;
        this.status = status;
        this.isLab = isLab;
    }

    @Ignore
    public AttendanceSchema(int aid, int sid, String date, String status) {
        this(aid, sid, date, status, false);
    }

    @Ignore
    public AttendanceSchema(int sid, String date, String status, boolean isLab) {
        this.sid = sid;
        this.date = date;
        this.status = status;
        this.isLab = isLab;
    }

    @Ignore
    public AttendanceSchema(int sid, String date, String status) {
        this(sid, date, status, false);
    }

    public int getAid() {
        return aid;
    }

    public void setAid(int aid) {
        this.aid = aid;
    }

    public int getSid() {
        return sid;
    }

    public void setSid(int sid) {
        this.sid = sid;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isLab() {
        return isLab;
    }

    public void setLab(boolean lab) {
        isLab = lab;
    }

    public int getErpAttendanceId() {
        return erpAttendanceId;
    }

    public void setErpAttendanceId(int erpAttendanceId) {
        this.erpAttendanceId = erpAttendanceId;
    }

    public int getLectureSessionId() {
        return lectureSessionId;
    }

    public void setLectureSessionId(int lectureSessionId) {
        this.lectureSessionId = lectureSessionId;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public String getRawMark() {
        return rawMark;
    }

    public void setRawMark(String rawMark) {
        this.rawMark = rawMark;
    }
}
