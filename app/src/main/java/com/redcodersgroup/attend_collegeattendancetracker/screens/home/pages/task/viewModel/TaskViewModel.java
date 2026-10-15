package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.task.viewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.assignment.AssignmentSchema;

import java.util.List;

public class TaskViewModel extends AndroidViewModel {
    public TaskViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<AssignmentSchema>> getAssignments(){
        return Database.getInstance().getAssignmentDao().getAssignments();
    }
}
