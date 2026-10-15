package com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog;

import android.app.Dialog;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.DialogAppConfirmationBinding;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;

public class AppConfirmationDialogFragment extends DialogFragment {

    public static final String TAG = "AppConfirmationDialogFragment";

    public interface OnConfirmListener {
        void onConfirmed();
    }

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_MESSAGE = "arg_message";
    private static final String ARG_CONFIRM = "arg_confirm";
    private static final String ARG_CANCEL = "arg_cancel";
    private static final String ARG_ICON = "arg_icon";
    private static final String ARG_DESTRUCTIVE = "arg_destructive";
    private static final String ARG_CANCELABLE = "arg_cancelable";

    private String title;
    private String message;
    private String confirmText;
    private String cancelText;
    private int iconRes;
    private boolean isDestructive = true;
    private boolean isDialogCancelable = true;
    private OnConfirmListener onConfirmListener;

    public static AppConfirmationDialogFragment newInstance(
            String title, String message, String confirmText, String cancelText,
            int iconRes, boolean isDestructive, boolean cancelable, OnConfirmListener onConfirmListener) {
        AppConfirmationDialogFragment dialog = new AppConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_CONFIRM, confirmText != null ? confirmText : "Confirm");
        args.putString(ARG_CANCEL, cancelText);
        args.putInt(ARG_ICON, iconRes != 0 ? iconRes : R.drawable.ic_delete);
        args.putBoolean(ARG_DESTRUCTIVE, isDestructive);
        args.putBoolean(ARG_CANCELABLE, cancelable);
        dialog.setArguments(args);
        dialog.title = title;
        dialog.message = message;
        dialog.confirmText = confirmText != null ? confirmText : "Confirm";
        dialog.cancelText = cancelText;
        dialog.iconRes = iconRes != 0 ? iconRes : R.drawable.ic_delete;
        dialog.isDestructive = isDestructive;
        dialog.isDialogCancelable = cancelable;
        dialog.onConfirmListener = onConfirmListener;
        return dialog;
    }

    public static AppConfirmationDialogFragment newInstance(
            String title, String message, String confirmText, String cancelText,
            int iconRes, boolean isDestructive, OnConfirmListener onConfirmListener) {
        return newInstance(title, message, confirmText, cancelText, iconRes, isDestructive, true, onConfirmListener);
    }

    public static void show(
            FragmentManager fm, String title, String message, String confirmText, String cancelText,
            int iconRes, boolean isDestructive, OnConfirmListener onConfirmListener) {
        if (fm == null || fm.isStateSaved() || fm.isDestroyed()) return;
        AppConfirmationDialogFragment dialog = newInstance(title, message, confirmText, cancelText, iconRes, isDestructive, true, onConfirmListener);
        dialog.show(fm, TAG);
    }

    public static void showDeleteConfirm(FragmentManager fm, String title, String message, OnConfirmListener onConfirmListener) {
        show(fm, title, message, "Delete", "Cancel", R.drawable.ic_delete, true, onConfirmListener);
    }

    public static void showSuccess(FragmentManager fm, String title, String message, Runnable onConfirm) {
        showSuccess(fm, title, message, "OK", true, onConfirm);
    }

    public static void showSuccess(FragmentManager fm, String title, String message, String confirmText, boolean cancelable, Runnable onConfirm) {
        if (fm == null || fm.isStateSaved() || fm.isDestroyed()) return;
        AppConfirmationDialogFragment dialog = newInstance(title, message, confirmText, null, R.drawable.ic_check, false, cancelable, () -> {
            if (onConfirm != null) onConfirm.run();
        });
        dialog.show(fm, TAG);
    }

    public static void showInfo(FragmentManager fm, String title, String message) {
        showInfo(fm, title, message, null);
    }

    public static void showInfo(FragmentManager fm, String title, String message, Runnable onConfirm) {
        if (fm == null || fm.isStateSaved() || fm.isDestroyed()) return;
        AppConfirmationDialogFragment dialog = newInstance(title, message, "OK", null, R.drawable.ic_cancel_circle, false, true, () -> {
            if (onConfirm != null) onConfirm.run();
        });
        dialog.show(fm, TAG);
    }

    public static void showError(FragmentManager fm, String title, String message) {
        show(fm, title, message, "OK", null, R.drawable.ic_cancel_circle, true, null);
    }

    public static void showErrorWithRetry(FragmentManager fm, String title, String message, Runnable retryAction) {
        show(fm, title, message, "Try Again", "Cancel", R.drawable.ic_cancel_circle, false, () -> {
            if (retryAction != null) retryAction.run();
        });
    }

    public static void showRewardedAction(FragmentManager fm, String title, String message, String confirmText, Runnable onConfirm) {
        if (fm == null || fm.isStateSaved() || fm.isDestroyed()) return;
        AppConfirmationDialogFragment dialog = newInstance(title, message, confirmText, "Cancel", R.drawable.ic_play, false, true, () -> {
            if (onConfirm != null) onConfirm.run();
        });
        dialog.show(fm, TAG);
    }

    private DialogAppConfirmationBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE, title);
            message = getArguments().getString(ARG_MESSAGE, message);
            confirmText = getArguments().getString(ARG_CONFIRM, confirmText);
            cancelText = getArguments().getString(ARG_CANCEL, cancelText);
            iconRes = getArguments().getInt(ARG_ICON, iconRes);
            isDestructive = getArguments().getBoolean(ARG_DESTRUCTIVE, isDestructive);
            isDialogCancelable = getArguments().getBoolean(ARG_CANCELABLE, isDialogCancelable);
        }
        setCancelable(isDialogCancelable);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogAppConfirmationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.dialogTitle.setText(title);
        binding.dialogMessage.setText(message);
        binding.btnConfirm.setText(confirmText);

        if (cancelText != null && !cancelText.trim().isEmpty()) {
            binding.btnCancel.setText(cancelText);
            binding.btnCancel.setVisibility(View.VISIBLE);
        } else {
            binding.btnCancel.setVisibility(View.GONE);
            android.widget.LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams) binding.btnConfirm.getLayoutParams();
            lp.setMarginStart(0);
            binding.btnConfirm.setLayoutParams(lp);
        }

        if (iconRes != 0) {
            binding.dialogIcon.setImageResource(iconRes);
        }

        if (isDestructive) {
            int dangerColor = ContextCompat.getColor(requireContext(), R.color.danger);
            binding.dialogIconCard.setCardBackgroundColor(Color.argb(35, 245, 101, 74));
            binding.dialogIcon.setImageTintList(ColorStateList.valueOf(dangerColor));
            binding.btnConfirm.setBackgroundTintList(ColorStateList.valueOf(dangerColor));
        } else if (iconRes == R.drawable.ic_check) {
            int successColor = ContextCompat.getColor(requireContext(), R.color.success);
            binding.dialogIconCard.setCardBackgroundColor(Color.argb(35, 34, 197, 94));
            binding.dialogIcon.setImageTintList(ColorStateList.valueOf(successColor));
            int accent = ThemeManager.getAccentColorInt();
            binding.btnConfirm.setBackgroundTintList(ColorStateList.valueOf(accent));
        } else {
            int accent = ThemeManager.getAccentColorInt();
            binding.dialogIconCard.setCardBackgroundColor(Color.argb(35, Color.red(accent), Color.green(accent), Color.blue(accent)));
            binding.dialogIcon.setImageTintList(ColorStateList.valueOf(accent));
            binding.btnConfirm.setBackgroundTintList(ColorStateList.valueOf(accent));
        }

        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnConfirm.setOnClickListener(v -> {
            dismiss();
            if (onConfirmListener != null) {
                onConfirmListener.onConfirmed();
            }
        });
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
}
