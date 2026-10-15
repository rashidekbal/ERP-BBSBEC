package com.redcodersgroup.attend_collegeattendancetracker.db.holiday;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HolidayDao {
    @Query("select * from holiday")
    LiveData<List<HolidaySchema>> getHolidays();
    @Query("select * from holiday")
    List<HolidaySchema> getHolidaysSync();
    @Query("delete from holiday where hid=:id")
    void deleteHoliday(int id);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addHoliday(HolidaySchema holidaySchema);
    @Query("select * from holiday where hid=:id")
    HolidaySchema getHolidayById(int id);
    @androidx.room.Update
    void updateHoliday(HolidaySchema holidaySchema);

    @Query("DELETE FROM holiday")
    void deleteAll();
}
