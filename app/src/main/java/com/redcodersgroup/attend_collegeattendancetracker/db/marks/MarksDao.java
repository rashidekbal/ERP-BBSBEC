package com.redcodersgroup.attend_collegeattendancetracker.db.marks;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MarksDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addMarks(MarksSchema marksSchema);

    @Query("select * from marks where sid=:sid")
    MarksSchema getMarks(int sid);

    @Query("SELECT m.* FROM marks m INNER JOIN subject s ON m.sid = s.sid WHERE s.isLab = 0")
    LiveData<List<MarksSchema>> getMarks();

    @Query("DELETE FROM marks WHERE sid IN (SELECT sid FROM subject WHERE isLab = 1)")
    void deleteLabMarks();

    @Query("UPDATE marks SET subjectName = :subjectName WHERE sid = :sid")
    void updateSubjectName(int sid, String subjectName);

    @Query("DELETE FROM marks WHERE sid = :sid")
    void deleteMarks(int sid);

    @Query("SELECT * FROM marks")
    List<MarksSchema> getAllMarksSync();

    @Query("UPDATE marks SET mid1 = -1.0 WHERE sid = :sid")
    void clearMid1(int sid);

    @Query("UPDATE marks SET mid2 = -1.0 WHERE sid = :sid")
    void clearMid2(int sid);

    @Query("UPDATE marks SET mid3 = -1.0 WHERE sid = :sid")
    void clearMid3(int sid);

    @Query("DELETE FROM marks")
    void deleteAll();
}
