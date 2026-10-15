package com.redcodersgroup.attend_collegeattendancetracker.db.assignment;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AssignmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addAssignment(AssignmentSchema assignmentSchema);
    @Query("SELECT * FROM assignment")
    LiveData<List<AssignmentSchema>> getAssignments();
    @Query("delete from assignment where aid=:aid")
    void deleteAssignment(int aid);
    @Query("SELECT * FROM assignment")
    List<AssignmentSchema> getAssignmentsSync();

    @Query("UPDATE assignment SET subjectName = :subjectName WHERE sid = :sid")
    void updateSubjectName(int sid, String subjectName);

    @Query("SELECT * FROM assignment WHERE aid = :aid")
    AssignmentSchema getAssignmentById(int aid);

    @androidx.room.Update
    void updateAssignment(AssignmentSchema assignmentSchema);

    @Query("UPDATE assignment SET submitted = :submitted WHERE aid = :aid")
    void setSubmitted(int aid, boolean submitted);

    @Query("DELETE FROM assignment")
    void deleteAll();

    @Query("DELETE FROM assignment WHERE sid = :sid")
    void deleteBySid(int sid);
}

