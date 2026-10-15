package com.redcodersgroup.attend_collegeattendancetracker.screens.holiday.viewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.holiday.HolidaySchema;

import java.util.List;

public class HolidayViewModel extends AndroidViewModel {
    public HolidayViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<HolidaySchema>> getHolidayList(){
        return Database.getInstance().getHolidayDao().getHolidays();
    }
}
