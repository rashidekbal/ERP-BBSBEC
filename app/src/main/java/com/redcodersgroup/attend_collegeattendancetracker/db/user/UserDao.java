package com.redcodersgroup.attend_collegeattendancetracker.db.user;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addUser(UserSchema user);

    @Query("SELECT * FROM user LIMIT 1")
    UserSchema getUser();

    @Query("SELECT * FROM user LIMIT 1")
    LiveData<UserSchema> getUserLiveData();

    @Update
    void updateUser(UserSchema user);

    @Query("UPDATE user SET working_days=:workingDays, first_period_start_time=:firstPeriodStartTime, attendanceCriteria=:attendanceCriteria, maxMarks=:maxMarks WHERE uid=:uid")
    void updateUserAcademicInfo(int uid, int workingDays, long firstPeriodStartTime, int attendanceCriteria, int maxMarks);

    @Query("DELETE FROM user")
    void deleteAll();
}
