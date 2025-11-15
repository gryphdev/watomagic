package com.parishod.watomagic.replyproviders;

import java.util.List;

/**
 * Datos de la notificación entrante que se pasan a los proveedores de respuestas
 */
public class NotificationData {
    private final int id;
    private final String appPackage;
    private final String title;
    private final String body;
    private final long timestamp;
    private final boolean isGroup;
    private final List<String> actions;

    public NotificationData(int id, String appPackage, String title, String body,
                           long timestamp, boolean isGroup, List<String> actions) {
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

    public List<String> getActions() {
        return actions;
    }
}
