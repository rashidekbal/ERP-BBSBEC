package com.redcodersgroup.attend_collegeattendancetracker.db.timeTable;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TimeTableDayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMulti(List<TimeTableDaySchema> days);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TimeTableDaySchema day);

    @Query("select * from timeTable_day")
    List<TimeTableDaySchema> getDays();

    @Query("select * from timeTable_day")
    androidx.lifecycle.LiveData<List<TimeTableDaySchema>> getDaysLiveData();

    @Query("SELECT * FROM timeTable_day WHERE dayName = :dayName LIMIT 1")
    TimeTableDaySchema getDayByName(String dayName);

    @Query("DELETE FROM timeTable_day WHERE did = :did")
    void deleteDay(int did);

    @Query("DELETE FROM timeTable_day")
    void deleteAll();
}

