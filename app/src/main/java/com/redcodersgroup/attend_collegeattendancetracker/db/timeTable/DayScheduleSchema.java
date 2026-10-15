package com.redcodersgroup.attend_collegeattendancetracker.db.timeTable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;

@Entity(tableName = "daySchedule",foreignKeys = {@ForeignKey(
        entity = TimeTableDaySchema.class,
        parentColumns = "did",
        childColumns = "did",
        onDelete = ForeignKey.CASCADE
),@ForeignKey(entity = SubjectSchema.class,parentColumns = "sid",childColumns = "p1",onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = SubjectSchema.class,parentColumns = "sid",childColumns = "p2",onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = SubjectSchema.class,parentColumns = "sid",childColumns = "p3",onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = SubjectSchema.class,parentColumns = "sid",childColumns = "p4",onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = SubjectSchema.class,parentColumns = "sid",childColumns = "p5",onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = SubjectSchema.class,parentColumns = "sid",childColumns = "p6",onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = SubjectSchema.class,parentColumns = "sid",childColumns = "p7",onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = SubjectSchema.class,parentColumns = "sid",childColumns = "p8",onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = SubjectSchema.class,parentColumns = "sid",childColumns = "p9",onDelete = ForeignKey.CASCADE)},
        indices = {@Index(value = "did",unique = true),
                @Index(value = "p1",unique = false),
                @Index(value = "p2",unique = false),
                @Index(value = "p3",unique = false),
                @Index(value = "p4",unique = false),
                @Index(value = "p5",unique = false),
                @Index(value = "p6",unique = false),
                @Index(value = "p7",unique = false),
                @Index(value = "p8",unique = false),
                @Index(value = "p9",unique = false)
})
public class DayScheduleSchema {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "scheduleId")
    private int scheduleId;
    @ColumnInfo(name = "did")
    private int did;
    @ColumnInfo(name = "p1")
    private Integer p1;
    @ColumnInfo(name = "p2")
    private Integer p2;
    @ColumnInfo(name = "p3")
    private Integer p3;
    @ColumnInfo(name = "p4")
    private Integer p4;
    @ColumnInfo(name = "p5")
    private Integer p5;
    @ColumnInfo(name = "p6")
    private Integer p6;
    @ColumnInfo(name = "p7")
    private Integer p7;
    @ColumnInfo(name = "p8")
    private Integer p8;
    @ColumnInfo(name = "p9")
    private Integer p9;



    public DayScheduleSchema(int scheduleId, int did, Integer p1, Integer p2, Integer p3, Integer p4, Integer p5, Integer p6, Integer p7, Integer p8, Integer p9) {
        this.scheduleId = scheduleId;
        this.did = did;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.p4 = p4;
        this.p5 = p5;
        this.p6 = p6;
        this.p7 = p7;
        this.p8 = p8;
        this.p9 = p9;
    }
    @Ignore

    public DayScheduleSchema(int did, Integer p1, Integer p2, Integer p3, Integer p4, Integer p5, Integer p6, Integer p7, Integer p8, Integer p9) {
        this.did = did;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.p4 = p4;
        this.p5 = p5;
        this.p6 = p6;
        this.p7 = p7;
        this.p8 = p8;
        this.p9 = p9;
    }


    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public int getDid() {
        return did;
    }

    public void setDid(int did) {
        this.did = did;
    }

    public Integer getP1() {
        return p1;
    }

    public void setP1(Integer p1) {
        this.p1 = p1;
    }

    public Integer getP3() {
        return p3;
    }

    public void setP3(Integer p3) {
        this.p3 = p3;
    }

    public Integer getP2() {
        return p2;
    }

    public void setP2(Integer p2) {
        this.p2 = p2;
    }

    public Integer getP4() {
        return p4;
    }

    public void setP4(Integer p4) {
        this.p4 = p4;
    }

    public Integer getP5() {
        return p5;
    }

    public void setP5(Integer p5) {
        this.p5 = p5;
    }

    public Integer getP6() {
        return p6;
    }

    public void setP6(Integer p6) {
        this.p6 = p6;
    }

    public Integer getP7() {
        return p7;
    }

    public void setP7(Integer p7) {
        this.p7 = p7;
    }

    public Integer getP8() {
        return p8;
    }

    public void setP8(Integer p8) {
        this.p8 = p8;
    }

    public Integer getP9() {
        return p9;
    }

    public void setP9(Integer p9) {
        this.p9 = p9;
    }
}
