package com.redcodersgroup.attend_collegeattendancetracker.db.subjects;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "subject", indices = {@Index(value = {"sid"}, unique = true)})
public class SubjectSchema {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "sid")
    private int sid;
    @ColumnInfo(name = "subjectName")
    private String subjectName;
    @ColumnInfo(name = "isLab")
    private boolean isLab;
    @ColumnInfo(name = "classesPerWeek")
    private int classesPerWeek;
    @ColumnInfo(name = "labsPerWeek")
    private int labsPerWeek;
    @ColumnInfo(name = "attendance_criteria")
    private int attendanceCriteria;
    @ColumnInfo(name = "uniqueColorCode")
    private String uniqueColorCode;

    @ColumnInfo(name = "timeTableSetupComplete")
    private boolean isTimeTableSetUpComplete;

    @ColumnInfo(name = "attendanceSetupComplete")
    private boolean isAttendanceSetUpComplete;

    @ColumnInfo(name = "erpSubjectId", defaultValue = "0")
    private int erpSubjectId;

    @ColumnInfo(name = "subjectCode")
    private String subjectCode;

    @ColumnInfo(name = "subjectType")
    private String subjectType;

    @ColumnInfo(name = "credits", defaultValue = "0")
    private int credits;

    @ColumnInfo(name = "teacherName")
    private String teacherName;

    @ColumnInfo(name = "roomNo")
    private String roomNo;

    public SubjectSchema(int sid, String subjectName, boolean isLab, int labsPerWeek, int classesPerWeek, int attendanceCriteria, String uniqueColorCode, boolean isTimeTableSetUpComplete, boolean isAttendanceSetUpComplete) {
        this.sid = sid;
        this.subjectName = subjectName;
        this.isLab = isLab;
        this.labsPerWeek = labsPerWeek;
        this.classesPerWeek = classesPerWeek;
        this.attendanceCriteria = attendanceCriteria;
        this.uniqueColorCode = uniqueColorCode;
        this.isTimeTableSetUpComplete = isTimeTableSetUpComplete;
        this.isAttendanceSetUpComplete = isAttendanceSetUpComplete;
    }

    @Ignore
    public SubjectSchema(String subjectName, boolean isLab, int classesPerWeek, int labsPerWeek, int attendanceCriteria, String uniqueColorCode, boolean isTimeTableSetUpComplete, boolean isAttendanceSetUpComplete) {
        this.subjectName = subjectName;
        this.isLab = isLab;
        this.classesPerWeek = classesPerWeek;
        this.labsPerWeek = labsPerWeek;
        this.attendanceCriteria = attendanceCriteria;
        this.uniqueColorCode = uniqueColorCode;
        this.isTimeTableSetUpComplete = isTimeTableSetUpComplete;
        this.isAttendanceSetUpComplete = isAttendanceSetUpComplete;
    }

    public int getSid() {
        return sid;
    }

    public void setSid(int sid) {
        this.sid = sid;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public boolean isLab() {
        return isLab;
    }

    public void setLab(boolean lab) {
        this.isLab = lab;
    }

    public int getClassesPerWeek() {
        return classesPerWeek;
    }

    public void setClassesPerWeek(int cPerWeek) {
        this.classesPerWeek = cPerWeek;
    }

    public int getAttendanceCriteria() {
        return attendanceCriteria;
    }

    public void setAttendanceCriteria(int attendanceCriteria) {
        this.attendanceCriteria = attendanceCriteria;
    }

    public int getLabsPerWeek() {
        return labsPerWeek;
    }

    public void setLabsPerWeek(int lPerWeek) {
        this.labsPerWeek = lPerWeek;
    }

    public String getUniqueColorCode() {
        return uniqueColorCode;
    }

    public void setUniqueColorCode(String uniqueColorCode) {
        this.uniqueColorCode = uniqueColorCode;
    }

    public boolean isTimeTableSetUpComplete() {
        return isTimeTableSetUpComplete;
    }

    public void setTimeTableSetUpComplete(boolean timeTableSetUpComplete) {
        isTimeTableSetUpComplete = timeTableSetUpComplete;
    }

    public boolean isAttendanceSetUpComplete() {
        return isAttendanceSetUpComplete;
    }

    public void setAttendanceSetUpComplete(boolean attendanceSetUpComplete) {
        isAttendanceSetUpComplete = attendanceSetUpComplete;
    }

    public int getErpSubjectId() {
        return erpSubjectId;
    }

    public void setErpSubjectId(int erpSubjectId) {
        this.erpSubjectId = erpSubjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }
}
