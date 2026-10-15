package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redcodersgroup.attend_collegeattendancetracker.databinding.DialogDayAttendanceBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.attendance.AttendanceSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.adapter.DaySubjectAttendanceAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.model.DaySubjectAttendanceModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.viewmodel.TimeTableViewModel;
import com.redcodersgroup.attend_collegeattendancetracker.utils.AttendanceScheduleUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.widget.AttendanceWidgetProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DayAttendanceDialogFragment extends DialogFragment {

    public static final String TAG = "DayAttendanceDialogFragment";

    private static final String ARG_DATE_STR = "arg_date_str";
    private static final String ARG_DATE_MS = "arg_date_ms";
    private static final String ARG_IS_HOLIDAY = "arg_is_holiday";
    private static final String ARG_HOLIDAY_NAME = "arg_holiday_name";
    private static final String ARG_IS_WEEKEND = "arg_is_weekend";

    public interface OnAttendanceDismissListener {
        void onDismissed();
    }

    private OnAttendanceDismissListener dismissListener;

    public static DayAttendanceDialogFragment newInstance(String dateStr, long dateMs, boolean isHoliday, String holidayName, boolean isWeekend) {
        DayAttendanceDialogFragment fragment = new DayAttendanceDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE_STR, dateStr);
        args.putLong(ARG_DATE_MS, dateMs);
        args.putBoolean(ARG_IS_HOLIDAY, isHoliday);
        args.putString(ARG_HOLIDAY_NAME, holidayName);
        args.putBoolean(ARG_IS_WEEKEND, isWeekend);
        fragment.setArguments(args);
        return fragment;
    }

    public static void showDialog(FragmentManager fm, String dateStr, long dateMs, boolean isHoliday, String holidayName, boolean isWeekend, OnAttendanceDismissListener listener) {
        DayAttendanceDialogFragment fragment = newInstance(dateStr, dateMs, isHoliday, holidayName, isWeekend);
        fragment.setDismissListener(listener);
        fragment.show(fm, TAG);
    }

    public void setDismissListener(OnAttendanceDismissListener listener) {
        this.dismissListener = listener;
    }

    private DialogDayAttendanceBinding binding;
    private String dateStr;
    private long dateMs;
    private boolean isHoliday;
    private String holidayName;
    private boolean isWeekend;

    private final List<DaySubjectAttendanceModel> subjectList = new ArrayList<>();
    private DaySubjectAttendanceAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogDayAttendanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        parseArguments();
        initViews();
        loadData();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void parseArguments() {
        if (getArguments() != null) {
            dateStr = getArguments().getString(ARG_DATE_STR, "");
            dateMs = getArguments().getLong(ARG_DATE_MS, 0L);
            isHoliday = getArguments().getBoolean(ARG_IS_HOLIDAY, false);
            holidayName = getArguments().getString(ARG_HOLIDAY_NAME, "");
            isWeekend = getArguments().getBoolean(ARG_IS_WEEKEND, false);
        }
    }

    private void initViews() {
        Calendar cal = Calendar.getInstance();
        if (dateMs > 0) {
            cal.setTimeInMillis(dateMs);
        }
        String formattedDate = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(cal.getTime());
        binding.dayTitle.setText(formattedDate);

        if (isHoliday) {
            String hol = holidayName != null && !holidayName.isEmpty() ? holidayName : "College Holiday";
            binding.daySubtitle.setText("🎉 Holiday: " + hol);
        } else if (isWeekend) {
            binding.daySubtitle.setText("🏖️ Weekend / Off-day");
        } else {
            binding.daySubtitle.setText("College Day");
        }

        // Apply accent color
        int accent = ThemeManager.getAccentColorInt();
        binding.dateIconCard.setCardBackgroundColor(Color.argb(35, Color.red(accent), Color.green(accent), Color.blue(accent)));
        binding.dateIcon.setImageTintList(ColorStateList.valueOf(accent));
        binding.btnDone.setBackgroundTintList(ColorStateList.valueOf(accent));
        binding.btnDone.setText("Close");

        adapter = new DaySubjectAttendanceAdapter(requireContext(), subjectList, null);

        binding.daySubjectsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.daySubjectsRecyclerView.setAdapter(adapter);

        binding.closeBtn.setOnClickListener(v -> dismiss());
        binding.btnDone.setOnClickListener(v -> dismiss());
    }

    private void loadData() {
        Calendar todayEndCal = Calendar.getInstance();
        todayEndCal.set(Calendar.HOUR_OF_DAY, 23);
        todayEndCal.set(Calendar.MINUTE, 59);
        todayEndCal.set(Calendar.SECOND, 59);
        todayEndCal.set(Calendar.MILLISECOND, 999);
        boolean isFuture = dateMs > todayEndCal.getTimeInMillis();

        if (isHoliday) {
            String hol = (holidayName != null && !holidayName.trim().isEmpty()) ? holidayName : "College Holiday";
            binding.daySubtitle.setText("🎉 Holiday: " + hol);
            binding.daySubjectsRecyclerView.setVisibility(View.GONE);
            binding.emptyLayout.setVisibility(View.VISIBLE);
            binding.emptyTitle.setText("College Holiday");
            binding.emptySubtitle.setText(hol + " · College is closed. No classes conducted or attendance required.");
            binding.btnDone.setText("Close");
            return;
        }

        DbHelper.execute(() -> {
            Database db = Database.getInstance();
            List<SubjectSchema> subjects = db.getSubjectDao().getSubjectsSync();
            List<AttendanceSchema> attendances = db.getAttendanceDao().getAttendanceByDate(dateStr);
            if ((attendances == null || attendances.isEmpty()) && dateStr != null) {
                long ms = com.redcodersgroup.attend_collegeattendancetracker.utils.DateUtil.getDateMs(dateStr);
                if (ms > 0) {
                    String altFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date(ms));
                    List<AttendanceSchema> altList = db.getAttendanceDao().getAttendanceByDate(altFormat);
                    if (altList != null && !altList.isEmpty()) {
                        attendances = altList;
                    }
                }
            }

            List<TimeTableDaySchema> days = db.getTimeTableDayDao().getDays();
            List<DayScheduleSchema> schedules = db.getDayScheduleDao().getAllSchedules();

            Map<Integer, SubjectSchema> subjectMap = new HashMap<>();
            if (subjects != null) {
                for (SubjectSchema s : subjects) {
                    subjectMap.put(s.getSid(), s);
                }
            }

            Map<String, String> statusMap = new HashMap<>();
            if (attendances != null) {
                for (AttendanceSchema att : attendances) {
                    if (att != null) {
                        statusMap.put(att.getSid() + "_" + att.isLab(), att.getStatus());
                    }
                }
            }

            // Find scheduled subjects for this day of week from timetable
            Calendar cal = Calendar.getInstance();
            if (dateMs > 0) cal.setTimeInMillis(dateMs);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            String dayName = AttendanceScheduleUtil.getDayNameFromDayOfWeek(dayOfWeek);

            DayScheduleSchema daySchedule = null;
            if (days != null && schedules != null) {
                for (TimeTableDaySchema d : days) {
                    if (d != null && d.getDayName() != null && d.getDayName().equalsIgnoreCase(dayName)) {
                        for (DayScheduleSchema s : schedules) {
                            if (s != null && s.getDid() == d.getDid()) {
                                daySchedule = s;
                                break;
                            }
                        }
                        break;
                    }
                }
            }

            List<DaySubjectAttendanceModel> models = new ArrayList<>();
            Set<String> addedKeys = new HashSet<>();

            // 1. Add all attendance records logged on this date (from ERP sync)
            if (attendances != null) {
                for (AttendanceSchema att : attendances) {
                    if (att == null) continue;
                    String key = att.getSid() + "_" + att.isLab();
                    if (!addedKeys.contains(key)) {
                        addedKeys.add(key);
                        SubjectSchema sub = subjectMap.get(att.getSid());
                        String subName = (sub != null && sub.getSubjectName() != null && !sub.getSubjectName().isEmpty())
                                ? sub.getSubjectName()
                                : ("Subject #" + att.getSid());
                        String colorCode = (sub != null) ? sub.getUniqueColorCode() : null;

                        models.add(new DaySubjectAttendanceModel(
                                att.getSid(),
                                subName,
                                att.isLab(),
                                colorCode,
                                att.getStatus(),
                                true,
                                sub
                        ));
                    }
                }
            }

            // 2. If timetable has additional scheduled classes for this weekday, include them too
            if (daySchedule != null) {
                for (int p = 1; p <= TimeTableViewModel.TOTAL_PERIODS; p++) {
                    Integer sid = TimeTableViewModel.getPeriodValue(daySchedule, p);
                    if (sid != null && sid > 0) {
                        SubjectSchema sub = subjectMap.get(sid);
                        if (sub != null) {
                            boolean isLab = sub.isLab();
                            String key = sid + "_" + isLab;
                            if (!addedKeys.contains(key)) {
                                addedKeys.add(key);
                                String status = statusMap.get(key);

                                models.add(new DaySubjectAttendanceModel(
                                        sub.getSid(),
                                        sub.getSubjectName(),
                                        isLab,
                                        sub.getUniqueColorCode(),
                                        status,
                                        true,
                                        sub
                                ));
                            }
                        }
                    }
                }
            }

            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    subjectList.clear();
                    subjectList.addAll(models);
                    adapter.notifyDataSetChanged();

                    if (subjectList.isEmpty()) {
                        binding.emptyLayout.setVisibility(View.VISIBLE);
                        binding.daySubjectsRecyclerView.setVisibility(View.GONE);

                        if (isHoliday) {
                            binding.emptyTitle.setText("College Holiday");
                            binding.emptySubtitle.setText(holidayName != null && !holidayName.isEmpty() ? ("Campus closed for " + holidayName) : "Campus Closed");
                        } else if (isWeekend) {
                            binding.emptyTitle.setText("Weekend");
                            binding.emptySubtitle.setText("No classes or attendance records on weekend.");
                        } else {
                            binding.emptyTitle.setText("No Attendance Recorded");
                            binding.emptySubtitle.setText("No attendance records found for this date.");
                        }
                    } else {
                        binding.emptyLayout.setVisibility(View.GONE);
                        binding.daySubjectsRecyclerView.setVisibility(View.VISIBLE);

                        int presentCount = 0;
                        int absentCount = 0;
                        for (DaySubjectAttendanceModel m : subjectList) {
                            if (com.redcodersgroup.attend_collegeattendancetracker.constants.AttendanceConstants.isPresent(m.getStatus())) {
                                presentCount++;
                            } else if (com.redcodersgroup.attend_collegeattendancetracker.constants.AttendanceConstants.isAbsent(m.getStatus())) {
                                absentCount++;
                            }
                        }

                        if (presentCount > 0 || absentCount > 0) {
                            binding.daySubtitle.setText(subjectList.size() + (subjectList.size() == 1 ? " Class · " : " Classes · ") + presentCount + " Present, " + absentCount + " Absent");
                        } else {
                            binding.daySubtitle.setText(subjectList.size() + (subjectList.size() == 1 ? " Scheduled Class" : " Scheduled Classes"));
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissListener != null) {
            dismissListener.onDismissed();
        }
    }
}
