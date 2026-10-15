package com.redcodersgroup.attend_collegeattendancetracker.db.subjects;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "subjectAdditionalInfo",
        foreignKeys = @ForeignKey(entity = SubjectSchema.class,
        parentColumns = "sid", childColumns = "sid", onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = {"sid"}, unique = true)})
public class SubjectAdditionalInfoSchema {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "said")
    private int said;

    @ColumnInfo(name = "sid")
    private int sid;
    @ColumnInfo(name = "totalClasses")
    private int totalClasses;
    @ColumnInfo(name = "totalLabs")
    private int totalLabs;

    public SubjectAdditionalInfoSchema(int said, int sid, int totalClasses, int totalLabs) {
        this.said = said;
        this.sid = sid;
        this.totalClasses = totalClasses;
        this.totalLabs = totalLabs;
    }

    @Ignore

    public SubjectAdditionalInfoSchema(int sid, int totalClasses, int totalLabs) {
        this.sid = sid;
        this.totalClasses = totalClasses;
        this.totalLabs = totalLabs;
    }

    public int getSaid() {
        return said;
    }

    public void setSaid(int said) {
        this.said = said;
    }

    public int getSid() {
        return sid;
    }

    public void setSid(int sid) {
        this.sid = sid;
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(int totalClasses) {
        this.totalClasses = totalClasses;
    }

    public int getTotalLabs() {
        return totalLabs;
    }

    public void setTotalLabs(int totalLabs) {
        this.totalLabs = totalLabs;
    }
}
