package com.parishod.watomagic.botjs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.RemoteInput;
import androidx.core.content.FileProvider;

import com.parishod.watomagic.NotificationWear;

import java.io.File;
import java.util.List;

/**
 * Maneja el envío de archivos adjuntos (imágenes) a través de RemoteInput.
 */
public class AttachmentSender {
    private static final String TAG = "AttachmentSender";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
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
     * @param intent El Intent que se enviará
     * @param notificationWear La notificación con RemoteInputs
     * @param attachments Lista de attachments a enviar
     * @return true si se añadieron attachments exitosamente, false si no se soporta o hay error
     */
    public boolean addAttachmentsToIntent(@NonNull Intent intent,
                                          @NonNull NotificationWear notificationWear,
                                          @NonNull List<AttachmentToSend> attachments) {
        if (attachments.isEmpty()) {
            return true; // No hay attachments, no hay problema
        }

        // Validar tamaño total
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

        // Intentar añadir attachments usando RemoteInput con tipos de datos
        // Nota: No todas las apps soportan esto, así que puede fallar silenciosamente
        try {
            List<RemoteInput> remoteInputs = notificationWear.getRemoteInputs();
            if (remoteInputs.isEmpty()) {
                return false;
            }

            // Para cada RemoteInput, intentar añadir el primer attachment
            // (la mayoría de apps solo soportan un attachment por RemoteInput)
            RemoteInput firstInput = remoteInputs.get(0);
            
            // Verificar si el RemoteInput soporta tipos de datos
            // Esto requiere Android 7.0+ (API 24+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                // Intentar añadir el primer attachment
                if (!attachments.isEmpty()) {
                    AttachmentToSend firstAtt = attachments.get(0);
                    File file = resolveAttachmentFile(firstAtt.path);
                    if (file != null && file.exists()) {
                        Uri fileUri = FileProvider.getUriForFile(context, fileProviderAuthority, file);
                        
                        // Añadir URI al intent con FLAG_GRANT_READ_URI_PERMISSION
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        intent.setType(firstAtt.mimeType);
                        
                        Log.d(TAG, "Added attachment to intent: " + fileUri);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding attachments to intent", e);
            return false;
        }

        return false;
    }

    @Nullable
    private File resolveAttachmentFile(@NonNull String path) {
        // Si es una ruta absoluta dentro del directorio de la app, usarla directamente
        File file = new File(path);
        if (file.isAbsolute() && file.exists()) {
            // Verificar que está dentro del directorio permitido
            File attachmentsDir = new File(context.getExternalFilesDir(null), "bot_attachments");
            if (path.startsWith(attachmentsDir.getAbsolutePath())) {
                return file;
            }
        }

        // Si es relativa, asumir que está en bot_attachments/
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

