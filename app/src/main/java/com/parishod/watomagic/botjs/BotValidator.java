package com.parishod.watomagic.botjs;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Comprueba reglas básicas antes de ejecutar un bot descargado.
 */
public final class BotValidator {

    private static final String TAG = "BotValidator";
    private static final int MAX_BOT_SIZE_BYTES = 102_400; // 100 KB
    private static final Pattern[] BLACKLISTED_PATTERNS = new Pattern[]{
            Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Function\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("constructor\\s*\\[", Pattern.CASE_INSENSITIVE),
            Pattern.compile("__proto__", Pattern.CASE_INSENSITIVE),
            Pattern.compile("import\\s*\\(", Pattern.CASE_INSENSITIVE)
    };

    private BotValidator() {
        // Utility class
    }

    public static boolean validate(String jsCode) {
        if (jsCode == null) {
            Log.w(TAG, "Bot code is null");
            return false;
        }

        if (jsCode.getBytes(StandardCharsets.UTF_8).length > MAX_BOT_SIZE_BYTES) {
            Log.w(TAG, "Bot too large");
            return false;
        }

        for (Pattern pattern : BLACKLISTED_PATTERNS) {
            if (pattern.matcher(jsCode).find()) {
                Log.w(TAG, "Dangerous pattern detected: " + pattern.pattern());
                return false;
            }
        }

        if (!jsCode.contains("processNotification")) {
            Log.w(TAG, "Missing processNotification function");
            return false;
        }

        return true;
    }
}
