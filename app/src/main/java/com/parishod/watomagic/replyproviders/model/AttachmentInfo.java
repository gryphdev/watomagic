package com.parishod.watomagic.replyproviders.model;

import androidx.annotation.Nullable;

import java.util.UUID;

/**
 * Información sobre un archivo adjunto (imagen) extraído de una notificación.
 */
public class AttachmentInfo {
    public enum SourceType {
        BITMAP,  // Extraído desde Notification.EXTRA_PICTURE o largeIcon
        URI,     // Extraído desde MessagingStyle.Message.getDataUri()
        FILE     // Archivo temporal creado por el bot
    }

    private final String id;
    private final String mimeType;
    private final long size;
    private final SourceType sourceType;
    @Nullable
    private final String temporaryPath;
    @Nullable
    private final String thumbnailBase64;

    public AttachmentInfo(String id, String mimeType, long size, SourceType sourceType,
                          @Nullable String temporaryPath, @Nullable String thumbnailBase64) {
        this.id = id;
        this.mimeType = mimeType;
        this.size = size;
        this.sourceType = sourceType;
        this.temporaryPath = temporaryPath;
        this.thumbnailBase64 = thumbnailBase64;
    }

    public String getId() {
        return id;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSize() {
        return size;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    @Nullable
    public String getTemporaryPath() {
        return temporaryPath;
    }

    @Nullable
    public String getThumbnailBase64() {
        return thumbnailBase64;
    }

    public boolean hasFile() {
        return temporaryPath != null;
    }
}

