package com.redcodersgroup.attend_collegeattendancetracker.screens.notifications;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivityNotificationsBinding;
import com.redcodersgroup.attend_collegeattendancetracker.network.NetworkClient;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.MarkNotificationReadRequest;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.NotificationActionResponse;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.NotificationsInboxResponse;
import com.redcodersgroup.attend_collegeattendancetracker.screens.notifications.adapter.NotificationAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.notifications.storage.NotificationStorage;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends AppCompatActivity {

    private ActivityNotificationsBinding binding;
    private NotificationAdapter adapter;
    private final List<NotificationsInboxResponse.NotificationItem> allNotifications = new ArrayList<>();
    private final List<NotificationsInboxResponse.NotificationItem> displayedNotifications = new ArrayList<>();
    private String currentFilter = "All";
    private long lastRefreshTime = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        loadCachedData();
        fetchNotifications(false);
    }

    private void initViews() {
        binding.backBtn.setOnClickListener(v -> finish());

        adapter = new NotificationAdapter(this, displayedNotifications, this::onNotificationClicked);
        binding.notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.notificationsRecyclerView.setAdapter(adapter);

        binding.swipeRefreshLayout.setColorSchemeColors(ThemeManager.getAccentColorInt());
        binding.swipeRefreshLayout.setOnRefreshListener(() -> fetchNotifications(true));

        binding.markAllReadBtn.setOnClickListener(v -> markAllRead());

        setupFilterChips();
    }

    private void setupFilterChips() {
        binding.filterAllBtn.setOnClickListener(v -> setFilter("All"));
        binding.filterAcademicBtn.setOnClickListener(v -> setFilter("Academic"));
        binding.filterGeneralBtn.setOnClickListener(v -> setFilter("General"));
        binding.filterFeesBtn.setOnClickListener(v -> setFilter("Fees"));
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterChipStyles();
        filterAndDisplay();
    }

    private void updateFilterChipStyles() {
        int activeBg = Color.parseColor("#4F46E5");
        int activeText = Color.WHITE;
        int inactiveBg = Color.WHITE;
        int inactiveText = Color.parseColor("#64748B");

        binding.filterAllBtn.setCardBackgroundColor("All".equals(currentFilter) ? activeBg : inactiveBg);
        binding.filterAllText.setTextColor("All".equals(currentFilter) ? activeText : inactiveText);

        binding.filterAcademicBtn.setCardBackgroundColor("Academic".equals(currentFilter) ? activeBg : inactiveBg);
        binding.filterAcademicText.setTextColor("Academic".equals(currentFilter) ? activeText : inactiveText);

        binding.filterGeneralBtn.setCardBackgroundColor("General".equals(currentFilter) ? activeBg : inactiveBg);
        binding.filterGeneralText.setTextColor("General".equals(currentFilter) ? activeText : inactiveText);

        binding.filterFeesBtn.setCardBackgroundColor("Fees".equals(currentFilter) ? activeBg : inactiveBg);
        binding.filterFeesText.setTextColor("Fees".equals(currentFilter) ? activeText : inactiveText);
    }

    private void loadCachedData() {
        List<NotificationsInboxResponse.NotificationItem> cached =
                NotificationStorage.getInstance(this).getNotificationsList();
        if (!cached.isEmpty()) {
            allNotifications.clear();
            allNotifications.addAll(cached);
            filterAndDisplay();
            updateUnreadBadge();
        }
    }

    private void fetchNotifications(boolean isPullToRefresh) {
        long now = System.currentTimeMillis();
        if (isPullToRefresh) {
            if (now - lastRefreshTime < 8000L) {
                binding.swipeRefreshLayout.setRefreshing(false);
                long waitSec = Math.max(1, (8000L - (now - lastRefreshTime)) / 1000L);
                ToastUtil.showToastShort("Please wait " + waitSec + "s before refreshing again.");
                return;
            }
            lastRefreshTime = now;
        }

        if (!binding.swipeRefreshLayout.isRefreshing() && allNotifications.isEmpty()) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }

        NetworkClient.getClient().getNotificationsInbox().enqueue(new Callback<NotificationsInboxResponse>() {
            @Override
            public void onResponse(Call<NotificationsInboxResponse> call, Response<NotificationsInboxResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    NotificationsInboxResponse.Data data = response.body().getData();
                    NotificationStorage.getInstance(NotificationsActivity.this).saveInbox(data);
                    allNotifications.clear();
                    allNotifications.addAll(data.getResults());
                    filterAndDisplay();
                    updateUnreadBadge();
                } else if (allNotifications.isEmpty()) {
                    loadFallbackMockNotifications();
                }
            }

            @Override
            public void onFailure(Call<NotificationsInboxResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);
                if (allNotifications.isEmpty()) {
                    loadFallbackMockNotifications();
                }
            }
        });
    }

    private void loadFallbackMockNotifications() {
        // Sample announcements from BBSBEC ERP
        allNotifications.clear();
        allNotifications.add(new NotificationsInboxResponse.NotificationItem(
                128,
                "MST-1 Schedule Announced",
                "Mid-semester test schedule for 7th semester has been uploaded. Check the academic calendar.",
                "Academic",
                false,
                "2026-10-10T14:20:00+05:30"
        ));
        allNotifications.add(new NotificationsInboxResponse.NotificationItem(
                127,
                "Semester Fee Reminder",
                "Last date for semester fee submission without late fee is approaching. Verify your fee ledger.",
                "Fees",
                false,
                "2026-10-08T11:00:00+05:30"
        ));
        allNotifications.add(new NotificationsInboxResponse.NotificationItem(
                126,
                "Campus Placement Drive: Infosys",
                "Registration for Infosys campus pool placement is now open for pre-final and final year students.",
                "General",
                true,
                "2026-10-05T09:30:00+05:30"
        ));

        NotificationsInboxResponse.Data mockData = new NotificationsInboxResponse.Data();
        mockData.getResults().addAll(allNotifications);
        NotificationStorage.getInstance(this).saveInbox(mockData);

        filterAndDisplay();
        updateUnreadBadge();
    }

    private void filterAndDisplay() {
        displayedNotifications.clear();
        for (NotificationsInboxResponse.NotificationItem item : allNotifications) {
            if ("All".equalsIgnoreCase(currentFilter)) {
                displayedNotifications.add(item);
            } else if (item.getCategory() != null && item.getCategory().equalsIgnoreCase(currentFilter)) {
                displayedNotifications.add(item);
            }
        }

        binding.emptyLayout.setVisibility(displayedNotifications.isEmpty() ? View.VISIBLE : View.GONE);
        binding.notificationsRecyclerView.setVisibility(displayedNotifications.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    private void updateUnreadBadge() {
        int unread = NotificationStorage.getInstance(this).getUnreadCount();
        if (unread > 0) {
            binding.unreadCountBadge.setVisibility(View.VISIBLE);
            binding.unreadCountBadge.setText(unread + " new");
        } else {
            binding.unreadCountBadge.setVisibility(View.GONE);
        }
    }

    private void onNotificationClicked(NotificationsInboxResponse.NotificationItem item) {
        if (!item.isRead()) {
            item.setRead(true);
            NotificationStorage.getInstance(this).markAsRead(item.getId());
            adapter.notifyDataSetChanged();
            updateUnreadBadge();

            // API Mark as read
            NetworkClient.getClient().markNotificationRead(new MarkNotificationReadRequest(item.getId()))
                    .enqueue(new Callback<NotificationActionResponse>() {
                        @Override
                        public void onResponse(Call<NotificationActionResponse> call, Response<NotificationActionResponse> response) {}
                        @Override
                        public void onFailure(Call<NotificationActionResponse> call, Throwable t) {}
                    });
        }

        // Show clean detail dialog
        new AlertDialog.Builder(this)
                .setTitle(item.getTitle())
                .setMessage("[" + item.getCategory() + "]\n\n" + item.getMessage())
                .setPositiveButton("Close", null)
                .show();
    }

    private void markAllRead() {
        NotificationStorage.getInstance(this).markAllAsRead();
        for (NotificationsInboxResponse.NotificationItem item : allNotifications) {
            item.setRead(true);
        }
        adapter.notifyDataSetChanged();
        updateUnreadBadge();
        ToastUtil.showToastShort("All notifications marked as read");

        NetworkClient.getClient().markAllNotificationsRead(new HashMap<>())
                .enqueue(new Callback<NotificationActionResponse>() {
                    @Override
                    public void onResponse(Call<NotificationActionResponse> call, Response<NotificationActionResponse> response) {}
                    @Override
                    public void onFailure(Call<NotificationActionResponse> call, Throwable t) {}
                });
    }
}
