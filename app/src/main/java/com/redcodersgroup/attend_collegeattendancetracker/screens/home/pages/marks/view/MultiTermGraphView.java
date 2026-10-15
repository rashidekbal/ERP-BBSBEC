package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.marks.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;

public class MultiTermGraphView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path curvePath = new Path();
    private final Path fillPath = new Path();

    private float pct1 = 41.7f;
    private float pct2 = 66.7f;
    private float pct3 = 91.7f;

    private String label1 = "10/24";
    private String label2 = "16/24";
    private String label3 = "22/24";

    private float animationFraction = 0f;
    private ValueAnimator graphAnimator;

    public MultiTermGraphView(Context context) {
        super(context);
        init();
    }

    public MultiTermGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MultiTermGraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        int accent = ThemeManager.getAccentColorInt();

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dpToPx(3f));
        linePaint.setColor(accent);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);

        dotOuterPaint.setStyle(Paint.Style.FILL);
        dotOuterPaint.setColor(accent);

        dotInnerPaint.setStyle(Paint.Style.FILL);
        dotInnerPaint.setColor(Color.WHITE);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dpToPx(1f));
        gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.border));
        gridPaint.setPathEffect(new DashPathEffect(new float[]{dpToPx(4), dpToPx(4)}, 0));

        textPaint.setTextSize(dpToPx(11f));
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    public void setTermScores(float p1, float p2, float p3, String l1, String l2, String l3) {
        this.pct1 = Math.max(0, Math.min(100, p1));
        this.pct2 = Math.max(0, Math.min(100, p2));
        this.pct3 = Math.max(0, Math.min(100, p3));
        this.label1 = l1;
        this.label2 = l2;
        this.label3 = l3;
        invalidate();
    }

    public void resetToZero() {
        if (graphAnimator != null && graphAnimator.isRunning()) {
            graphAnimator.cancel();
        }
        animationFraction = 0f;
        invalidate();
    }

    public void animateGraph() {
        if (graphAnimator != null && graphAnimator.isRunning()) {
            graphAnimator.cancel();
        }
        animationFraction = 0f;
        invalidate();
        graphAnimator = ValueAnimator.ofFloat(0f, 1f);
        graphAnimator.setDuration(600);
        graphAnimator.setInterpolator(new FastOutSlowInInterpolator());
        graphAnimator.addUpdateListener(anim -> {
            animationFraction = (float) anim.getAnimatedValue();
            invalidate();
        });
        graphAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        int accent = ThemeManager.getAccentColorInt();
        linePaint.setColor(accent);
        dotOuterPaint.setColor(accent);

        float topPadding = dpToPx(32f);
        float bottomPadding = dpToPx(28f);
        float chartH = h - topPadding - bottomPadding;

        // Draw horizontal grid lines (25%, 50%, 75%)
        for (int i = 1; i <= 3; i++) {
            float gy = topPadding + chartH * (1f - i * 0.25f);
            canvas.drawLine(dpToPx(16f), gy, w - dpToPx(16f), gy, gridPaint);
        }

        // 3 Key points
        float x1 = w * 0.18f;
        float x2 = w * 0.50f;
        float x3 = w * 0.82f;

        float animPct1 = pct1 * animationFraction;
        float animPct2 = pct2 * animationFraction;
        float animPct3 = pct3 * animationFraction;

        float y1 = topPadding + chartH * (1f - animPct1 / 100f);
        float y2 = topPadding + chartH * (1f - animPct2 / 100f);
        float y3 = topPadding + chartH * (1f - animPct3 / 100f);

        // Path creation
        curvePath.reset();
        curvePath.moveTo(x1, y1);
        curvePath.lineTo(x2, y2);
        curvePath.lineTo(x3, y3);

        // Fill path
        fillPath.reset();
        fillPath.moveTo(x1, y1);
        fillPath.lineTo(x2, y2);
        fillPath.lineTo(x3, y3);
        fillPath.lineTo(x3, h - bottomPadding);
        fillPath.lineTo(x1, h - bottomPadding);
        fillPath.close();

        int startGradient = Color.argb(60, Color.red(accent), Color.green(accent), Color.blue(accent));
        int endGradient = Color.argb(0, Color.red(accent), Color.green(accent), Color.blue(accent));
        fillPaint.setShader(new LinearGradient(0, topPadding, 0, h - bottomPadding, startGradient, endGradient, Shader.TileMode.CLAMP));

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(curvePath, linePaint);

        // Draw points and labels
        drawPoint(canvas, x1, y1, label1, "Mid 1 (" + Math.round(animPct1) + "%)", h - bottomPadding);
        drawPoint(canvas, x2, y2, label2, "Mid 2 (" + Math.round(animPct2) + "%)", h - bottomPadding);
        drawPoint(canvas, x3, y3, label3, "Mid 3 (" + Math.round(animPct3) + "%)", h - bottomPadding);
    }

    private void drawPoint(Canvas canvas, float x, float y, String scoreText, String bottomLabel, float axisY) {
        float outerR = dpToPx(6f);
        float innerR = dpToPx(3f);

        // Outer dot
        canvas.drawCircle(x, y, outerR, dotOuterPaint);
        // Inner dot
        canvas.drawCircle(x, y, innerR, dotInnerPaint);

        // Score Text above dot
        canvas.drawText(scoreText, x, y - dpToPx(10f), textPaint);

        // Term label below axis
        Paint subPaint = new Paint(textPaint);
        subPaint.setTextSize(dpToPx(10f));
        subPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_dim));
        subPaint.setFakeBoldText(false);
        canvas.drawText(bottomLabel, x, axisY + dpToPx(16f), subPaint);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
