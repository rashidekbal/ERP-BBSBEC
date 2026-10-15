package com.redcodersgroup.attend_collegeattendancetracker.db.subjects;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface SubjectAdditionalInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SubjectAdditionalInfoSchema subjectAdditionalInfoSchema);

    @Query("SELECT * FROM subjectAdditionalInfo WHERE sid = :sid LIMIT 1")
    SubjectAdditionalInfoSchema getSubjectAdditionalInfoBySid(int sid);

    @Update
    void update(SubjectAdditionalInfoSchema subjectAdditionalInfoSchema);

    @Query("DELETE FROM subjectAdditionalInfo WHERE sid = :sid")
    void deleteBySid(int sid);

    @Query("SELECT * FROM subjectAdditionalInfo")
    java.util.List<SubjectAdditionalInfoSchema> getAllSync();

    @Query("DELETE FROM subjectAdditionalInfo")
    void deleteAll();
}
