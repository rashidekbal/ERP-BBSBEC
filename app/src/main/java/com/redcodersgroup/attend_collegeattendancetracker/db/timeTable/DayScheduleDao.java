package com.redcodersgroup.attend_collegeattendancetracker.db.timeTable;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DayScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMulti(List<DayScheduleSchema> daySchedules);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DayScheduleSchema daySchedule);

    @Query("SELECT * FROM daySchedule WHERE did = :did LIMIT 1")
    DayScheduleSchema getScheduleByDid(int did);

    @Query("SELECT * FROM daySchedule WHERE did = :did LIMIT 1")
    androidx.lifecycle.LiveData<DayScheduleSchema> getScheduleByDidLiveData(int did);

    @Query("SELECT * FROM daySchedule")
    List<DayScheduleSchema> getAllSchedules();

    @Query("SELECT * FROM daySchedule")
    androidx.lifecycle.LiveData<List<DayScheduleSchema>> getAllSchedulesLiveData();

    @Update
    void update(DayScheduleSchema daySchedule);

    @Query("DELETE FROM daySchedule")
    void deleteAll();
}

