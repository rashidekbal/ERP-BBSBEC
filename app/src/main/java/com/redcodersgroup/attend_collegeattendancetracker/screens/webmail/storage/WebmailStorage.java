package com.redcodersgroup.attend_collegeattendancetracker.screens.webmail.storage;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.WebmailInboxResponse;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Local file-based storage and memory cache for campus webmail messages.
 * Provides offline viewing, persistence, and instant mailbox access.
 */
public class WebmailStorage {

    private static final String TAG = "WebmailStorage";
    private static final String FILE_NAME = "webmail_inbox_cache.json";
    private static final String PREF_NAME = "webmail_storage_prefs";
    private static final String KEY_LAST_SYNC = "last_sync_timestamp";

    private static WebmailStorage instance;
    private final Context appContext;
    private final Gson gson;
    private WebmailInboxResponse.WebmailData cachedData;
    private final Map<String, WebmailInboxResponse.MailItem> mailIndex = new HashMap<>();

    private WebmailStorage(Context context) {
        this.appContext = context.getApplicationContext();
        this.gson = new Gson();
        loadFromDisk();
    }

    public static synchronized WebmailStorage getInstance(Context context) {
        if (instance == null) {
            instance = new WebmailStorage(context);
        }
        return instance;
    }

    /**
     * Persists new or refreshed inbox data to disk and updates in-memory cache.
     */
    public synchronized void saveInbox(WebmailInboxResponse.WebmailData data) {
        if (data == null) return;
        this.cachedData = data;
        mailIndex.clear();
        if (data.getResults() != null) {
            for (WebmailInboxResponse.MailItem item : data.getResults()) {
                if (item != null && item.getId() != null) {
                    mailIndex.put(item.getId(), item);
                }
            }
        }
        writeToDisk(data);
        setLastSyncTime(System.currentTimeMillis());
    }

    /**
     * Returns cached inbox data, or null if no cache is present.
     */
    public synchronized WebmailInboxResponse.WebmailData getCachedInbox() {
        if (cachedData == null) {
            loadFromDisk();
        }
        return cachedData;
    }

    /**
     * Checks if local cached mails exist.
     */
    public synchronized boolean hasCachedData() {
        if (cachedData != null && cachedData.getResults() != null && !cachedData.getResults().isEmpty()) {
            return true;
        }
        File file = new File(appContext.getFilesDir(), FILE_NAME);
        return file.exists() && file.length() > 0;
    }

    /**
     * Retrieves a single cached mail item by ID.
     */
    public synchronized WebmailInboxResponse.MailItem getMail(String id) {
        if (id == null) return null;
        if (mailIndex.containsKey(id)) {
            return mailIndex.get(id);
        }
        if (cachedData != null && cachedData.getResults() != null) {
            for (WebmailInboxResponse.MailItem item : cachedData.getResults()) {
                if (id.equals(item.getId())) {
                    mailIndex.put(id, item);
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * Marks a mail as read in local storage.
     */
    public synchronized void markAsRead(String id) {
        if (id == null) return;
        boolean modified = false;

        if (mailIndex.containsKey(id)) {
            WebmailInboxResponse.MailItem item = mailIndex.get(id);
            if (item != null && !item.isRead()) {
                item.setRead(true);
                modified = true;
            }
        }

        if (cachedData != null && cachedData.getResults() != null) {
            for (WebmailInboxResponse.MailItem item : cachedData.getResults()) {
                if (id.equals(item.getId()) && !item.isRead()) {
                    item.setRead(true);
                    modified = true;
                }
            }
        }

        if (modified && cachedData != null) {
            writeToDisk(cachedData);
        }
    }

    /**
     * Updates or caches a single mail item (e.g. if detail view loaded extra content).
     */
    public synchronized void saveMail(WebmailInboxResponse.MailItem item) {
        if (item == null || item.getId() == null) return;
        mailIndex.put(item.getId(), item);

        if (cachedData == null) {
            cachedData = new WebmailInboxResponse.WebmailData();
            cachedData.setResults(new ArrayList<>());
        }

        List<WebmailInboxResponse.MailItem> list = cachedData.getResults();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (item.getId().equals(list.get(i).getId())) {
                list.set(i, item);
                found = true;
                break;
            }
        }
        if (!found) {
            list.add(0, item);
        }
        writeToDisk(cachedData);
    }

    public synchronized long getLastSyncTime() {
        return appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_SYNC, 0L);
    }

    public synchronized void setLastSyncTime(long time) {
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_SYNC, time)
                .apply();
    }

    public synchronized void clearCache() {
        cachedData = null;
        mailIndex.clear();
        File file = new File(appContext.getFilesDir(), FILE_NAME);
        if (file.exists()) {
            file.delete();
        }
    }

    private void writeToDisk(WebmailInboxResponse.WebmailData data) {
        new Thread(() -> {
            synchronized (WebmailStorage.this) {
                File file = new File(appContext.getFilesDir(), FILE_NAME);
                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(data, writer);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to write webmail cache to disk", e);
                }
            }
        }).start();
    }

    private void loadFromDisk() {
        File file = new File(appContext.getFilesDir(), FILE_NAME);
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<WebmailInboxResponse.WebmailData>() {}.getType();
            cachedData = gson.fromJson(reader, type);
            mailIndex.clear();
            if (cachedData != null && cachedData.getResults() != null) {
                for (WebmailInboxResponse.MailItem item : cachedData.getResults()) {
                    if (item != null && item.getId() != null) {
                        mailIndex.put(item.getId(), item);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load webmail cache from disk", e);
        }
    }
}
