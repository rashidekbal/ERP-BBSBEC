package com.redcodersgroup.attend_collegeattendancetracker.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.network.BbsbecApiService;
import com.redcodersgroup.attend_collegeattendancetracker.network.NetworkClient;
import com.redcodersgroup.attend_collegeattendancetracker.network.TokenManager;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.NotificationsInboxResponse;
import com.redcodersgroup.attend_collegeattendancetracker.screens.notifications.storage.NotificationStorage;
import com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil;

import java.util.List;
import java.util.Set;

import retrofit2.Response;

/**
 * Background receiver invoked periodically to check for unread ERP campus notifications,
 * even when the app is closed or terminated.
 */
public class NotificationCheckReceiver extends BroadcastReceiver {

    private static final String TAG = "NotifCheckReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Re-arm for subsequent periodic checks
        NotificationScheduler.scheduleNotificationCheck(context);

        if (!TokenManager.isLoggedIn()) {
            return;
        }

        DbHelper.execute(() -> {
            try {
                BbsbecApiService api = NetworkClient.getApiService();
                Response<NotificationsInboxResponse> response = api.getNotificationsInbox().execute();
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    NotificationsInboxResponse.Data data = response.body().getData();
                    NotificationStorage.getInstance(context).saveInbox(data);

                    List<NotificationsInboxResponse.NotificationItem> items = data.getResults();
                    if (items != null) {
                        Set<String> notifiedIds = SharedPrefUtil.getNotifiedAnnouncementIds();
                        for (NotificationsInboxResponse.NotificationItem item : items) {
                            if (item == null) continue;
                            String idStr = String.valueOf(item.getId());
                            if (!item.isRead() && !notifiedIds.contains(idStr)) {
                                NotificationHelper.showCampusNotification(
                                        context,
                                        NotificationHelper.NOTIFICATION_ID_CAMPUS_BASE + (item.getId() % 10000),
                                        item.getTitle() != null && !item.getTitle().isEmpty() ? item.getTitle() : "Campus Notification",
                                        item.getMessage() != null ? item.getMessage() : ""
                                );
                                SharedPrefUtil.addNotifiedAnnouncementId(item.getId());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Background notification check error: " + e.getMessage());
            }
        });
    }
}
