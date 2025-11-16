package com.parishod.watomagic.botjs;

import android.util.Log;

import java.util.regex.Pattern;

/**
 * Validador de código JavaScript para bots
 * Verifica seguridad y requisitos básicos
 */
public class BotValidator {
    private static final String TAG = "BotValidator";
    private static final int MAX_BOT_SIZE_BYTES = 102400; // 100KB

    private static final String[] BLACKLISTED_PATTERNS = {
        "eval\\s*\\(",
        "Function\\s*\\(",
        "constructor\\s*\\[",
        "__proto__",
        "import\\s*\\("
    };

    /**
     * Valida el código JavaScript del bot
     * @param jsCode Código JavaScript a validar
     * @return true si el código es válido, false en caso contrario
     */
    public static boolean validate(String jsCode) {
        if (jsCode == null || jsCode.trim().isEmpty()) {
            Log.w(TAG, "Bot code is null or empty");
            return false;
        }

        // Verificar tamaño
        if (jsCode.length() > MAX_BOT_SIZE_BYTES) {
            Log.w(TAG, "Bot too large: " + jsCode.length() + " bytes (max: " + MAX_BOT_SIZE_BYTES + ")");
            return false;
        }

        // Verificar patrones peligrosos
        for (String patternStr : BLACKLISTED_PATTERNS) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(jsCode).find()) {
                Log.w(TAG, "Dangerous pattern detected: " + patternStr);
                return false;
            }
        }

        // Verificar que define processNotification
        if (!jsCode.contains("processNotification")) {
            Log.w(TAG, "Missing processNotification function");
            return false;
        }

        return true;
    }
}
