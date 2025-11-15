package com.parishod.watomagic.replyproviders;

import android.content.Context;

import androidx.annotation.NonNull;

import com.parishod.watomagic.model.CustomRepliesData;
import com.parishod.watomagic.model.data.NotificationData;

/**
 * Provides the default static reply behaviour matching the legacy implementation.
 */
public class StaticReplyProvider implements ReplyProvider {

    @Override
    public void generateReply(@NonNull Context context,
                              String incomingMessage,
                              @NonNull NotificationData notificationData,
                              @NonNull ReplyCallback callback) {
        String reply = CustomRepliesData.getInstance(context).getTextToSendOrElse();
        callback.onSuccess(reply);
    }
}
