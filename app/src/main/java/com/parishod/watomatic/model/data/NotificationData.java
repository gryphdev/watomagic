package com.parishod.watomagic.model.data;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

import androidx.annotation.Nullable;

import com.parishod.watomagic.model.utils.NotificationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight representation of the essential data extracted from a {@link StatusBarNotification}.
 * This model is used by the reply provider system to make decisions without relying directly on
 * framework classes.
 */
public class NotificationData {

    private final int id;
    private final String appPackage;
    @Nullable
    private final String title;
    @Nullable
    private final String body;
    private final long timestamp;
    private final boolean isGroupConversation;
    private final List<String> actions;

    public NotificationData(int id,
                            String appPackage,
                            @Nullable String title,
                            @Nullable String body,
                            long timestamp,
                            boolean isGroupConversation,
                            List<String> actions) {
        this.id = id;
        this.appPackage = appPackage;
        this.title = title;
        this.body = body;
        this.timestamp = timestamp;
        this.isGroupConversation = isGroupConversation;
        this.actions = actions != null ? Collections.unmodifiableList(new ArrayList<>(actions))
                : Collections.emptyList();
    }

    public int getId() {
        return id;
    }

    public String getAppPackage() {
        return appPackage;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getBody() {
        return body;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isGroupConversation() {
        return isGroupConversation;
    }

    public List<String> getActions() {
        return actions;
    }

    /**
     * Convenience factory method to convert a {@link StatusBarNotification} into {@link NotificationData}.
     */
    public static NotificationData from(StatusBarNotification sbn) {
        String derivedTitle = NotificationUtils.getTitle(sbn);
        CharSequence bodySequence = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
        String bodyText = bodySequence != null ? bodySequence.toString() : null;
        boolean isGroup = sbn.getNotification().extras.getBoolean("android.isGroupConversation");
        List<String> actionTitles = extractActionTitles(sbn.getNotification());

        return new NotificationData(
                sbn.getId(),
                sbn.getPackageName(),
                derivedTitle,
                bodyText,
                sbn.getPostTime(),
                isGroup,
                actionTitles
        );
    }

    private static List<String> extractActionTitles(Notification notification) {
        Notification.Action[] actions = notification.actions;
        if (actions == null || actions.length == 0) {
            return Collections.emptyList();
        }

        List<String> titles = new ArrayList<>(actions.length);
        for (Notification.Action action : actions) {
            CharSequence title = action.title;
            titles.add(title != null ? title.toString() : "");
        }
        return titles;
    }
}
