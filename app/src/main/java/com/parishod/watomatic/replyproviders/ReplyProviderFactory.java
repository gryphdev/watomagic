package com.parishod.watomagic.replyproviders;

import androidx.annotation.NonNull;

import com.parishod.watomagic.model.preferences.PreferencesManager;

/**
 * Simple factory encapsulating the strategy selection for reply generation.
 * Current priority: OpenAI > Static replies. Future providers (BotJS, etc.) will be injected here.
 */
public final class ReplyProviderFactory {

    private ReplyProviderFactory() {
        // Utility class
    }

    @NonNull
    public static ReplyProvider getProvider(@NonNull PreferencesManager preferencesManager) {
        if (preferencesManager.isOpenAIRepliesEnabled()) {
            return new OpenAIReplyProvider();
        }
        return new StaticReplyProvider();
    }
}
