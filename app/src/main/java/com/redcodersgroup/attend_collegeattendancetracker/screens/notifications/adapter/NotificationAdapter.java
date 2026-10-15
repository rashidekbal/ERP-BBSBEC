package com.redcodersgroup.attend_collegeattendancetracker.screens.notifications.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemNotificationCardBinding;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.NotificationsInboxResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationsInboxResponse.NotificationItem item);
    }

    private final Context context;
    private final List<NotificationsInboxResponse.NotificationItem> items;
    private final OnNotificationClickListener listener;
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

    public NotificationAdapter(Context context, List<NotificationsInboxResponse.NotificationItem> items, OnNotificationClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationCardBinding binding = ItemNotificationCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationsInboxResponse.NotificationItem item = items.get(position);
        holder.bind(item, displayFormat, listener);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNotificationCardBinding binding;

        public ViewHolder(@NonNull ItemNotificationCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(NotificationsInboxResponse.NotificationItem item,
                         SimpleDateFormat displayFormat,
                         OnNotificationClickListener listener) {
            if (item == null) return;

            binding.titleText.setText(item.getTitle());
            binding.messageSnippetText.setText(item.getMessage());
            binding.categoryBadge.setText(item.getCategory());

            // Unread dot
            binding.unreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);

            // Category styling
            String cat = item.getCategory().toLowerCase();
            if (cat.contains("academic")) {
                binding.categoryBadge.setBackgroundColor(Color.parseColor("#EEF2FF"));
                binding.categoryBadge.setTextColor(Color.parseColor("#4F46E5"));
            } else if (cat.contains("fee")) {
                binding.categoryBadge.setBackgroundColor(Color.parseColor("#FEF3C7"));
                binding.categoryBadge.setTextColor(Color.parseColor("#B45309"));
            } else if (cat.contains("exam") || cat.contains("mst")) {
                binding.categoryBadge.setBackgroundColor(Color.parseColor("#FCE7F3"));
                binding.categoryBadge.setTextColor(Color.parseColor("#BE185D"));
            } else {
                binding.categoryBadge.setBackgroundColor(Color.parseColor("#F1F5F9"));
                binding.categoryBadge.setTextColor(Color.parseColor("#475569"));
            }

            // Timestamp parsing
            binding.timestampText.setText(formatDate(item.getCreatedAt(), displayFormat));

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(item);
                }
            });
        }

        private String formatDate(String isoString, SimpleDateFormat displayFormat) {
            if (isoString == null || isoString.isEmpty()) return "";
            try {
                // Try ISO 8601 parsing
                SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                parser.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = parser.parse(isoString);
                if (date != null) {
                    return displayFormat.format(date);
                }
            } catch (Exception e) {
                try {
                    // Fallback for simple date
                    SimpleDateFormat fallback = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    Date date = fallback.parse(isoString);
                    if (date != null) {
                        return displayFormat.format(date);
                    }
                } catch (Exception ignored) {}
            }
            return isoString;
        }
    }
}
