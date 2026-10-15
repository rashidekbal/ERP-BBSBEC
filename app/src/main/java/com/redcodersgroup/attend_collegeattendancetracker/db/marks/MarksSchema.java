package com.redcodersgroup.attend_collegeattendancetracker.db.marks;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;

@Entity(tableName = "marks",foreignKeys = @ForeignKey(
        entity = SubjectSchema.class,
        parentColumns = "sid",
        childColumns = "sid",
        onDelete = ForeignKey.CASCADE
),indices = {@Index(value = {"sid"},unique = true)})
public class MarksSchema {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "marksId")
    private int marksId;
    @ColumnInfo(name = "sid")
    private int sid;
    @ColumnInfo(name = "subjectName")
    private String subjectName;
    @ColumnInfo(name = "mid1")
    private double mid1;
    @ColumnInfo(name = "mid2")
    private double mid2;
    @ColumnInfo(name = "mid3")
    private double mid3;

    public MarksSchema(int marksId, int sid, String subjectName, double mid1, double mid2, double mid3) {
        this.marksId = marksId;
        this.sid = sid;
        this.subjectName = subjectName;
        this.mid1 = mid1;
        this.mid2 = mid2;
        this.mid3 = mid3;
    }

    @Ignore

    public MarksSchema(int sid, String subjectName, double mid1, double mid2, double mid3) {
        this.sid = sid;
        this.subjectName = subjectName;
        this.mid1 = mid1;
        this.mid2 = mid2;
        this.mid3 = mid3;
    }


    public int getMarksId() {
        return marksId;
    }

    public void setMarksId(int marksId) {
        this.marksId = marksId;
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

    public double getMid1() {
        return mid1;
    }

    public void setMid1(double mid1) {
        this.mid1 = mid1;
    }

    public double getMid2() {
        return mid2;
    }

    public void setMid2(double mid2) {
        this.mid2 = mid2;
    }

    public double getMid3() {
        return mid3;
    }

    public void setMid3(double mid3) {
        this.mid3 = mid3;
    }
}
