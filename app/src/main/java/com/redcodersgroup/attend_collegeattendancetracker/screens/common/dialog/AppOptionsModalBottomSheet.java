package com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.DialogAppOptionsSheetBinding;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemAppOptionBinding;

import java.util.ArrayList;
import java.util.List;

public class AppOptionsModalBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "AppOptionsModalBottomSheet";

    public interface OnOptionSelectedListener {
        void onOptionSelected(String optionId);
    }

    private String title;
    private String subtitle;
    private final List<AppOptionItem> options = new ArrayList<>();
    private OnOptionSelectedListener listener;
    private DialogAppOptionsSheetBinding binding;

    public static AppOptionsModalBottomSheet newInstance(String title, String subtitle, List<AppOptionItem> options, OnOptionSelectedListener listener) {
        AppOptionsModalBottomSheet sheet = new AppOptionsModalBottomSheet();
        sheet.title = title;
        sheet.subtitle = subtitle;
        if (options != null) sheet.options.addAll(options);
        sheet.listener = listener;
        return sheet;
    }

    public static void show(FragmentManager fm, String title, String subtitle, List<AppOptionItem> options, OnOptionSelectedListener listener) {
        AppOptionsModalBottomSheet sheet = newInstance(title, subtitle, options, listener);
        sheet.show(fm, TAG);
    }

    @Override
    public int getTheme() {
        return R.style.TransparentBottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogAppOptionsSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (title != null && !title.isEmpty()) {
            binding.sheetTitle.setText(title);
        }

        if (subtitle != null && !subtitle.isEmpty()) {
            binding.sheetSubtitle.setText(subtitle);
            binding.sheetSubtitle.setVisibility(View.VISIBLE);
        } else {
            binding.sheetSubtitle.setVisibility(View.GONE);
        }

        binding.closeSheetBtn.setOnClickListener(v -> dismiss());

        renderOptions();
    }

    private void renderOptions() {
        binding.optionsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        int dangerColor = ContextCompat.getColor(requireContext(), R.color.danger);
        int textColor = ContextCompat.getColor(requireContext(), R.color.text);
        int surface2Color = ContextCompat.getColor(requireContext(), R.color.surface2);
        int dangerDimColor = Color.argb(35, 245, 101, 74);

        for (AppOptionItem item : options) {
            ItemAppOptionBinding itemBinding = ItemAppOptionBinding.inflate(inflater, binding.optionsContainer, false);

            itemBinding.optionTitle.setText(item.getTitle());

            if (item.getSubtitle() != null && !item.getSubtitle().isEmpty()) {
                itemBinding.optionSubtitle.setText(item.getSubtitle());
                itemBinding.optionSubtitle.setVisibility(View.VISIBLE);
            } else {
                itemBinding.optionSubtitle.setVisibility(View.GONE);
            }

            if (item.getIconRes() != 0) {
                itemBinding.optionIcon.setImageResource(item.getIconRes());
            }

            if (item.isDestructive()) {
                itemBinding.optionTitle.setTextColor(dangerColor);
                itemBinding.optionIcon.setImageTintList(ColorStateList.valueOf(dangerColor));
                itemBinding.iconContainer.setCardBackgroundColor(dangerDimColor);
                itemBinding.chevronIcon.setImageTintList(ColorStateList.valueOf(dangerColor));
            } else {
                itemBinding.optionTitle.setTextColor(textColor);
                itemBinding.optionIcon.setImageTintList(ColorStateList.valueOf(textColor));
                itemBinding.iconContainer.setCardBackgroundColor(surface2Color);
                itemBinding.chevronIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_dim)));
            }

            itemBinding.getRoot().setOnClickListener(v -> {
                dismiss();
                if (listener != null) {
                    listener.onOptionSelected(item.getId());
                }
            });

            binding.optionsContainer.addView(itemBinding.getRoot());
        }
    }
}
