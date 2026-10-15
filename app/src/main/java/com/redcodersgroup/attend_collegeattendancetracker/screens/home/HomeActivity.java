package com.redcodersgroup.attend_collegeattendancetracker.screens.home;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.constants.FragmentIds;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivityHomeBinding;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.AddDataHomeBottomsheetBinding;
import com.redcodersgroup.attend_collegeattendancetracker.notifications.NotificationScheduler;
import com.redcodersgroup.attend_collegeattendancetracker.screens.addAssignment.AddAssignmentDialogFragment;
import com.redcodersgroup.attend_collegeattendancetracker.screens.addMarks.AddMarksDialogFragment;
import com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.CompleteProfileBottomSheet;
import com.redcodersgroup.attend_collegeattendancetracker.screens.holiday.AddHolidayDialogFragment;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.AttendanceFragment;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.home.HomeFragment;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.marks.MarksFragment;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.sidebar.SideBarFragment;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.task.TaskFragment;
import com.redcodersgroup.attend_collegeattendancetracker.screens.subject.AddSubjectDialogFragment;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ProfileUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.util.ArrayDeque;
import java.util.Deque;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    private FragmentManager fragmentManager;
    ActionBarDrawerToggle drawerToggle;
    private String currentFragment = FragmentIds.HOME_FRAGMENT.toString();
    private int selectedNavId = R.id.home;

    // Cached fragment instances for zero-inflation tab switching
    private Fragment homeFragment;
    private Fragment attendanceFragment;
    private Fragment taskFragment;
    private Fragment marksFragment;
    private final Deque<Integer> tabBackStack = new ArrayDeque<>();

    // Smooth bottom nav animation state
    private ValueAnimator navAnimator;
    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
    private final int[] currentBgColors = new int[4];
    private final int[] currentIconColors = new int[4];
    private int uniformExpandedWidth = 0;
    private boolean colorsInitialized = false;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Attendance reminders removed; notifications used for assignments and holiday alerts
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        init();
        setupWindowInsets();
        setupNavigationSystem();
        setUpDrawerLayout();
        handleBackPress();
        setEventListeners();
        requestNotificationPermissionIfNeeded();
        handleIntent(getIntent());
        observeAccentColor();

        // Pre-warm other tab fragments shortly after initial render so first tap is instant
        binding.mainContainer.postDelayed(this::prewarmFragmentsIfNeeded, 300);

        // Fresh launch sync: full data synced if 4-hour interval reached or never synced
        com.redcodersgroup.attend_collegeattendancetracker.network.ErpSyncManager.syncOnLaunchIfNeeded(new com.redcodersgroup.attend_collegeattendancetracker.network.ErpSyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                if (isFinishing() || isDestroyed()) return;
                runOnUiThread(() -> {
                    if (homeFragment instanceof HomeFragment) {
                        ((HomeFragment) homeFragment).refreshData();
                    }
                });
            }

            @Override
            public void onError(String message) {
                // Background sync failure handled silently
            }
        });
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation, (v, windowInsets) -> {
            Insets navBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            // For gesture navigation (small bottom inset <= 28dp), sit sleekly right above the gesture pill (12dp).
            // For 3-button navigation (large bottom inset > 28dp), clear the navigation bar + 6dp.
            int gestureThresholdPx = dpToPx(28);
            if (navBarInsets.bottom > gestureThresholdPx) {
                lp.bottomMargin = navBarInsets.bottom + dpToPx(6);
            } else {
                lp.bottomMargin = Math.max(dpToPx(10), navBarInsets.bottom > 0 ? dpToPx(14) : dpToPx(12));
            }
            v.setLayoutParams(lp);
            return windowInsets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBottomNavVisuals(selectedNavId, false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("NAVIGATE_TO_TASK", false)) {
            navigateToTask();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setEventListeners() {
        binding.actionBtn.setOnClickListener(v -> showActionBottomSheet());
    }

    private void showActionBottomSheet() {
        if (binding != null) {
            binding.actionBtnIcon.animate()
                    .rotation(45f)
                    .setDuration(220)
                    .setInterpolator(new OvershootInterpolator(1.4f))
                    .start();
        }

        AddDataHomeBottomsheetBinding bottomSheetBinding = AddDataHomeBottomsheetBinding.inflate(getLayoutInflater());
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.TransparentBottomSheet);
        FrameLayout frameLayout = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (frameLayout != null) {
            BottomSheetBehavior.from(frameLayout);
        }
        bottomSheetDialog.setContentView(bottomSheetBinding.getRoot());
        bottomSheetDialog.setOnDismissListener(dialog -> {
            if (binding != null) {
                binding.actionBtnIcon.animate()
                        .rotation(0f)
                        .setDuration(220)
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .start();
            }
        });
        bottomSheetDialog.show();
        bottomSheetDialog.setCanceledOnTouchOutside(true);

        bottomSheetBinding.addAssignmentBtn.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            AddAssignmentDialogFragment.showDialog(getSupportFragmentManager());
        });
        bottomSheetBinding.addSubjectBtn.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            if (!ProfileUtil.isProfileCompletedBareMinimum()) {
                ToastUtil.showToastShort("Please complete your profile basics before adding subjects");
                CompleteProfileBottomSheet.show(getSupportFragmentManager(), () -> {
                    AddSubjectDialogFragment.showDialog(getSupportFragmentManager());
                });
                return;
            }
            AddSubjectDialogFragment.showDialog(getSupportFragmentManager());
        });
        bottomSheetBinding.addMarksBtn.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            AddMarksDialogFragment.showDialog(getSupportFragmentManager());
        });
        bottomSheetBinding.addHolidayBtn.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            AddHolidayDialogFragment.showDialog(getSupportFragmentManager());
        });
    }

    private void handleBackPress() {
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.main.isDrawerOpen(GravityCompat.START)) {
                    binding.main.closeDrawer(GravityCompat.START);
                    return;
                }
                if (tabBackStack.size() > 1) {
                    tabBackStack.pop();
                    Integer prevTabId = tabBackStack.peek();
                    if (prevTabId != null) {
                        selectNavTabInternal(prevTabId, true, false);
                        return;
                    }
                }
                if (selectedNavId != R.id.home) {
                    tabBackStack.clear();
                    tabBackStack.push(R.id.home);
                    selectNavTabInternal(R.id.home, true, false);
                    return;
                }
                finish();
            }
        });
    }

    private void setUpDrawerLayout() {
        if (fragmentManager.findFragmentById(binding.sideBarLayout.getId()) == null) {
            fragmentManager.beginTransaction()
                    .replace(binding.sideBarLayout.getId(), new SideBarFragment())
                    .commit();
        }
    }

    private void setupNavigationSystem() {
        // Restore any existing fragments across config changes
        homeFragment = fragmentManager.findFragmentByTag(FragmentIds.HOME_FRAGMENT.toString());
        attendanceFragment = fragmentManager.findFragmentByTag(FragmentIds.ATTENDANCE_FRAGMENT.toString());
        taskFragment = fragmentManager.findFragmentByTag(FragmentIds.TASK_FRAGMENT.toString());
        marksFragment = fragmentManager.findFragmentByTag(FragmentIds.MARKS_FRAGMENT.toString());

        binding.home.setOnClickListener(v -> selectNavTab(R.id.home, true));
        binding.attendance.setOnClickListener(v -> selectNavTab(R.id.attendance, true));
        binding.task.setOnClickListener(v -> selectNavTab(R.id.task, true));
        binding.marks.setOnClickListener(v -> selectNavTab(R.id.marks, true));

        tabBackStack.clear();
        tabBackStack.push(R.id.home);
        selectNavTabInternal(R.id.home, false, false);
    }

    private void prewarmFragmentsIfNeeded() {
        if (isFinishing() || isDestroyed() || fragmentManager.isStateSaved()) return;
        FragmentTransaction tx = fragmentManager.beginTransaction();
        boolean changed = false;

        if (attendanceFragment == null) {
            attendanceFragment = new AttendanceFragment();
            tx.add(binding.mainContainer.getId(), attendanceFragment, FragmentIds.ATTENDANCE_FRAGMENT.toString())
                    .hide(attendanceFragment)
                    .setMaxLifecycle(attendanceFragment, Lifecycle.State.STARTED);
            changed = true;
        }
        if (taskFragment == null) {
            taskFragment = new TaskFragment();
            tx.add(binding.mainContainer.getId(), taskFragment, FragmentIds.TASK_FRAGMENT.toString())
                    .hide(taskFragment)
                    .setMaxLifecycle(taskFragment, Lifecycle.State.STARTED);
            changed = true;
        }
        if (marksFragment == null) {
            marksFragment = new MarksFragment();
            tx.add(binding.mainContainer.getId(), marksFragment, FragmentIds.MARKS_FRAGMENT.toString())
                    .hide(marksFragment)
                    .setMaxLifecycle(marksFragment, Lifecycle.State.STARTED);
            changed = true;
        }

        if (changed) {
            tx.commitNowAllowingStateLoss();
        }
    }

    private void selectNavTab(int itemId, boolean animate) {
        selectNavTabInternal(itemId, animate, true);
    }

    private void selectNavTabInternal(int itemId, boolean animate, boolean recordBackStack) {
        if (selectedNavId == itemId && colorsInitialized) {
            return;
        }
        selectedNavId = itemId;
        if (recordBackStack) {
            tabBackStack.remove(itemId);
            tabBackStack.push(itemId);
        }
        updateBottomNavVisuals(itemId, animate);
        switchVisibleFragment(itemId, animate);
    }

    private void switchVisibleFragment(int itemId, boolean animate) {
        if (fragmentManager.isStateSaved()) return;

        Fragment target;
        String targetTag;

        if (itemId == R.id.attendance) {
            if (attendanceFragment == null) {
                attendanceFragment = new AttendanceFragment();
            }
            target = attendanceFragment;
            targetTag = FragmentIds.ATTENDANCE_FRAGMENT.toString();
        } else if (itemId == R.id.task) {
            if (taskFragment == null) {
                taskFragment = new TaskFragment();
            }
            target = taskFragment;
            targetTag = FragmentIds.TASK_FRAGMENT.toString();
        } else if (itemId == R.id.marks) {
            if (marksFragment == null) {
                marksFragment = new MarksFragment();
            }
            target = marksFragment;
            targetTag = FragmentIds.MARKS_FRAGMENT.toString();
        } else {
            if (homeFragment == null) {
                homeFragment = new HomeFragment();
            }
            target = homeFragment;
            targetTag = FragmentIds.HOME_FRAGMENT.toString();
        }

        currentFragment = targetTag;

        FragmentTransaction tx = fragmentManager.beginTransaction();
        Fragment[] all = new Fragment[]{homeFragment, attendanceFragment, taskFragment, marksFragment};
        for (Fragment f : all) {
            if (f != null && f != target && f.isAdded()) {
                tx.hide(f).setMaxLifecycle(f, Lifecycle.State.STARTED);
            }
        }

        if (!target.isAdded()) {
            tx.add(binding.mainContainer.getId(), target, targetTag);
        } else {
            tx.show(target);
        }
        tx.setMaxLifecycle(target, Lifecycle.State.RESUMED);
        tx.commitNowAllowingStateLoss();

        // Hardware-accelerated smooth fade + gentle upward glide on fragment container view
        View targetView = target.getView();
        if (animate && targetView != null) {
            targetView.animate().cancel();
            targetView.setAlpha(0.15f);
            targetView.setTranslationY(dpToPx(6));
            targetView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(190)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .withLayer()
                    .start();
        }
    }

    private void ensureUniformExpandedWidth(View[] innerRows, View[] badges) {
        if (uniformExpandedWidth > 0) return;
        int maxBadgeW = 0;
        int badgeHeightPx = getResources().getDimensionPixelSize(R.dimen.dp_36);
        for (View badge : badges) {
            badge.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(badgeHeightPx, View.MeasureSpec.EXACTLY)
            );
            maxBadgeW = Math.max(maxBadgeW, badge.getMeasuredWidth());
        }
        int padStart = getResources().getDimensionPixelSize(R.dimen.dp_12);
        int iconW = getResources().getDimensionPixelSize(R.dimen.dp_22);
        int margin = getResources().getDimensionPixelSize(R.dimen.dp_6);
        int padEnd = getResources().getDimensionPixelSize(R.dimen.dp_8);
        uniformExpandedWidth = padStart + iconW + margin + maxBadgeW + padEnd;

        for (int i = 0; i < 4; i++) {
            ViewGroup.LayoutParams badgeLp = badges[i].getLayoutParams();
            if (badgeLp != null) {
                badgeLp.width = maxBadgeW;
                badgeLp.height = badgeHeightPx;
                badges[i].setLayoutParams(badgeLp);
            }
            ViewGroup.LayoutParams innerLp = innerRows[i].getLayoutParams();
            if (innerLp != null) {
                innerLp.width = uniformExpandedWidth;
                innerRows[i].setLayoutParams(innerLp);
            }
        }
    }

    private void updateBottomNavVisuals(int activeId, boolean animate) {
        if (binding == null) return;

        if (navAnimator != null && navAnimator.isRunning()) {
            navAnimator.cancel();
        }

        int accent = ThemeManager.getAccentColorInt();
        int softCircleBg = Color.argb(32, Color.red(accent), Color.green(accent), Color.blue(accent));
        int collapsedW = getResources().getDimensionPixelSize(R.dimen.dp_52);
        int iconW = getResources().getDimensionPixelSize(R.dimen.dp_22);
        int collapsedPadStart = Math.max(0, (collapsedW - iconW) / 2);
        int expandedPadStart = getResources().getDimensionPixelSize(R.dimen.dp_12);
        int padEnd = getResources().getDimensionPixelSize(R.dimen.dp_8);

        final MaterialCardView[] cards = new MaterialCardView[]{
                binding.home,
                binding.attendance,
                binding.task,
                binding.marks
        };
        final View[] innerRows = new View[]{
                binding.navHomeInner,
                binding.navAttendanceInner,
                binding.navTaskInner,
                binding.navMarksInner
        };
        final AppCompatImageView[] icons = new AppCompatImageView[]{
                binding.navHomeIcon,
                binding.navAttendanceIcon,
                binding.navTaskIcon,
                binding.navMarksIcon
        };
        final View[] badges = new View[]{
                binding.navHomeBadge,
                binding.navAttendanceBadge,
                binding.navTaskBadge,
                binding.navMarksBadge
        };
        final int[] tabIds = new int[]{
                R.id.home,
                R.id.attendance,
                R.id.task,
                R.id.marks
        };

        ensureUniformExpandedWidth(innerRows, badges);
        final int expandedW = uniformExpandedWidth;

        final int[] startWidths = new int[4];
        final int[] endWidths = new int[4];
        final int[] startPadStarts = new int[4];
        final int[] endPadStarts = new int[4];
        final float[] startAlphas = new float[4];
        final float[] endAlphas = new float[4];
        final int[] startBg = new int[4];
        final int[] endBg = new int[4];
        final int[] startIcon = new int[4];
        final int[] endIcon = new int[4];

        for (int i = 0; i < 4; i++) {
            boolean isActive = (tabIds[i] == activeId);
            endWidths[i] = isActive ? expandedW : collapsedW;
            endPadStarts[i] = isActive ? expandedPadStart : collapsedPadStart;
            endAlphas[i] = isActive ? 1f : 0f;
            endBg[i] = isActive ? accent : softCircleBg;
            endIcon[i] = isActive ? Color.WHITE : accent;

            ViewGroup.LayoutParams lp = cards[i].getLayoutParams();
            startWidths[i] = (colorsInitialized && lp.width > 0) ? lp.width : endWidths[i];
            startPadStarts[i] = colorsInitialized ? innerRows[i].getPaddingStart() : endPadStarts[i];
            startAlphas[i] = colorsInitialized ? badges[i].getAlpha() : endAlphas[i];
            startBg[i] = colorsInitialized ? currentBgColors[i] : endBg[i];
            startIcon[i] = colorsInitialized ? currentIconColors[i] : endIcon[i];
        }

        if (!animate || !colorsInitialized) {
            for (int i = 0; i < 4; i++) {
                ViewGroup.LayoutParams lp = cards[i].getLayoutParams();
                lp.width = endWidths[i];
                cards[i].setLayoutParams(lp);
                innerRows[i].setPaddingRelative(endPadStarts[i], 0, padEnd, 0);
                cards[i].setCardBackgroundColor(endBg[i]);
                icons[i].setImageTintList(ColorStateList.valueOf(endIcon[i]));
                badges[i].setAlpha(endAlphas[i]);
                badges[i].setScaleX(endAlphas[i] > 0.5f ? 1f : 0.85f);
                badges[i].setScaleY(endAlphas[i] > 0.5f ? 1f : 0.85f);
                currentBgColors[i] = endBg[i];
                currentIconColors[i] = endIcon[i];
            }
            colorsInitialized = true;
            binding.bottomNavRow.requestLayout();
        } else {
            for (int i = 0; i < 4; i++) {
                if (tabIds[i] == activeId) {
                    icons[i].setScaleX(0.88f);
                    icons[i].setScaleY(0.88f);
                    icons[i].animate()
                            .scaleX(1.04f)
                            .scaleY(1.04f)
                            .setDuration(230)
                            .setInterpolator(new OvershootInterpolator(1.8f))
                            .start();
                } else {
                    icons[i].animate().cancel();
                    icons[i].setScaleX(1.0f);
                    icons[i].setScaleY(1.0f);
                }
            }

            navAnimator = ValueAnimator.ofFloat(0f, 1f);
            navAnimator.setDuration(250);
            navAnimator.setInterpolator(new FastOutSlowInInterpolator());
            navAnimator.addUpdateListener(anim -> {
                float f = (float) anim.getAnimatedValue();
                for (int i = 0; i < 4; i++) {
                    if (startWidths[i] != endWidths[i]) {
                        ViewGroup.LayoutParams lp = cards[i].getLayoutParams();
                        int delta = Math.round(Math.abs(endWidths[i] - startWidths[i]) * f);
                        lp.width = endWidths[i] > startWidths[i]
                                ? (startWidths[i] + delta)
                                : (startWidths[i] - delta);
                        cards[i].setLayoutParams(lp);
                    }
                    if (startPadStarts[i] != endPadStarts[i]) {
                        int padS = Math.round(startPadStarts[i] + (endPadStarts[i] - startPadStarts[i]) * f);
                        innerRows[i].setPaddingRelative(padS, 0, padEnd, 0);
                    }

                    if (startBg[i] != endBg[i]) {
                        int bg = (int) argbEvaluator.evaluate(f, startBg[i], endBg[i]);
                        cards[i].setCardBackgroundColor(bg);
                        currentBgColors[i] = bg;
                    }
                    if (startIcon[i] != endIcon[i]) {
                        int ic = (int) argbEvaluator.evaluate(f, startIcon[i], endIcon[i]);
                        icons[i].setImageTintList(ColorStateList.valueOf(ic));
                        currentIconColors[i] = ic;
                    }
                    if (startAlphas[i] != endAlphas[i]) {
                        float a = startAlphas[i] + (endAlphas[i] - startAlphas[i]) * f;
                        badges[i].setAlpha(a);
                        float s = 0.85f + 0.15f * a;
                        badges[i].setScaleX(s);
                        badges[i].setScaleY(s);
                    }
                }
                binding.bottomNavRow.requestLayout();
            });
            navAnimator.start();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.bottomNavigation.setOutlineAmbientShadowColor(Color.argb(60, 0, 0, 0));
            binding.bottomNavigation.setOutlineSpotShadowColor(Color.argb(90, 0, 0, 0));
        }
    }

    private void observeAccentColor() {
        ThemeManager.getAccentColorLiveData().observe(this, colorHex -> {
            if (colorHex != null) {
                colorsInitialized = false;
                updateBottomNavVisuals(selectedNavId, false);
            }
        });
    }

    private void init() {
        this.fragmentManager = getSupportFragmentManager();
        drawerToggle = new ActionBarDrawerToggle(this, binding.main, R.string.open_drawer, R.string.close_drawer);
    }

    public void openDrawer() {
        binding.main.openDrawer(GravityCompat.START);
    }

    public void closeDrawer() {
        binding.main.closeDrawer(GravityCompat.START);
    }

    public void navigateToTask() {
        selectNavTab(R.id.task, true);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}