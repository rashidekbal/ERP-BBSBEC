package com.redcodersgroup.attend_collegeattendancetracker.screens.profile.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;

import java.util.List;

public class ProfileViewModel extends AndroidViewModel {
    private final LiveData<UserSchema> userLiveData;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        userLiveData = Database.getInstance().getUserDao().getUserLiveData();
    }

    public LiveData<UserSchema> getUserLiveData() {
        return userLiveData;
    }

    public void saveUserProfile(UserSchema user, int previousWorkingDays, Runnable onSuccess, Consumer<String> onError) {
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            onError.accept("Name cannot be empty");
            return;
        }

        if (user.getCollegeName() == null || user.getCollegeName().trim().isEmpty()) {
            onError.accept("College name cannot be empty");
            return;
        }

        if (user.getSemester() < 1 || user.getSemester() > 8) {
            onError.accept("Please select a valid semester");
            return;
        }

        if (user.getSemStartDate() > 0 && user.getSemEndDate() > 0 && user.getSemStartDate() >= user.getSemEndDate()) {
            onError.accept("Semester start date must be before end date");
            return;
        }

        DbHelper.execute(() -> {
            try {
                UserSchema existing = Database.getInstance().getUserDao().getUser();
                if (existing == null) {
                    Database.getInstance().getUserDao().addUser(user);
                } else {
                    Database.getInstance().getUserDao().updateUser(user);
                }

                // Sync working days if changed
                int newWorkingDays = user.getWorkingDays();
                if (previousWorkingDays > 0 && previousWorkingDays != newWorkingDays) {
                    List<TimeTableDaySchema> days = Database.getInstance().getTimeTableDayDao().getDays();
                    if (newWorkingDays == 6) {
                        boolean hasSaturday = false;
                        for (TimeTableDaySchema day : days) {
                            if ("Saturday".equalsIgnoreCase(day.getDayName())) {
                                hasSaturday = true;
                                break;
                            }
                        }
                        if (!hasSaturday) {
                            TimeTableDaySchema saturday = new TimeTableDaySchema("Saturday");
                            Database.getInstance().getTimeTableDayDao().insert(saturday);
                            List<TimeTableDaySchema> updatedDays = Database.getInstance().getTimeTableDayDao().getDays();
                            for (TimeTableDaySchema day : updatedDays) {
                                if ("Saturday".equalsIgnoreCase(day.getDayName())) {
                                    Database.getInstance().getDayScheduleDao().insert(
                                            new DayScheduleSchema(day.getDid(), null, null, null, null, null, null, null, null, null)
                                    );
                                    break;
                                }
                            }
                        }
                    } else if (newWorkingDays == 5) {
                        for (TimeTableDaySchema day : days) {
                            if ("Saturday".equalsIgnoreCase(day.getDayName())) {
                                Database.getInstance().getTimeTableDayDao().deleteDay(day.getDid());
                                break;
                            }
                        }
                    }
                }

                mainHandler.post(onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> onError.accept("Failed to update profile: " + e.getMessage()));
            }
        });
    }
}
