package com.redcodersgroup.attend_collegeattendancetracker.db.assignment;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;

@Entity(tableName = "assignment",foreignKeys = @ForeignKey(
        entity = SubjectSchema.class,
        parentColumns = "sid",
        childColumns = "sid",
        onDelete = ForeignKey.CASCADE
), indices = {@Index(value = {"sid"}, unique = false)})
public class AssignmentSchema {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "aid")
    private int aid;
    @ColumnInfo(name = "sid")
    private int sid;
    @ColumnInfo(name ="subjectName")
    private String subjectName;
    @ColumnInfo(name="assignmentTitle")
    private String assignmentTitle;
    @ColumnInfo(name="assignmentDesc")
    private String assignmentDesc;

    @ColumnInfo(name = "dueDate")
    private long dueDate;
    @ColumnInfo(name = "reminderSchedule")
    private String reminderSchedule;
    @ColumnInfo(name = "submitted")
    private boolean submitted;


    public AssignmentSchema(int aid, int sid, String assignmentTitle, String subjectName, String assignmentDesc, long dueDate, String reminderSchedule,boolean submitted) {
        this.aid = aid;
        this.sid = sid;
        this.assignmentTitle = assignmentTitle;
        this.subjectName = subjectName;
        this.assignmentDesc = assignmentDesc;
        this.dueDate = dueDate;
        this.reminderSchedule = reminderSchedule;
        this.submitted=submitted;
    }

    @Ignore

    public AssignmentSchema(int sid, String subjectName, String assignmentTitle, String assignmentDesc, long dueDate, String reminderSchedule,boolean submitted) {
        this.sid = sid;
        this.subjectName = subjectName;
        this.assignmentTitle = assignmentTitle;
        this.assignmentDesc = assignmentDesc;
        this.dueDate = dueDate;
        this.reminderSchedule = reminderSchedule;
        this.submitted=submitted;
    }

    public int getAid() {
        return aid;
    }

    public void setAid(int aid) {
        this.aid = aid;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getSid() {
        return sid;
    }

    public void setSid(int sid) {
        this.sid = sid;
    }

    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    public void setAssignmentTitle(String assignmentTitle) {
        this.assignmentTitle = assignmentTitle;
    }

    public String getAssignmentDesc() {
        return assignmentDesc;
    }

    public void setAssignmentDesc(String assignmentDesc) {
        this.assignmentDesc = assignmentDesc;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public String getReminderSchedule() {
        return reminderSchedule;
    }

    public void setReminderSchedule(String reminderSchedule) {
        this.reminderSchedule = reminderSchedule;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }
}
