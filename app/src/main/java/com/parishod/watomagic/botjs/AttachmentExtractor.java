package com.parishod.watomagic.botjs;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.parishod.watomagic.replyproviders.model.AttachmentInfo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Extrae imágenes de notificaciones y las guarda como archivos temporales.
 */
public class AttachmentExtractor {
    private static final String TAG = "AttachmentExtractor";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final int THUMBNAIL_MAX_SIZE = 200; // 200x200px
    private static final String ATTACHMENTS_DIR = "bot_attachments";

    private final Context context;
    private final File attachmentsDir;

    public AttachmentExtractor(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.attachmentsDir = new File(context.getExternalFilesDir(null), ATTACHMENTS_DIR);
        if (!attachmentsDir.exists()) {
            attachmentsDir.mkdirs();
        }
    }

    /**
     * Extrae todas las imágenes disponibles de una notificación.
     */
    @NonNull
    public List<AttachmentInfo> extractAttachments(@NonNull StatusBarNotification sbn) {
        List<AttachmentInfo> attachments = new ArrayList<>();
        
        try {
            // 1. Intentar extraer desde Notification.EXTRA_PICTURE
            Bitmap picture = extractFromExtraPicture(sbn);
            if (picture != null) {
                AttachmentInfo info = saveBitmap(picture, "image/jpeg", AttachmentInfo.SourceType.BITMAP);
                if (info != null) {
                    attachments.add(info);
                }
            }

            // 2. Intentar extraer desde Notification.EXTRA_BIG_PICTURE
            Bitmap bigPicture = extractFromExtraBigPicture(sbn);
            if (bigPicture != null && !attachments.contains(bigPicture)) {
                AttachmentInfo info = saveBitmap(bigPicture, "image/jpeg", AttachmentInfo.SourceType.BITMAP);
                if (info != null) {
                    attachments.add(info);
                }
            }

            // 3. Intentar extraer desde largeIcon
            Bitmap largeIcon = extractFromLargeIcon(sbn);
            if (largeIcon != null) {
                AttachmentInfo info = saveBitmap(largeIcon, "image/png", AttachmentInfo.SourceType.BITMAP);
                if (info != null) {
                    attachments.add(info);
                }
            }

            // 4. Intentar extraer desde MessagingStyle messages
            List<AttachmentInfo> messagingAttachments = extractFromMessagingStyle(sbn);
            attachments.addAll(messagingAttachments);

        } catch (Exception e) {
            Log.e(TAG, "Error extracting attachments", e);
        }

        return attachments;
    }

    @Nullable
    private Bitmap extractFromExtraPicture(@NonNull StatusBarNotification sbn) {
        try {
            Object picture = sbn.getNotification().extras.get(NotificationCompat.EXTRA_PICTURE);
            if (picture instanceof Bitmap) {
                return (Bitmap) picture;
            }
        } catch (Exception e) {
            Log.d(TAG, "No EXTRA_PICTURE found", e);
        }
        return null;
    }

    @Nullable
    private Bitmap extractFromExtraBigPicture(@NonNull StatusBarNotification sbn) {
        try {
            Object bigPicture = sbn.getNotification().extras.get(NotificationCompat.EXTRA_BIG_PICTURE);
            if (bigPicture instanceof Bitmap) {
                return (Bitmap) bigPicture;
            }
        } catch (Exception e) {
            Log.d(TAG, "No EXTRA_BIG_PICTURE found", e);
        }
        return null;
    }

    @Nullable
    private Bitmap extractFromLargeIcon(@NonNull StatusBarNotification sbn) {
        try {
            android.app.Notification notification = sbn.getNotification();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.graphics.drawable.Icon largeIcon = notification.getLargeIcon();
                if (largeIcon != null) {
                    // Try to get bitmap from icon
                    if (largeIcon.getType() == android.graphics.drawable.Icon.TYPE_BITMAP) {
                        return largeIcon.getBitmap();
                    } else {
                        // For other types, try loading as drawable and converting
                        android.graphics.drawable.Drawable drawable = largeIcon.loadDrawable(context);
                        if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
                            return ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "No largeIcon found or error loading", e);
        }
        return null;
    }

    @NonNull
    private List<AttachmentInfo> extractFromMessagingStyle(@NonNull StatusBarNotification sbn) {
        List<AttachmentInfo> attachments = new ArrayList<>();
        try {
            NotificationCompat.MessagingStyle messagingStyle = 
                NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(sbn.getNotification());
            
            if (messagingStyle != null) {
                for (NotificationCompat.MessagingStyle.Message message : messagingStyle.getMessages()) {
                    Uri dataUri = message.getDataUri();
                    if (dataUri != null && isImageUri(dataUri)) {
                        AttachmentInfo info = saveUri(dataUri);
                        if (info != null) {
                            attachments.add(info);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "No MessagingStyle or error extracting", e);
        }
        return attachments;
    }

    private boolean isImageUri(@NonNull Uri uri) {
        String scheme = uri.getScheme();
        if ("content".equals(scheme) || "file".equals(scheme)) {
            String mimeType = context.getContentResolver().getType(uri);
            return mimeType != null && mimeType.startsWith("image/");
        }
        return false;
    }

    @Nullable
    private AttachmentInfo saveBitmap(@NonNull Bitmap bitmap, @NonNull String mimeType, 
                                      @NonNull AttachmentInfo.SourceType sourceType) {
        String id = UUID.randomUUID().toString();
        File outputFile = new File(attachmentsDir, id + getExtensionFromMimeType(mimeType));
        
        try {
            // Guardar bitmap
            FileOutputStream fos = new FileOutputStream(outputFile);
            Bitmap.CompressFormat format = getCompressFormat(mimeType);
            bitmap.compress(format, 85, fos);
            fos.close();

            long fileSize = outputFile.length();
            if (fileSize > MAX_FILE_SIZE) {
                outputFile.delete();
                Log.w(TAG, "Image too large: " + fileSize);
                return null;
            }

            // Generar thumbnail
            String thumbnailBase64 = generateThumbnail(bitmap);

            return new AttachmentInfo(id, mimeType, fileSize, sourceType, 
                                    outputFile.getAbsolutePath(), thumbnailBase64);
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap", e);
            if (outputFile.exists()) {
                outputFile.delete();
            }
            return null;
        }
    }

    @Nullable
    private AttachmentInfo saveUri(@NonNull Uri uri) {
        String id = UUID.randomUUID().toString();
        ContentResolver resolver = context.getContentResolver();
        
        try {
            String mimeType = resolver.getType(uri);
            if (mimeType == null || !mimeType.startsWith("image/")) {
                return null;
            }

            File outputFile = new File(attachmentsDir, id + getExtensionFromMimeType(mimeType));
            
            // Copiar desde URI a archivo
            try (InputStream inputStream = resolver.openInputStream(uri);
                 FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                
                if (inputStream == null) {
                    return null;
                }

                byte[] buffer = new byte[8192];
                long totalSize = 0;
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    if (totalSize + bytesRead > MAX_FILE_SIZE) {
                        outputFile.delete();
                        Log.w(TAG, "Image from URI too large");
                        return null;
                    }
                    outputStream.write(buffer, 0, bytesRead);
                    totalSize += bytesRead;
                }
            }

            long fileSize = outputFile.length();
            
            // Generar thumbnail desde el archivo
            Bitmap bitmap = BitmapFactory.decodeFile(outputFile.getAbsolutePath());
            String thumbnailBase64 = bitmap != null ? generateThumbnail(bitmap) : null;
            if (bitmap != null) {
                bitmap.recycle();
            }

            return new AttachmentInfo(id, mimeType, fileSize, AttachmentInfo.SourceType.URI,
                                    outputFile.getAbsolutePath(), thumbnailBase64);
        } catch (IOException e) {
            Log.e(TAG, "Error saving URI", e);
            return null;
        }
    }

    @Nullable
    private String generateThumbnail(@NonNull Bitmap original) {
        try {
            int width = original.getWidth();
            int height = original.getHeight();
            
            if (width <= THUMBNAIL_MAX_SIZE && height <= THUMBNAIL_MAX_SIZE) {
                // Ya es pequeño, usar directamente
                return bitmapToBase64(original);
            }

            // Calcular nuevo tamaño manteniendo aspect ratio
            float scale = Math.min((float) THUMBNAIL_MAX_SIZE / width, 
                                 (float) THUMBNAIL_MAX_SIZE / height);
            int newWidth = Math.round(width * scale);
            int newHeight = Math.round(height * scale);

            Bitmap thumbnail = Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
            String base64 = bitmapToBase64(thumbnail);
            thumbnail.recycle();
            return base64;
        } catch (Exception e) {
            Log.e(TAG, "Error generating thumbnail", e);
            return null;
        }
    }

    @NonNull
    private String bitmapToBase64(@NonNull Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }

    @NonNull
    private String getExtensionFromMimeType(@NonNull String mimeType) {
        if (mimeType.contains("jpeg") || mimeType.contains("jpg")) {
            return ".jpg";
        } else if (mimeType.contains("png")) {
            return ".png";
        } else if (mimeType.contains("gif")) {
            return ".gif";
        } else if (mimeType.contains("webp")) {
            return ".webp";
        }
        return ".jpg"; // Default
    }

    @NonNull
    private Bitmap.CompressFormat getCompressFormat(@NonNull String mimeType) {
        if (mimeType.contains("png")) {
            return Bitmap.CompressFormat.PNG;
        } else if (mimeType.contains("webp")) {
            return Bitmap.CompressFormat.WEBP;
        }
        return Bitmap.CompressFormat.JPEG; // Default
    }

    /**
     * Obtiene la ruta de un archivo adjunto por su ID.
     */
    @Nullable
    public String getAttachmentPath(@NonNull String attachmentId) {
        File[] files = attachmentsDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith(attachmentId)) {
                    return file.getAbsolutePath();
                }
            }
        }
        return null;
    }

    /**
     * Elimina un archivo adjunto por su ID.
     */
    public boolean deleteAttachment(@NonNull String attachmentId) {
        File[] files = attachmentsDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith(attachmentId)) {
                    return file.delete();
                }
            }
        }
        return false;
    }
}

