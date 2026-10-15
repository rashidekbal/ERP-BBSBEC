package com.redcodersgroup.attend_collegeattendancetracker.db.attendance;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addAttendance(AttendanceSchema attendance);

    @Query("SELECT * FROM attendance WHERE sid = :sid")
    LiveData<List<AttendanceSchema>> getAttendanceBySid(int sid);

    @Query("SELECT * FROM attendance WHERE sid = :sid")
    List<AttendanceSchema> getAttendanceBySidSync(int sid);

    @Query("SELECT * FROM attendance WHERE sid = :sid AND isLab = :isLab ")
    LiveData<List<AttendanceSchema>> getAttendanceBySidAndLab(int sid, boolean isLab);

    @Query("SELECT * FROM attendance WHERE sid = :sid AND isLab = :isLab order by aid desc")
    List<AttendanceSchema> getAttendanceBySidAndLabSync(int sid, boolean isLab);

    @Query("SELECT * FROM attendance WHERE sid = :sid AND date = :date AND isLab = :isLab LIMIT 1")
    AttendanceSchema getAttendanceRecord(int sid, String date, boolean isLab);

    @Query("SELECT * FROM attendance WHERE erpAttendanceId = :erpAttendanceId LIMIT 1")
    AttendanceSchema getAttendanceByErpId(int erpAttendanceId);

    @Query("SELECT * FROM attendance WHERE sid = :sid AND date = :date AND hour = :hour LIMIT 1")
    AttendanceSchema getAttendanceRecordByHour(int sid, String date, String hour);

    @Query("SELECT * FROM attendance WHERE date = :date order by aid desc")
    List<AttendanceSchema> getAttendanceByDate(String date);

    @Query("SELECT * FROM attendance order by aid desc")
    LiveData<List<AttendanceSchema>> getAllAttendance();

    @Query("SELECT * FROM attendance order by aid desc")
    List<AttendanceSchema> getAllAttendanceSync();

    @Query("DELETE FROM attendance WHERE aid = :aid")
    void deleteAttendance(int aid);

    @Query("DELETE FROM attendance WHERE sid = :sid AND date = :date AND isLab = :isLab")
    void deleteAttendanceRecord(int sid, String date, boolean isLab);

    @Query("UPDATE attendance SET status = :status WHERE aid = :aid")
    void updateStatus(int aid, String status);

    @Query("SELECT COUNT(*) FROM attendance WHERE sid = :sid AND status = :status")
    int getCountByStatus(int sid, String status);

    @Query("SELECT COUNT(*) FROM attendance WHERE sid = :sid AND isLab = :isLab AND status = :status")
    int getCountByStatusAndLab(int sid, boolean isLab, String status);

    @Query("SELECT COUNT(*) FROM attendance WHERE sid = :sid AND isLab = :isLab AND (UPPER(status) = 'PRESENT' OR UPPER(status) = 'ATTENDED')")
    int getAttendedCount(int sid, boolean isLab);

    @Query("SELECT COUNT(*) FROM attendance WHERE sid = :sid AND (UPPER(status) = 'PRESENT' OR UPPER(status) = 'ATTENDED')")
    int getAttendedCountBySid(int sid);

    @Query("SELECT COUNT(*) FROM attendance WHERE sid = :sid AND isLab = :isLab AND (UPPER(status) = 'ABSENT' OR UPPER(status) = 'MISSED')")
    int getAbsentCount(int sid, boolean isLab);

    @Query("SELECT COUNT(*) FROM attendance WHERE sid = :sid AND isLab = :isLab AND UPPER(status) = 'CANCELLED'")
    int getCancelledCount(int sid, boolean isLab);

    @Query("SELECT COUNT(*) FROM attendance")
    int getTotalAttendanceMarkedCount();

    @Query("DELETE FROM attendance")
    void deleteAll();

    @Query("DELETE FROM attendance WHERE sid = :sid")
    void deleteBySid(int sid);
}

