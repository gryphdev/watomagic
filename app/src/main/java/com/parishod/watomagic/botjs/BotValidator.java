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
            String message = "Bot code is null";
            Log.w(TAG, message);
            if (BotLogCapture.isEnabled()) {
                BotLogCapture.addLog("error", message);
            }
            return false;
        }

        if (jsCode.trim().isEmpty()) {
            String message = "Bot code is empty";
            Log.w(TAG, message);
            if (BotLogCapture.isEnabled()) {
                BotLogCapture.addLog("error", message);
            }
            return false;
        }

        int botSize = jsCode.getBytes(StandardCharsets.UTF_8).length;
        if (botSize > MAX_BOT_SIZE_BYTES) {
            String message = String.format("Bot too large: %d bytes (max: %d bytes)", botSize, MAX_BOT_SIZE_BYTES);
            Log.w(TAG, message);
            if (BotLogCapture.isEnabled()) {
                BotLogCapture.addLog("error", message);
            }
            return false;
        }

        for (Pattern pattern : BLACKLISTED_PATTERNS) {
            java.util.regex.Matcher matcher = pattern.matcher(jsCode);
            if (matcher.find()) {
                int position = matcher.start();
                String message = String.format("Dangerous pattern detected: '%s' at position %d", pattern.pattern(), position);
                Log.w(TAG, message);
                if (BotLogCapture.isEnabled()) {
                    BotLogCapture.addLog("error", message);
                }
                return false;
            }
        }

        if (!jsCode.contains("processNotification")) {
            String message = "Missing processNotification function. Bot code must contain a function named 'processNotification'";
            Log.w(TAG, message);
            if (BotLogCapture.isEnabled()) {
                BotLogCapture.addLog("error", message);
            }
            return false;
        }

        return true;
    }
}
