package com.redcodersgroup.attend_collegeattendancetracker.screens.onBoarding;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.FragmentWelcomeBinding;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.HomeActivity;
import android.util.Log;

import com.redcodersgroup.attend_collegeattendancetracker.screens.privacy.PrivacyPolicyActivity;
import com.redcodersgroup.attend_collegeattendancetracker.screens.terms.TermsActivity;
import com.redcodersgroup.attend_collegeattendancetracker.services.ImportService;
import com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil;

public class WelcomeFragment extends Fragment {
    private FragmentWelcomeBinding binding;

    private final ActivityResultLauncher<String[]> importFileLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null && getContext() != null) {
                    showImportConfirmDialog(uri);
                }
            });

    public WelcomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentWelcomeBinding.inflate(inflater, container, false);
        setupTermsAndConditions();
        binding.nextBtn.setOnClickListener(this::handleNextBtnClick);
        binding.restoreBtn.setOnClickListener(v -> importFileLauncher.launch(new String[]{"application/json", "*/*"}));
        return binding.getRoot();
    }

    private void setupTermsAndConditions() {
        if (getContext() == null) return;
        String prefix = getString(R.string.terms_agreement_prefix);
        String terms =getString(R.string.terms_agreement_terms);
        String and = " " +getString(R.string.terms_agreement_and)+ " ";
        String privacy = getString(R.string.terms_agreement_privacy);

        String fullText = prefix + terms +and + privacy + ".";
        SpannableString spannable = new SpannableString(fullText);

        int termsStart = prefix.length();
        int termsEnd = termsStart + terms.length();
        int privacyStart = termsEnd + and.length();
        int privacyEnd = privacyStart + privacy.length();

        int accentColor = ContextCompat.getColor(requireContext(), R.color.accent);

        ClickableSpan termsSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showTermsDialog();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(accentColor);
            }
        };

        ClickableSpan privacySpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Intent intent = new Intent(requireContext(), PrivacyPolicyActivity.class);
                startActivity(intent);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(accentColor);
            }
        };

        spannable.setSpan(termsSpan, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(accentColor), termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannable.setSpan(privacySpan, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(accentColor), privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        binding.termsText.setText(spannable);
        binding.termsText.setMovementMethod(LinkMovementMethod.getInstance());
        binding.termsText.setHighlightColor(Color.TRANSPARENT);

        // Clicking container toggles checkbox unless clicking links
        binding.termsAgreementContainer.setOnClickListener(v -> {
            binding.termsCheckbox.setChecked(!binding.termsCheckbox.isChecked());
            updateNextBtnVisualState();
        });

        binding.termsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateNextBtnVisualState());
        updateNextBtnVisualState();
    }

    private void showTermsDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_terms_and_conditions, null, false);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View closeBtn = dialogView.findViewById(R.id.dialogCloseBtn);
        View declineBtn = dialogView.findViewById(R.id.dialogDeclineBtn);
        View acceptBtn = dialogView.findViewById(R.id.dialogAcceptBtn);

        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> dialog.dismiss());
        }

        if (declineBtn != null) {
            declineBtn.setOnClickListener(v -> {
                if (binding != null) {
                    binding.termsCheckbox.setChecked(false);
                }
                dialog.dismiss();
            });
        }

        if (acceptBtn != null) {
            acceptBtn.setOnClickListener(v -> {
                if (binding != null) {
                    binding.termsCheckbox.setChecked(true);
                }
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void updateNextBtnVisualState() {
        if (binding == null) return;
        boolean agreed = binding.termsCheckbox.isChecked();
        binding.nextBtn.setAlpha(agreed ? 1.0f : 0.6f);
    }

    private void handleNextBtnClick(View view) {
        if (!binding.termsCheckbox.isChecked()) {
            Toast.makeText(requireContext(), R.string.terms_error_must_agree, Toast.LENGTH_LONG).show();
            // Subtle bounce or highlight feedback
            binding.termsAgreementContainer.animate().scaleX(1.03f).scaleY(1.03f).setDuration(120).withEndAction(() -> {
                if (binding != null) {
                    binding.termsAgreementContainer.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                }
            }).start();
            return;
        }
        SharedPrefUtil.setSetupComplete(true);
        Intent intent = new Intent(requireActivity(), HomeActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    private void showImportConfirmDialog(Uri uri) {
        if (getContext() == null || isStateSaved()) return;
        com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.show(
                getChildFragmentManager(),
                "Restore Backup Data?",
                "This will restore your subjects, attendance history, assignments, and settings from the selected backup file.",
                "Restore",
                "Cancel",
                R.drawable.ic_cloud_upload,
                false,
                () -> performImport(uri)
        );
    }

    private void performImport(Uri uri) {
        if (getContext() == null) return;
        Toast.makeText(requireContext(), "Importing data...", Toast.LENGTH_SHORT).show();
        ImportService.importData(requireContext(), uri, new ImportService.ImportCallback() {
            @Override
            public void onSuccess() {
                SharedPrefUtil.setSetupComplete(true);
                if (getContext() == null || isStateSaved()) return;
                com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.showSuccess(
                        getChildFragmentManager(),
                        "Import Successful",
                        "Your backup data has been restored successfully.",
                        "Continue to App",
                        false,
                        () -> {
                            Intent intent = new Intent(requireContext(), HomeActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            if (getActivity() != null) {
                                getActivity().finish();
                            }
                        }
                );
            }

            @Override
            public void onError(String msg) {
                Log.e("WelcomeFragment", "Import failed: " + msg);
                if (getContext() == null) return;
                Toast.makeText(requireContext(), "Import failed: " + msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}