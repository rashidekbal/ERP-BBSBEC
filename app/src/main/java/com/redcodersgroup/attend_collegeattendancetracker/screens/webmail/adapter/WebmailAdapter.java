package com.redcodersgroup.attend_collegeattendancetracker.screens.webmail.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemWebmailMessageBinding;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.WebmailInboxResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class WebmailAdapter extends RecyclerView.Adapter<WebmailAdapter.ViewHolder> {

    public interface OnMailClickListener {
        void onMailClick(WebmailInboxResponse.MailItem item);
    }

    private final Context context;
    private final List<WebmailInboxResponse.MailItem> mailList;
    private final OnMailClickListener listener;

    private static final String[] AVATAR_COLORS = new String[]{
            "#3B82F6", "#8B5CF6", "#EC4899", "#10B981", "#F59E0B", "#06B6D4", "#6366F1"
    };

    public WebmailAdapter(Context context, List<WebmailInboxResponse.MailItem> mailList, OnMailClickListener listener) {
        this.context = context;
        this.mailList = mailList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWebmailMessageBinding binding = ItemWebmailMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(mailList.get(position));
    }

    @Override
    public int getItemCount() {
        return mailList != null ? mailList.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemWebmailMessageBinding binding;

        public ViewHolder(@NonNull ItemWebmailMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(WebmailInboxResponse.MailItem item) {
            String sender = item.getFromName();
            binding.senderName.setText(sender);
            binding.senderEmail.setText(item.getFromEmail());
            binding.subjectText.setText(item.getSubject());
            binding.previewText.setText(item.getPreview() != null ? item.getPreview().trim() : "");

            // Initial for avatar
            String initial = !sender.isEmpty() ? sender.substring(0, 1).toUpperCase(Locale.US) : "M";
            binding.avatarInitial.setText(initial);

            // Deterministic pastel avatar background
            int colorIdx = Math.abs(sender.hashCode()) % AVATAR_COLORS.length;
            try {
                binding.avatarCard.setCardBackgroundColor(Color.parseColor(AVATAR_COLORS[colorIdx]));
            } catch (Exception ignored) {
                binding.avatarCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.accent));
            }

            // Date format
            binding.dateText.setText(formatDate(item.getReceivedAt()));

            // Read / Unread status
            if (item.isRead()) {
                binding.unreadDot.setVisibility(View.GONE);
                binding.subjectText.setTypeface(null, Typeface.NORMAL);
                binding.senderName.setTypeface(null, Typeface.NORMAL);
            } else {
                binding.unreadDot.setVisibility(View.VISIBLE);
                binding.subjectText.setTypeface(null, Typeface.BOLD);
                binding.senderName.setTypeface(null, Typeface.BOLD);
            }

            // Attachments
            binding.attachmentContainer.setVisibility(item.isHasAttachments() ? View.VISIBLE : View.GONE);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMailClick(item);
                }
            });
        }
    }

    public static String formatDate(String isoDateStr) {
        if (isoDateStr == null || isoDateStr.isEmpty()) return "";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(isoDateStr);
            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            try {
                SimpleDateFormat inputFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                inputFormat2.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date2 = inputFormat2.parse(isoDateStr);
                if (date2 != null) {
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
                    return outputFormat.format(date2);
                }
            } catch (Exception ignored) {}
        }
        return isoDateStr.length() > 10 ? isoDateStr.substring(0, 10) : isoDateStr;
    }
}
