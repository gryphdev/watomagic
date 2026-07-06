package com.parishod.watomagic.botjs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Detecta placeholders de media de WhatsApp (ej. "📷 Foto") sin confundirlos
 * con mensajes de texto que contengan el emoji en otra posición.
 */
public final class MediaPlaceholderDetector {

    private static final String CAMERA_EMOJI = "\uD83D\uDCF7";
    private static final Pattern WHATSAPP_MEDIA_PLACEHOLDER = Pattern.compile(
            "^" + CAMERA_EMOJI + "\\s*(Foto|Photo|Image|Imagen|Video|GIF|Sticker)?$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Set<String> WHATSAPP_PACKAGES = new HashSet<>(Arrays.asList(
            "com.whatsapp",
            "com.whatsapp.w4b"
    ));

    private MediaPlaceholderDetector() {
    }

    public static boolean isMediaPlaceholder(@NonNull String appPackage,
                                             @Nullable String body,
                                             boolean hasAttachments) {
        if (hasAttachments || body == null || body.isEmpty()) {
            return false;
        }
        if (!WHATSAPP_PACKAGES.contains(appPackage)) {
            return false;
        }
        return WHATSAPP_MEDIA_PLACEHOLDER.matcher(extractMediaText(body)).matches();
    }

    @VisibleForTesting
    @NonNull
    static String extractMediaText(@NonNull String body) {
        int lastColon = body.lastIndexOf(": ");
        if (lastColon >= 0) {
            return body.substring(lastColon + 2).trim();
        }
        return body.trim();
    }
}
