package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WebmailInboxResponse implements Serializable {

    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private WebmailData data;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public WebmailData getData() {
        return data;
    }

    public void setData(WebmailData data) {
        this.data = data;
    }

    public static class WebmailData implements Serializable {
        @SerializedName("email_address")
        private String emailAddress;

        @SerializedName("total")
        private int total;

        @SerializedName("position")
        private int position;

        @SerializedName("limit")
        private int limit;

        @SerializedName("results")
        private List<MailItem> results = new ArrayList<>();

        public String getEmailAddress() {
            return emailAddress != null ? emailAddress : "";
        }

        public void setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public List<MailItem> getResults() {
            return results != null ? results : new ArrayList<>();
        }

        public void setResults(List<MailItem> results) {
            this.results = results;
        }
    }

    public static class MailItem implements Serializable {
        @SerializedName("id")
        private String id;

        @SerializedName("thread_id")
        private String threadId;

        @SerializedName("from_name")
        private String fromName;

        @SerializedName("from_email")
        private String fromEmail;

        @SerializedName("subject")
        private String subject;

        @SerializedName("preview")
        private String preview;

        @SerializedName("received_at")
        private String receivedAt;

        @SerializedName("is_read")
        private boolean isRead;

        @SerializedName("has_attachments")
        private boolean hasAttachments;

        @SerializedName("body")
        private String body;

        @SerializedName("html")
        private String html;

        @SerializedName("content")
        private String content;

        @SerializedName("text")
        private String text;

        public String getId() {
            return id != null ? id : "";
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getThreadId() {
            return threadId != null ? threadId : "";
        }

        public void setThreadId(String threadId) {
            this.threadId = threadId;
        }

        public String getFromName() {
            return fromName != null && !fromName.trim().isEmpty() ? fromName.trim() : (fromEmail != null ? fromEmail : "Sender");
        }

        public void setFromName(String fromName) {
            this.fromName = fromName;
        }

        public String getFromEmail() {
            return fromEmail != null ? fromEmail : "";
        }

        public void setFromEmail(String fromEmail) {
            this.fromEmail = fromEmail;
        }

        public String getSubject() {
            return subject != null && !subject.trim().isEmpty() ? subject.trim() : "(No subject)";
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getPreview() {
            return preview != null ? preview : "";
        }

        public void setPreview(String preview) {
            this.preview = preview;
        }

        public String getReceivedAt() {
            return receivedAt != null ? receivedAt : "";
        }

        public void setReceivedAt(String receivedAt) {
            this.receivedAt = receivedAt;
        }

        public boolean isRead() {
            return isRead;
        }

        public void setRead(boolean read) {
            isRead = read;
        }

        public boolean isHasAttachments() {
            return hasAttachments;
        }

        public void setHasAttachments(boolean hasAttachments) {
            this.hasAttachments = hasAttachments;
        }

        public String getBody() {
            return body != null ? body : "";
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getHtml() {
            return html != null ? html : "";
        }

        public void setHtml(String html) {
            this.html = html;
        }

        public String getContent() {
            return content != null ? content : "";
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getText() {
            return text != null ? text : "";
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getBodyContent() {
            if (html != null && !html.trim().isEmpty()) {
                return html;
            }
            if (body != null && !body.trim().isEmpty()) {
                return body;
            }
            if (content != null && !content.trim().isEmpty()) {
                return content;
            }
            if (text != null && !text.trim().isEmpty()) {
                return text;
            }
            return preview != null ? preview : "";
        }
    }
}
