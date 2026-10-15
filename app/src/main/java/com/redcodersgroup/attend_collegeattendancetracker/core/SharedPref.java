package com.redcodersgroup.attend_collegeattendancetracker.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.redcodersgroup.attend_collegeattendancetracker.App;
import com.redcodersgroup.attend_collegeattendancetracker.constants.SharedPrefKeys;

public class SharedPref {
    private final SharedPreferences sharedPrefs;
    private static SharedPref instance;

    private SharedPref(){
        sharedPrefs=App.getContext().getSharedPreferences(SharedPrefKeys.ATTEND_SHARED_PREF.toString(), Context.MODE_PRIVATE);
    }

    public static SharedPreferences getSharedPrefs(){
        if(instance==null){
            instance=new SharedPref();
        }
        return instance.sharedPrefs;
    }


}
