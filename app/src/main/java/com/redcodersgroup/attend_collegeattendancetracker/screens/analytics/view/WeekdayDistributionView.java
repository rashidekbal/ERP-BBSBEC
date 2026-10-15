package com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.model.DayOfWeekAnalyticsModel;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual bar distribution view rendering attendance rate per day of the week (Monday through Saturday).
 */
public class WeekdayDistributionView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bestBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint worstBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pctPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF trackRect = new RectF();
    private final RectF barRect = new RectF();

    private List<DayOfWeekAnalyticsModel> days = new ArrayList<>();
    private float animationFraction = 1f;
    private android.animation.ValueAnimator animator;

    public WeekdayDistributionView(Context context) {
        super(context);
        init();
    }

    public WeekdayDistributionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeekdayDistributionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        int accent = ThemeManager.getAccentColorInt();
        int surfaceColor = ContextCompat.getColor(getContext(), R.color.surface2);
        int textColor = ContextCompat.getColor(getContext(), R.color.text_dim);
        int dangerColor = ContextCompat.getColor(getContext(), R.color.danger);

        trackPaint.setStyle(Paint.Style.FILL);
        trackPaint.setColor(surfaceColor);

        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setColor(accent);

        bestBarPaint.setStyle(Paint.Style.FILL);
        bestBarPaint.setColor(accent);

        worstBarPaint.setStyle(Paint.Style.FILL);
        worstBarPaint.setColor(dangerColor);

        labelPaint.setTextSize(dpToPx(11f));
        labelPaint.setColor(textColor);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);

        pctPaint.setTextSize(dpToPx(10f));
        pctPaint.setColor(textColor);
        pctPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setDays(List<DayOfWeekAnalyticsModel> list) {
        this.days = list != null ? list : new ArrayList<>();
        init();
        invalidate();
    }

    public void resetToZero() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        animationFraction = 0f;
        invalidate();
    }

    public void animateBars() {
        if (animator != null) {
            animator.cancel();
        }
        animationFraction = 0f;
        invalidate();
        animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(600);
        animator.setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator());
        animator.addUpdateListener(animation -> {
            animationFraction = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (days == null || days.isEmpty()) return;

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        int count = days.size();
        float columnWidth = (float) w / count;
        float barWidth = Math.min(dpToPx(28f), columnWidth * 0.48f);
        float cornerRadius = dpToPx(6f);

        float topPadding = dpToPx(22f);
        float bottomPadding = dpToPx(26f);
        float chartHeight = h - topPadding - bottomPadding;

        for (int i = 0; i < count; i++) {
            DayOfWeekAnalyticsModel model = days.get(i);
            float centerX = (i + 0.5f) * columnWidth;
            float left = centerX - barWidth / 2f;
            float right = centerX + barWidth / 2f;
            float top = topPadding;
            float bottom = topPadding + chartHeight;

            // Draw track (background)
            trackRect.set(left, top, right, bottom);
            canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint);

            // Calculate fill height
            float pct = Math.max(0, Math.min(100, model.getPercentage()));
            float fillHeight = chartHeight * (pct / 100f) * animationFraction;
            float barTop = bottom - fillHeight;

            if (fillHeight > 0) {
                barRect.set(left, barTop, right, bottom);
                Paint currentBarPaint = barPaint;
                if (model.isWorstDay() && model.getConducted() > 0 && model.getPercentage() < 75) {
                    currentBarPaint = worstBarPaint;
                } else if (model.isBestDay() && model.getConducted() > 0) {
                    currentBarPaint = bestBarPaint;
                }
                canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, currentBarPaint);
            }

            // Draw percentage label at top
            String pctText = model.getConducted() > 0 ? (model.getPercentage() + "%") : "-";
            canvas.drawText(pctText, centerX, topPadding - dpToPx(6f), pctPaint);

            // Draw day label at bottom
            canvas.drawText(model.getDayLabel(), centerX, h - dpToPx(8f), labelPaint);
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
