package com.redcodersgroup.attend_collegeattendancetracker.screens.holiday;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivityHolidayBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.holiday.HolidaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.holiday.adapter.HolidayAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.holiday.viewModel.HolidayViewModel;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class HolidayActivity extends AppCompatActivity {
    ActivityHolidayBinding binding;
    HolidayViewModel holidayViewModel;
    HolidayAdapter holidayAdapter;
    List<HolidaySchema> holidaySchemaList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        binding = ActivityHolidayBinding.inflate(getLayoutInflater());
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        init();
        setupRecyclerView();
        observeData();
        observeAccentColor();
        setEventListeners();
    }

    private void observeAccentColor() {
        ThemeManager.getAccentColorLiveData().observe(this, this::applyAccentColor);
    }

    private void applyAccentColor(String colorHex) {
        if (colorHex == null || binding == null) return;
        try {
            int color = Color.parseColor(colorHex);
            binding.btnEmptyAddHoliday.setBackgroundTintList(ColorStateList.valueOf(color));
            binding.emptyHolidayIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        } catch (Exception ignored) {}
    }

    private void observeData() {
        holidayViewModel.getHolidayList().observe(this, holidayList -> {
            if (holidayList != null) {
                holidaySchemaList.clear();
                holidaySchemaList.addAll(holidayList);
                holidayAdapter.notifyDataSetChanged();

                boolean isEmpty = holidayList.isEmpty();
                binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.emptyHolidayLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setupRecyclerView() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setAdapter(holidayAdapter);
    }

    private void setEventListeners() {
        binding.backBtn.setOnClickListener(v -> finish());
        binding.addHolidayBtn.setOnClickListener(v -> com.redcodersgroup.attend_collegeattendancetracker.screens.holiday.AddHolidayDialogFragment.showDialog(getSupportFragmentManager()));
        binding.btnEmptyAddHoliday.setOnClickListener(v -> com.redcodersgroup.attend_collegeattendancetracker.screens.holiday.AddHolidayDialogFragment.showDialog(getSupportFragmentManager()));
    }

    private void init() {
        holidaySchemaList = new ArrayList<>();
        holidayAdapter = new HolidayAdapter(this, holidaySchemaList);
        holidayViewModel = new ViewModelProvider(this).get(HolidayViewModel.class);
    }
}