package com.redcodersgroup.attend_collegeattendancetracker.utils;

import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.progressindicator.BaseProgressIndicator;

/**
 * Utility helper to smoothly animate progress bars and indicators from 0 to their target values.
 */
public final class ProgressAnimationUtil {

    public static final int DEFAULT_DURATION_MS = 650;

    private ProgressAnimationUtil() {
        // Utility class
    }

    /**
     * Animate a BaseProgressIndicator (CircularProgressIndicator, LinearProgressIndicator)
     * from 0 to the target progress, optionally updating a companion percentage TextView.
     *
     * @param indicator The Material ProgressIndicator to animate.
     * @param targetPct The destination percentage / progress (0-100).
     * @param percentTextView Optional companion TextView to update with live percentage during animation (can be null).
     */
    public static void animateProgress(BaseProgressIndicator<?> indicator, int targetPct, TextView percentTextView) {
        animateProgress(indicator, targetPct, percentTextView, DEFAULT_DURATION_MS);
    }

    /**
     * Animate a BaseProgressIndicator from 0 to target progress with custom duration.
     */
    public static void animateProgress(BaseProgressIndicator<?> indicator, int targetPct, TextView percentTextView, int durationMs) {
        if (indicator == null) return;
        final int target = Math.max(0, Math.min(100, targetPct));

        // Cancel previous animator if running
        Object prevAnimator = indicator.getTag(com.redcodersgroup.attend_collegeattendancetracker.R.id.progress_animator_tag);
        if (prevAnimator instanceof ValueAnimator) {
            ((ValueAnimator) prevAnimator).cancel();
        }

        indicator.setProgressCompat(0, false);
        if (percentTextView != null) {
            percentTextView.setText("0%");
        }

        if (target == 0) {
            return;
        }

        ValueAnimator animator = ValueAnimator.ofInt(0, target);
        animator.setDuration(durationMs);
        animator.setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator());
        animator.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            indicator.setProgressCompat(val, false);
            if (percentTextView != null) {
                percentTextView.setText(val + "%");
            }
        });

        indicator.setTag(com.redcodersgroup.attend_collegeattendancetracker.R.id.progress_animator_tag, animator);
        animator.start();
    }

    /**
     * Animate a standard Android ProgressBar from 0 to target value.
     */
    public static void animateProgressBar(ProgressBar progressBar, int targetPct, TextView percentTextView) {
        animateProgressBar(progressBar, targetPct, percentTextView, DEFAULT_DURATION_MS);
    }

    /**
     * Animate a standard Android ProgressBar from 0 to target value with custom duration.
     */
    public static void animateProgressBar(ProgressBar progressBar, int targetPct, TextView percentTextView, int durationMs) {
        if (progressBar == null) return;
        final int target = Math.max(0, Math.min(100, targetPct));

        Object prevAnimator = progressBar.getTag(com.redcodersgroup.attend_collegeattendancetracker.R.id.progress_animator_tag);
        if (prevAnimator instanceof ValueAnimator) {
            ((ValueAnimator) prevAnimator).cancel();
        }

        progressBar.setProgress(0);
        if (percentTextView != null) {
            percentTextView.setText("0%");
        }

        if (target == 0) {
            return;
        }

        ValueAnimator animator = ValueAnimator.ofInt(0, target);
        animator.setDuration(durationMs);
        animator.setInterpolator(new DecelerateInterpolator(1.5f));
        animator.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            progressBar.setProgress(val);
            if (percentTextView != null) {
                percentTextView.setText(val + "%");
            }
        });

        progressBar.setTag(com.redcodersgroup.attend_collegeattendancetracker.R.id.progress_animator_tag, animator);
        animator.start();
    }
}
