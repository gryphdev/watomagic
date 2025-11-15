package com.parishod.watomagic.replyproviders;

import androidx.annotation.NonNull;

import com.parishod.watomagic.model.preferences.PreferencesManager;

/**
 * Central place to decide which reply strategy should be used.
 */
public final class ReplyProviderFactory {

    private ReplyProviderFactory() {
        // no-op
    }

    @NonNull
    public static ReplyProvider getProvider(@NonNull PreferencesManager preferencesManager) {
        if (preferencesManager.isOpenAIRepliesEnabled()) {
            return new OpenAIReplyProvider(preferencesManager);
        }
        return new StaticReplyProvider();
    }
}
