package com.parishod.watomagic.replyproviders.model;

import android.service.notification.StatusBarNotification;

import androidx.annotation.Nullable;

import com.parishod.watomagic.NotificationWear;

/**
 * Lightweight data holder that encapsulates the minimal information required by reply providers
 * without coupling them directly to {@link com.parishod.watomagic.service.NotificationService}.
 */
public class NotificationData {
    private final StatusBarNotification statusBarNotification;
    private final NotificationWear notificationWear;
    @Nullable
    private final String incomingMessage;
    private final String fallbackReply;

    public NotificationData(StatusBarNotification statusBarNotification,
                            NotificationWear notificationWear,
                            @Nullable String incomingMessage,
                            String fallbackReply) {
        this.statusBarNotification = statusBarNotification;
        this.notificationWear = notificationWear;
        this.incomingMessage = incomingMessage;
        this.fallbackReply = fallbackReply;
    }

    public StatusBarNotification getStatusBarNotification() {
        return statusBarNotification;
    }

    public NotificationWear getNotificationWear() {
        return notificationWear;
    }

    @Nullable
    public String getIncomingMessage() {
        return incomingMessage;
    }

    public String getFallbackReply() {
        return fallbackReply;
    }
}
