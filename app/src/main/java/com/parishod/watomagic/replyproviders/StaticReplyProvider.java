package com.parishod.watomagic.replyproviders;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.parishod.watomagic.replyproviders.model.NotificationData;

/**
 * Default provider that mirrors the legacy behaviour: always reply with the configured static text.
 */
public class StaticReplyProvider implements ReplyProvider {

    private static final String TAG = "StaticReplyProvider";

    @Override
    public void generateReply(@NonNull Context context,
                              @NonNull NotificationData notificationData,
                              @NonNull ReplyCallback callback) {
        String reply = notificationData.getFallbackReply();
        if (TextUtils.isEmpty(reply)) {
            Log.w(TAG, "Fallback reply text is empty.");
            callback.onFailure("No static reply configured");
        } else {
            callback.onSuccess(reply);
        }
    }
}
