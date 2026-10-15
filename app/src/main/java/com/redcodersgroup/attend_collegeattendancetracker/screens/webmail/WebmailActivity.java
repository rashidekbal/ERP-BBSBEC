package com.redcodersgroup.attend_collegeattendancetracker.screens.webmail;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivityWebmailBinding;
import com.redcodersgroup.attend_collegeattendancetracker.network.NetworkClient;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.WebmailInboxResponse;
import com.redcodersgroup.attend_collegeattendancetracker.screens.webmail.adapter.WebmailAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.webmail.storage.WebmailStorage;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WebmailActivity extends AppCompatActivity {

    private ActivityWebmailBinding binding;
    private WebmailAdapter adapter;
    private final List<WebmailInboxResponse.MailItem> mailList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWebmailBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        applyTheme();
        loadFromLocalStorage();
        loadMailbox(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh items from local storage to reflect read status changes
        WebmailInboxResponse.WebmailData cached = WebmailStorage.getInstance(this).getCachedInbox();
        if (cached != null && cached.getResults() != null && !cached.getResults().isEmpty()) {
            renderMailbox(cached, true);
        }
    }

    private void initViews() {
        binding.backBtn.setOnClickListener(v -> finish());
        binding.refreshBtn.setOnClickListener(v -> loadMailbox(true));

        adapter = new WebmailAdapter(this, mailList, this::openMailDetail);
        binding.mailRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.mailRecyclerView.setAdapter(adapter);

        binding.swipeRefreshLayout.setOnRefreshListener(() -> loadMailbox(true));
    }

    private void applyTheme() {
        int accentColor = ThemeManager.getAccentColorInt();
        binding.mailHeaderIcon.setImageTintList(ColorStateList.valueOf(accentColor));
        binding.swipeRefreshLayout.setColorSchemeColors(accentColor);
    }

    private void loadFromLocalStorage() {
        WebmailInboxResponse.WebmailData cached = WebmailStorage.getInstance(this).getCachedInbox();
        if (cached != null && cached.getResults() != null && !cached.getResults().isEmpty()) {
            renderMailbox(cached, true);
        }
    }

    private void openMailDetail(WebmailInboxResponse.MailItem item) {
        if (item == null) return;
        Intent intent = new Intent(this, WebmailDetailActivity.class);
        intent.putExtra(WebmailDetailActivity.EXTRA_MAIL_ITEM, item);
        intent.putExtra(WebmailDetailActivity.EXTRA_MAIL_ID, item.getId());
        startActivity(intent);
    }

    private long lastMailboxRefreshTime = 0L;

    private void loadMailbox(boolean isSwipeRefresh) {
        long now = System.currentTimeMillis();
        if (isSwipeRefresh) {
            if (now - lastMailboxRefreshTime < 8000L) {
                binding.swipeRefreshLayout.setRefreshing(false);
                long waitSec = Math.max(1, (8000L - (now - lastMailboxRefreshTime)) / 1000L);
                ToastUtil.showToastShort("Please wait " + waitSec + "s before refreshing again.");
                return;
            }
            lastMailboxRefreshTime = now;
        }

        if (!isSwipeRefresh && mailList.isEmpty()) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }

        NetworkClient.getClient().getWebmailInbox(0, 50).enqueue(new Callback<WebmailInboxResponse>() {
            @Override
            public void onResponse(Call<WebmailInboxResponse> call, Response<WebmailInboxResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    WebmailInboxResponse.WebmailData data = response.body().getData();
                    // Save to local storage
                    WebmailStorage.getInstance(WebmailActivity.this).saveInbox(data);
                    renderMailbox(data, false);
                } else {
                    if (mailList.isEmpty()) {
                        handleFailure("Unable to load inbox (" + response.code() + ")");
                    } else {
                        binding.syncStatusPill.setText("Cached");
                        binding.syncStatusPill.setTextColor(ContextCompat.getColor(WebmailActivity.this, R.color.text_dim));
                        ToastUtil.showToastShort("Showing offline cached inbox");
                    }
                }
            }

            @Override
            public void onFailure(Call<WebmailInboxResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);

                if (mailList.isEmpty()) {
                    handleFailure("Failed to connect to BBSBEC Webmail");
                } else {
                    binding.syncStatusPill.setText("Offline");
                    binding.syncStatusPill.setTextColor(ContextCompat.getColor(WebmailActivity.this, R.color.danger));
                    ToastUtil.showToastShort("Offline: Displaying locally saved mails");
                }
            }
        });
    }

    private void renderMailbox(WebmailInboxResponse.WebmailData data, boolean isCached) {
        String email = data.getEmailAddress();
        if (email != null && !email.isEmpty()) {
            binding.emailAddressText.setText(email);
        } else {
            binding.emailAddressText.setText("BBSBEC Student Webmail");
        }

        mailList.clear();
        List<WebmailInboxResponse.MailItem> results = data.getResults();
        if (results != null) {
            mailList.addAll(results);
        }

        int totalCount = data.getTotal() > 0 ? data.getTotal() : mailList.size();
        int unreadCount = 0;
        for (WebmailInboxResponse.MailItem item : mailList) {
            if (!item.isRead()) {
                unreadCount++;
            }
        }

        binding.mailCountSummaryText.setText(String.format(Locale.getDefault(),
                "%d %s · %d unread",
                totalCount,
                totalCount == 1 ? "message" : "messages",
                unreadCount));

        adapter.notifyDataSetChanged();

        if (mailList.isEmpty()) {
            binding.emptyStateContainer.setVisibility(View.VISIBLE);
            binding.mailRecyclerView.setVisibility(View.GONE);
            binding.syncStatusPill.setText("Empty");
            binding.syncStatusPill.setTextColor(ContextCompat.getColor(this, R.color.text_dim));
        } else {
            binding.emptyStateContainer.setVisibility(View.GONE);
            binding.mailRecyclerView.setVisibility(View.VISIBLE);
            if (isCached) {
                binding.syncStatusPill.setText("Cached");
                binding.syncStatusPill.setTextColor(ContextCompat.getColor(this, R.color.text_dim));
            } else {
                binding.syncStatusPill.setText("Connected");
                binding.syncStatusPill.setTextColor(ContextCompat.getColor(this, R.color.success));
            }
        }
    }

    private void handleFailure(String message) {
        ToastUtil.showToastShort(message);
        if (mailList.isEmpty()) {
            binding.emptyStateContainer.setVisibility(View.VISIBLE);
            binding.emptyStateTitle.setText("Inbox Unavailable");
            binding.emptyStateSubtitle.setText(message + ". Pull down to retry.");
            binding.mailRecyclerView.setVisibility(View.GONE);
            binding.syncStatusPill.setText("Offline");
            binding.syncStatusPill.setTextColor(ContextCompat.getColor(this, R.color.danger));
        }
    }
}
