package com.parishod.watomagic.replyproviders;

import android.content.Context;

import androidx.annotation.NonNull;

import com.parishod.watomagic.model.data.NotificationData;

/**
 * Defines the contract for any component that can generate a reply to an incoming notification.
 */
public interface ReplyProvider {

    /**
     * Generates a reply for the provided notification data.
     *
     * @param context           Android context for accessing system resources.
     * @param incomingMessage   The textual content extracted from the notification, if any.
     * @param notificationData  Structured data representation of the notification.
     * @param callback          Callback to deliver success or failure results.
     */
    void generateReply(@NonNull Context context,
                       String incomingMessage,
                       @NonNull NotificationData notificationData,
                       @NonNull ReplyCallback callback);

    interface ReplyCallback {
        void onSuccess(String reply);

        void onFailure(String error);
    }
}
