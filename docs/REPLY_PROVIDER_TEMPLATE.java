package com.parishod.watomagic.replyproviders;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.parishod.watomagic.replyproviders.model.NotificationData;

/**
 * Template para crear nuevos ReplyProviders
 * 
 * IMPORTANTE: Siempre incluir estos imports base:
 * - NotificationData (requerido por la interfaz ReplyProvider)
 * - Context (parámetro del método generateReply)
 * - Log (para debugging)
 */
public class YourNewReplyProvider implements ReplyProvider {
    private static final String TAG = "YourNewReplyProvider";

    @Override
    public void generateReply(@NonNull Context context,
                             @NonNull NotificationData notificationData,
                             @NonNull ReplyCallback callback) {
        // Tu implementación aquí
        try {
            // Procesar notificationData
            String reply = processNotification(notificationData);
            callback.onSuccess(reply);
        } catch (Exception e) {
            Log.e(TAG, "Error generating reply", e);
            callback.onFailure("Error: " + e.getMessage());
        }
    }

    private String processNotification(NotificationData data) {
        // Tu lógica aquí
        return data.getFallbackReply();
    }
}

