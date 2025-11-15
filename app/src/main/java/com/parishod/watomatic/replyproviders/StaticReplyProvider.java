package com.parishod.watomagic.replyproviders;

import android.content.Context;
import android.util.Log;

import com.parishod.watomagic.model.CustomRepliesData;
import com.parishod.watomagic.model.NotificationData;

/**
 * Provider que genera respuestas estáticas usando las respuestas personalizadas del usuario.
 * Este es el comportamiento por defecto de Watomatic.
 */
public class StaticReplyProvider implements ReplyProvider {
    private static final String TAG = "StaticReplyProvider";

    @Override
    public void generateReply(Context context,
                             String incomingMessage,
                             NotificationData notificationData,
                             ReplyCallback callback) {
        try {
            CustomRepliesData customRepliesData = CustomRepliesData.getInstance(context);
            String replyText = customRepliesData.getTextToSendOrElse();
            
            if (replyText != null && !replyText.trim().isEmpty()) {
                Log.d(TAG, "Generated static reply: " + replyText);
                callback.onSuccess(replyText);
            } else {
                Log.w(TAG, "No static reply available");
                callback.onFailure("No reply configured");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating static reply", e);
            callback.onFailure("Error: " + e.getMessage());
        }
    }
}
