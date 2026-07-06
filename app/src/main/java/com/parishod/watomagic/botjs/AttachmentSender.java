package com.parishod.watomagic.botjs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.RemoteInput;
import androidx.core.content.FileProvider;

import com.parishod.watomagic.NotificationWear;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maneja el envío de archivos adjuntos (imágenes) a través de RemoteInput.
 */
public class AttachmentSender {
    private static final String TAG = "AttachmentSender";
    private static final long MAX_TOTAL_SIZE = 10 * 1024 * 1024; // 10 MB

    private final Context context;
    private final String fileProviderAuthority;

    public AttachmentSender(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.fileProviderAuthority = context.getPackageName() + ".fileprovider";
    }

    /**
     * Añade attachments a un Intent para enviar a través de RemoteInput.
     *
     * @return true si se añadió al menos un adjunto, false si la app destino no lo soporta
     */
    public boolean addAttachmentsToIntent(@NonNull Intent intent,
                                          @NonNull NotificationWear notificationWear,
                                          @NonNull List<AttachmentToSend> attachments) {
        if (attachments.isEmpty()) {
            return true;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.w(TAG, "Image attachments require API 26+ RemoteInput data types");
            return false;
        }

        long totalSize = 0;
        for (AttachmentToSend att : attachments) {
            File file = resolveAttachmentFile(att.path);
            if (file == null || !file.exists()) {
                Log.w(TAG, "Attachment file not found: " + att.path);
                return false;
            }
            totalSize += file.length();
        }

        if (totalSize > MAX_TOTAL_SIZE) {
            Log.w(TAG, "Total attachment size exceeds limit: " + totalSize);
            return false;
        }

        try {
            List<RemoteInput> remoteInputs = notificationWear.getRemoteInputs();
            if (remoteInputs.isEmpty()) {
                return false;
            }

            AttachmentToSend firstAtt = attachments.get(0);
            File file = resolveAttachmentFile(firstAtt.path);
            if (file == null || !file.exists()) {
                return false;
            }

            Uri fileUri = FileProvider.getUriForFile(context, fileProviderAuthority, file);

            for (RemoteInput remoteInput : remoteInputs) {
                Set<String> allowedTypes = remoteInput.getAllowedDataTypes();
                if (allowedTypes == null || allowedTypes.isEmpty()) {
                    continue;
                }

                String matchedMime = matchMimeType(allowedTypes, firstAtt.mimeType);
                if (matchedMime == null) {
                    continue;
                }

                Map<String, Uri> dataResults = new HashMap<>();
                dataResults.put(matchedMime, fileUri);
                RemoteInput.addDataResultToIntent(remoteInput, intent, dataResults);
                RemoteInput.setResultsSource(intent, RemoteInput.SOURCE_CHOICE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Log.d(TAG, "Added attachment via RemoteInput (" + matchedMime + "): " + fileUri);
                return true;
            }

            Log.w(TAG, "No RemoteInput accepts image attachments for this notification");
        } catch (Exception e) {
            Log.e(TAG, "Error adding attachments to intent", e);
        }

        return false;
    }

    /**
     * Encuentra un MIME type permitido por el RemoteInput destino que coincida con el adjunto.
     */
    @VisibleForTesting
    @Nullable
    static String matchMimeType(@NonNull Set<String> allowedTypes, @NonNull String attachmentMimeType) {
        if (allowedTypes.contains(attachmentMimeType)) {
            return attachmentMimeType;
        }

        for (String allowed : allowedTypes) {
            if (allowed.endsWith("/*")) {
                String prefix = allowed.substring(0, allowed.length() - 1);
                if (attachmentMimeType.startsWith(prefix)) {
                    return allowed;
                }
            }
        }

        if (attachmentMimeType.startsWith("image/")) {
            for (String allowed : allowedTypes) {
                if (allowed.startsWith("image/")) {
                    return allowed;
                }
            }
        }

        return null;
    }

    @Nullable
    private File resolveAttachmentFile(@NonNull String path) {
        File file = new File(path);
        if (file.isAbsolute() && file.exists()) {
            File attachmentsDir = new File(context.getExternalFilesDir(null), "bot_attachments");
            if (path.startsWith(attachmentsDir.getAbsolutePath())) {
                return file;
            }
        }

        File attachmentsDir = new File(context.getExternalFilesDir(null), "bot_attachments");
        File resolvedFile = new File(attachmentsDir, path);
        if (resolvedFile.exists()) {
            return resolvedFile;
        }

        return null;
    }

    /**
     * Representa un attachment que el bot quiere enviar.
     */
    public static class AttachmentToSend {
        public final String path;
        public final String mimeType;

        public AttachmentToSend(String path, String mimeType) {
            this.path = path;
            this.mimeType = mimeType;
        }
    }
}
