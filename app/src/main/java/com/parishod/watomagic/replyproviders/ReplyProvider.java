package com.parishod.watomagic.replyproviders;

import android.content.Context;

import androidx.annotation.NonNull;

import com.parishod.watomagic.replyproviders.model.NotificationData;

/**
 * Strategy interface responsible for producing the text that Watomagic will send back
 * as an automatic reply for a given notification.
 */
public interface ReplyProvider {

    void generateReply(@NonNull Context context,
                       @NonNull NotificationData notificationData,
                       @NonNull ReplyCallback callback);

    interface ReplyCallback {
        void onSuccess(@NonNull String reply);

        void onFailure(@NonNull String error);
    }
}
