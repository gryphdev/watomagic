package com.parishod.watomagic.model;

import android.service.notification.StatusBarNotification;
import com.parishod.watomagic.model.utils.NotificationUtils;

/**
 * Modelo de datos que encapsula toda la información de una notificación
 * para ser pasada a los ReplyProviders.
 */
public class NotificationData {
    private final int id;
    private final String appPackage;
    private final String title;
    private final String body;
    private final long timestamp;
    private final boolean isGroup;
    private final String[] actions;

    public NotificationData(StatusBarNotification sbn) {
        this.id = sbn.getId();
        this.appPackage = sbn.getPackageName();
        this.title = NotificationUtils.getTitle(sbn);
        this.body = sbn.getNotification().extras.getString("android.text");
        this.timestamp = sbn.getPostTime();
        this.isGroup = sbn.getNotification().extras.getBoolean("android.isGroupConversation", false);
        // Actions could be extracted from notification if needed
        this.actions = new String[0];
    }

    public NotificationData(int id, String appPackage, String title, String body, 
                            long timestamp, boolean isGroup, String[] actions) {
        this.id = id;
        this.appPackage = appPackage;
        this.title = title;
        this.body = body;
        this.timestamp = timestamp;
        this.isGroup = isGroup;
        this.actions = actions;
    }

    public int getId() {
        return id;
    }

    public String getAppPackage() {
        return appPackage;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public String[] getActions() {
        return actions;
    }
}
