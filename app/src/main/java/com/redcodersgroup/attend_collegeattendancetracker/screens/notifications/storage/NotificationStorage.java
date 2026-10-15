package com.redcodersgroup.attend_collegeattendancetracker.screens.notifications.storage;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.NotificationsInboxResponse;

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
 * Local cache and offline storage for notifications.
 */
public class NotificationStorage {

    private static final String TAG = "NotificationStorage";
    private static final String FILE_NAME = "notifications_cache.json";

    private static NotificationStorage instance;
    private final Context appContext;
    private final Gson gson;
    private NotificationsInboxResponse.Data cachedData;
    private final Map<Integer, NotificationsInboxResponse.NotificationItem> notificationIndex = new HashMap<>();

    private NotificationStorage(Context context) {
        this.appContext = context.getApplicationContext();
        this.gson = new Gson();
        loadFromDisk();
    }

    public static synchronized NotificationStorage getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationStorage(context);
        }
        return instance;
    }

    public synchronized void saveInbox(NotificationsInboxResponse.Data data) {
        if (data == null) return;
        this.cachedData = data;
        notificationIndex.clear();
        if (data.getResults() != null) {
            for (NotificationsInboxResponse.NotificationItem item : data.getResults()) {
                if (item != null) {
                    notificationIndex.put(item.getId(), item);
                }
            }
        }
        persistToDisk();
    }

    public synchronized NotificationsInboxResponse.Data getCachedData() {
        return cachedData;
    }

    public synchronized List<NotificationsInboxResponse.NotificationItem> getNotificationsList() {
        if (cachedData != null && cachedData.getResults() != null) {
            return new ArrayList<>(cachedData.getResults());
        }
        return new ArrayList<>();
    }

    public synchronized int getUnreadCount() {
        if (cachedData == null || cachedData.getResults() == null) return 0;
        int count = 0;
        for (NotificationsInboxResponse.NotificationItem item : cachedData.getResults()) {
            if (item != null && !item.isRead()) {
                count++;
            }
        }
        return count;
    }

    public synchronized void markAsRead(int notificationId) {
        NotificationsInboxResponse.NotificationItem item = notificationIndex.get(notificationId);
        if (item != null && !item.isRead()) {
            item.setRead(true);
            persistToDisk();
        }
    }

    public synchronized void markAllAsRead() {
        if (cachedData == null || cachedData.getResults() == null) return;
        for (NotificationsInboxResponse.NotificationItem item : cachedData.getResults()) {
            if (item != null) {
                item.setRead(true);
            }
        }
        persistToDisk();
    }

    private void persistToDisk() {
        if (cachedData == null) return;
        File file = new File(appContext.getFilesDir(), FILE_NAME);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(cachedData, writer);
        } catch (IOException e) {
            Log.e(TAG, "Failed to persist notifications cache", e);
        }
    }

    private void loadFromDisk() {
        File file = new File(appContext.getFilesDir(), FILE_NAME);
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<NotificationsInboxResponse.Data>() {}.getType();
            cachedData = gson.fromJson(reader, type);
            notificationIndex.clear();
            if (cachedData != null && cachedData.getResults() != null) {
                for (NotificationsInboxResponse.NotificationItem item : cachedData.getResults()) {
                    if (item != null) {
                        notificationIndex.put(item.getId(), item);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load notifications cache", e);
        }
    }
}
