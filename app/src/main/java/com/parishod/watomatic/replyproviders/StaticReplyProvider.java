package com.parishod.watomagic.replyproviders;

import android.content.Context;
import android.util.Log;

import com.parishod.watomagic.model.CustomRepliesData;

/**
 * Provider de respuestas estáticas (comportamiento original de Watomatic)
 * Retorna el mensaje personalizado configurado por el usuario
 */
public class StaticReplyProvider implements ReplyProvider {
    private static final String TAG = "StaticReplyProvider";

    @Override
    public void generateReply(Context context,
                             String incomingMessage,
                             NotificationData notificationData,
                             ReplyCallback callback) {
        CustomRepliesData customRepliesData = CustomRepliesData.getInstance(context);
        String replyText = customRepliesData.getTextToSendOrElse();
        
        Log.d(TAG, "Using static reply: " + replyText);
        callback.onSuccess(replyText);
    }
}
