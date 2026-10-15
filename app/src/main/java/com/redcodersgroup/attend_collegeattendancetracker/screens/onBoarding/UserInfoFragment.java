package com.redcodersgroup.attend_collegeattendancetracker.screens.onBoarding;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.FragmentUserInfoBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.utils.DateUtil;

import java.util.Calendar;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class UserInfoFragment extends Fragment {
    FragmentUserInfoBinding binding;
    ExecutorService executorService;


    public UserInfoFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUserInfoBinding.inflate(inflater, container, false);
        init();
        setEventListeners();
        return binding.getRoot();
    }

    private void init() {
        executorService= Executors.newSingleThreadExecutor();
    }

    private void setEventListeners() {
        binding.backBtn.setOnClickListener(v->requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.startDate.setOnClickListener(this::handleStartDateClick);
        binding.endDate.setOnClickListener(this::handleEndDateClick);
        binding.continueBtn.setOnClickListener(this::handleContinueCLick);

    }

    private void handleEndDateClick(View view) {
        Calendar calendar=Calendar.getInstance();
        int year=calendar.get(Calendar.YEAR);
        int month=calendar.get(Calendar.MONTH);
        int day=calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog=new DatePickerDialog(requireActivity(),(view1, selectedYear, selectedMonth, selectedDay)-> handleEndDateSelected(selectedYear,selectedMonth,selectedDay),year,month,day);
        datePickerDialog.show();
    }

    private void handleStartDateClick(View view) {
        Calendar calendar=Calendar.getInstance();
        int year=calendar.get(Calendar.YEAR);
        int month=calendar.get(Calendar.MONTH);
        int day=calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog  datePickerDialog=new DatePickerDialog(requireActivity(),(view1,selectedYear,selectedMonth,selectedDay)-> handleStartDateSelected(selectedYear,selectedMonth,selectedDay),year,month,day);
        datePickerDialog.show();

    }
    private void handleStartDateSelected(int selectedYear, int selectedMonth, int selectedDay) {
       String startDate=selectedDay+"/"+(selectedMonth+1)+"/"+selectedYear;
       //TODO:handle date validation
        binding.startDateTextView.setText(startDate);
    }
    private void handleEndDateSelected(int selectedYear, int selectedMonth, int selectedDay) {
        String endDate=selectedDay+"/"+(selectedMonth+1)+"/"+selectedYear;
        //TODO:handle date validation
        binding.endDateTextView.setText(endDate);
    }

    private void handleContinueCLick(View view) {
        binding.continueBtn.setEnabled(false);
        String name= Objects.requireNonNull(binding.nameInput.getText()).toString();
        String college= Objects.requireNonNull(binding.collegeInput.getText()).toString();
        String startDate=binding.startDateTextView.getText().toString();
        String endDate=binding.endDateTextView.getText().toString();
        int checkedSemChipId=binding.semesterChipGroup.getCheckedChipId();
        int sem=Integer.parseInt( binding.semesterChipGroup.findViewById(checkedSemChipId).getTag().toString());
        if(!validInputs(name,college,startDate,endDate,sem)){
            binding.continueBtn.setEnabled(true);
            return;
        }
       executorService.execute(()->{
           UserSchema userSchema=new UserSchema(name,college,null,null,sem, DateUtil.getDateMs(startDate),DateUtil.getDateMs(endDate),0,0,0,"#F5A623");
           Database.getInstance().getUserDao().addUser(userSchema);

           requireActivity().runOnUiThread(()-> {
               binding.continueBtn.setEnabled(true);
               Navigation.findNavController(view).navigate(R.id.action_userInfoFragment_to_academicInfoFragment);
           });
       });





    }

    private boolean validInputs(String name, String college, String startDate, String endDate, int sem) {
        if(name.isEmpty()){
            binding.nameInput.setBackground(getResources().getDrawable(R.drawable.danger_surface2_card_bg,null));
            binding.nameInput.setError("name must not be empty");
            return false;
        }
        if(startDate.isEmpty()){
            binding.startDate.setBackground(getResources().getDrawable(R.drawable.danger_surface2_card_bg,null));
            binding.startDateTextView.setError("start date must not be empty");
            return false;
        }
        if(endDate.isEmpty()){
            binding.endDate.setBackground(getResources().getDrawable(R.drawable.danger_surface2_card_bg,null));
            binding.endDateTextView.setError("end date must not be empty");
            return false;
        }
        if(sem==0){
            binding.semesterChipGroup.setBackground(getResources().getDrawable(R.drawable.danger_surface2_card_bg,null));
            return false;
        }
        return true;
    }
}