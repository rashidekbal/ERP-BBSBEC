package com.redcodersgroup.attend_collegeattendancetracker.db.subjects;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SubjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addSubject(SubjectSchema subjectSchema);
    @Query("select * from subject")
    LiveData<List<SubjectSchema>> getSubjects();
    @Query("select * from subject")
    List<SubjectSchema> getSubjectsSync();
    @Query("select * from subject where subjectName=:sn limit 1")
    SubjectSchema getSubjectByName(String sn);
    @Query("delete from subject where sid=:id")
    void deleteSubject(int id);

    @Query("select * from subject where timeTableSetupComplete=false")
    LiveData<List<SubjectSchema>> getTimeTableSetupPendingSubjects();
    @Query("select * from subject where attendanceSetupComplete=false")
    LiveData<List<SubjectSchema> > getAttendanceSetupPendingSubjects();

    @Query("SELECT * FROM subject WHERE sid = :sid LIMIT 1")
    SubjectSchema getSubjectById(int sid);

    @Query("SELECT * FROM subject WHERE attendanceSetupComplete = 1")
    LiveData<List<SubjectSchema>> getSetupCompletedSubjects();

    @Query("SELECT * FROM subject WHERE attendanceSetupComplete = 1")
    List<SubjectSchema> getSetupCompletedSubjectsSync();

    @androidx.room.Update
    void updateSubject(SubjectSchema subjectSchema);

    @Query("UPDATE subject SET subjectName = :subjectName WHERE sid = :sid")
    void updateSubjectName(int sid, String subjectName);

    @Query("SELECT * FROM subject WHERE erpSubjectId = :erpId LIMIT 1")
    SubjectSchema getSubjectByErpId(int erpId);

    @androidx.room.Query("DELETE FROM subject")
    void deleteAll();
}
