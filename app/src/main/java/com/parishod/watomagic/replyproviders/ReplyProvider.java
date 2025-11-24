package com.parishod.watomagic.replyproviders;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parishod.watomagic.replyproviders.model.NotificationData;

import java.util.List;

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
        
        void onSuccess(@NonNull String reply, @Nullable List<com.parishod.watomagic.botjs.AttachmentSender.AttachmentToSend> attachments);

        void onFailure(@NonNull String error);
    }
}
