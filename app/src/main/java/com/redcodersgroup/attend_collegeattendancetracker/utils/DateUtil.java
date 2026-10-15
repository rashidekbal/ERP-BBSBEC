package com.redcodersgroup.attend_collegeattendancetracker.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtil {
    public static long getDateMs(String selectedDate) {
        if (selectedDate == null || selectedDate.trim().isEmpty()) {
            return 0;
        }
        String s = selectedDate.trim();
        try {
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            df.setLenient(false);
            Date date = df.parse(s);
            if (date != null) return date.getTime();
        } catch (Exception ignored) {}

        try {
            DateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            df.setLenient(false);
            Date date = df.parse(s);
            if (date != null) return date.getTime();
        } catch (Exception ignored) {}

        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            df.setLenient(false);
            Date date = df.parse(s);
            if (date != null) return date.getTime();
        } catch (Exception ignored) {}

        return 0;
    }
    public static String getDateString(long ms){
        Date date=new Date(ms);
        DateFormat dateFormat=new SimpleDateFormat("dd/MM/yyyy",Locale.US);
        return dateFormat.format(date);
    }
    public static boolean isSingleDay(long ms1,long ms2){
        long ans=ms2-ms1;
        ans=ans/(1000*60*60*24);
        return ans <= 1;
    }

    public static String getMonthName(int month){
        String[] months={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        return months[month];

    }
    public static String getDayName(int day){
        String[] days={"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        return days[day];
    }
}
