package com.redcodersgroup.attend_collegeattendancetracker.utils;

import android.widget.Toast;

import com.redcodersgroup.attend_collegeattendancetracker.App;

public class ToastUtil {
    public static void showToastShort(String msg){
        Toast.makeText(App.getContext(),msg,Toast.LENGTH_SHORT).show();
    }
    public static void showToastLong(String msg){
        Toast.makeText(App.getContext(),msg,Toast.LENGTH_LONG).show();
    }

}
