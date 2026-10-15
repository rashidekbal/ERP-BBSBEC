package com.redcodersgroup.attend_collegeattendancetracker.screens.webmail;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivityWebmailDetailBinding;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.WebmailInboxResponse;
import com.redcodersgroup.attend_collegeattendancetracker.screens.webmail.storage.WebmailStorage;
import com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebmailDetailActivity extends AppCompatActivity {

    public static final String EXTRA_MAIL_ITEM = "mail_item";
    public static final String EXTRA_MAIL_ID = "mail_id";

    private ActivityWebmailDetailBinding binding;
    private WebmailInboxResponse.MailItem mailItem;

    private static final String[] AVATAR_COLORS = new String[]{
            "#3B82F6", "#8B5CF6", "#EC4899", "#10B981", "#F59E0B", "#06B6D4", "#6366F1"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWebmailDetailBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        resolveMailItem();
        initViews();
        renderMailDetails();
    }

    private void resolveMailItem() {
        if (getIntent() != null) {
            Object itemObj = getIntent().getSerializableExtra(EXTRA_MAIL_ITEM);
            if (itemObj instanceof WebmailInboxResponse.MailItem) {
                mailItem = (WebmailInboxResponse.MailItem) itemObj;
            } else {
                String mailId = getIntent().getStringExtra(EXTRA_MAIL_ID);
                if (mailId != null) {
                    mailItem = WebmailStorage.getInstance(this).getMail(mailId);
                }
            }
        }

        if (mailItem != null && mailItem.getId() != null) {
            // Mark read in local storage
            WebmailStorage.getInstance(this).markAsRead(mailItem.getId());
        }
    }

    private void initViews() {
        binding.backBtn.setOnClickListener(v -> finish());

        int accentColor = ThemeManager.getAccentColorInt();
        binding.detailProgressBar.setIndeterminateTintList(ColorStateList.valueOf(accentColor));

        binding.shareBtn.setOnClickListener(v -> {
            if (mailItem != null) {
                copyMailToClipboard();
            }
        });

        setupWebView();
    }

    private void setupWebView() {
        WebView webView = binding.detailWebView;
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setDefaultTextEncodingName("UTF-8");

        webView.setBackgroundColor(Color.TRANSPARENT);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    openExternalUri(request.getUrl());
                    return true;
                }
                return false;
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null) {
                    openExternalUri(Uri.parse(url));
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                binding.detailProgressBar.setVisibility(View.GONE);
            }
        });
    }

    private void openExternalUri(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (Exception e) {
            ToastUtil.showToastShort("Cannot open link: " + uri.toString());
        }
    }

    private void renderMailDetails() {
        if (mailItem == null) {
            ToastUtil.showToastShort("Email message not found");
            finish();
            return;
        }

        String senderName = mailItem.getFromName();
        String senderEmail = mailItem.getFromEmail();
        String subject = mailItem.getSubject();
        String date = formatDate(mailItem.getReceivedAt());

        binding.detailSubject.setText(subject);
        binding.detailSenderName.setText(senderName);
        binding.detailSenderEmail.setText(senderEmail.isEmpty() ? "(No sender address)" : senderEmail);
        binding.detailDate.setText(date);

        // Avatar styling
        String initial = getInitial(senderName, senderEmail);
        binding.detailAvatarInitial.setText(initial);
        int color = getAvatarColor(senderName.isEmpty() ? senderEmail : senderName);
        binding.detailAvatarCard.setCardBackgroundColor(color);

        // Attachment badge
        if (mailItem.isHasAttachments()) {
            binding.detailAttachmentBadge.setVisibility(View.VISIBLE);
        } else {
            binding.detailAttachmentBadge.setVisibility(View.GONE);
        }

        // Render mail body
        String rawContent = mailItem.getBodyContent();
        loadMailBody(rawContent);
    }

    private void loadMailBody(String content) {
        if (content == null || content.trim().isEmpty()) {
            content = "<i>(No message content)</i>";
        }

        boolean isDark = isDarkThemeActive();
        String textColorHex = isDark ? "#F2F3F5" : "#17181B";
        String linkColorHex = "#3B82F6";
        String codeBgHex = isDark ? "#22262F" : "#F0EFEC";

        boolean hasHtmlTags = isHtmlContent(content);
        String bodyHtml;

        if (hasHtmlTags) {
            bodyHtml = content;
        } else {
            // Convert plain text to readable HTML with hyperlinks and line breaks
            bodyHtml = formatPlainTextToHtml(content);
        }

        String styledHtml = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes\">\n" +
                "  <style>\n" +
                "    * { box-sizing: border-box; }\n" +
                "    body {\n" +
                "      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;\n" +
                "      font-size: 15px;\n" +
                "      line-height: 1.6;\n" +
                "      color: " + textColorHex + ";\n" +
                "      background-color: transparent;\n" +
                "      margin: 16px;\n" +
                "      padding: 0;\n" +
                "      word-wrap: break-word;\n" +
                "      overflow-wrap: break-word;\n" +
                "    }\n" +
                "    img {\n" +
                "      max-width: 100% !important;\n" +
                "      height: auto !important;\n" +
                "      border-radius: 8px;\n" +
                "    }\n" +
                "    table {\n" +
                "      max-width: 100% !important;\n" +
                "      border-collapse: collapse;\n" +
                "    }\n" +
                "    a {\n" +
                "      color: " + linkColorHex + ";\n" +
                "      text-decoration: underline;\n" +
                "    }\n" +
                "    pre, code {\n" +
                "      font-family: monospace;\n" +
                "      background-color: " + codeBgHex + ";\n" +
                "      padding: 3px 6px;\n" +
                "      border-radius: 4px;\n" +
                "      white-space: pre-wrap;\n" +
                "      word-break: break-all;\n" +
                "    }\n" +
                "    blockquote {\n" +
                "      margin: 12px 0;\n" +
                "      padding-left: 12px;\n" +
                "      border-left: 3px solid " + codeBgHex + ";\n" +
                "      color: #888888;\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                bodyHtml + "\n" +
                "</body>\n" +
                "</html>";

        binding.detailWebView.loadDataWithBaseURL("https://api.bbsbec.edu.in/", styledHtml, "text/html", "UTF-8", null);
    }

    private boolean isHtmlContent(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("<html") || lower.contains("<body") || lower.contains("<p")
                || lower.contains("<div") || lower.contains("<br") || lower.contains("<table")
                || lower.contains("<!doctype") || lower.contains("<span");
    }

    private String formatPlainTextToHtml(String plainText) {
        if (plainText == null) return "";
        // 1. Escape HTML special chars
        String escaped = plainText
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");

        // 2. Auto-link URLs
        Pattern urlPattern = Pattern.compile("(https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\.&]+)");
        Matcher matcher = urlPattern.matcher(escaped);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group(1);
            matcher.appendReplacement(sb, "<a href=\"" + url + "\">" + url + "</a>");
        }
        matcher.appendTail(sb);

        // 3. Convert newlines to <br/>
        return sb.toString().replace("\n", "<br/>");
    }

    private boolean isDarkThemeActive() {
        if (SharedPrefUtil.isDarkModeEnabled()) {
            return true;
        }
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private void copyMailToClipboard() {
        if (mailItem == null) return;
        String textToCopy = "Subject: " + mailItem.getSubject() + "\n" +
                "From: " + mailItem.getFromName() + " <" + mailItem.getFromEmail() + ">\n" +
                "Date: " + formatDate(mailItem.getReceivedAt()) + "\n\n" +
                mailItem.getBodyContent();

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Campus Mail", textToCopy);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            ToastUtil.showToastShort("Message copied to clipboard");
        }
    }

    private String getInitial(String name, String email) {
        if (name != null && !name.trim().isEmpty()) {
            return String.valueOf(name.trim().charAt(0)).toUpperCase(Locale.ROOT);
        }
        if (email != null && !email.trim().isEmpty()) {
            return String.valueOf(email.trim().charAt(0)).toUpperCase(Locale.ROOT);
        }
        return "M";
    }

    private int getAvatarColor(String key) {
        if (key == null || key.isEmpty()) return Color.parseColor(AVATAR_COLORS[0]);
        int hash = Math.abs(key.hashCode());
        int index = hash % AVATAR_COLORS.length;
        return Color.parseColor(AVATAR_COLORS[index]);
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = parser.parse(isoDate);
            if (date != null) {
                SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMM d, yyyy · h:mm a", Locale.getDefault());
                return formatter.format(date);
            }
        } catch (Exception ignored) {
            try {
                SimpleDateFormat parser2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date date = parser2.parse(isoDate);
                if (date != null) {
                    SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMM d, yyyy · h:mm a", Locale.getDefault());
                    return formatter.format(date);
                }
            } catch (Exception ignored2) {}
        }
        return isoDate;
    }
}
