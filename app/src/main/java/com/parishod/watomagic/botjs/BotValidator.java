package com.parishod.watomagic.botjs;

import android.util.Log;

import java.nio.charset.StandardCharsets;

/**
 * Valida reglas básicas antes de ejecutar un bot.
 * 
 * Nota: Rhino ya proporciona seguridad mediante:
 * - Timeout de ejecución (5 segundos)
 * - Límites de stack depth
 * - Sandbox sin acceso a filesystem
 * - Solo APIs expuestas explícitamente (Android.*)
 * 
 * Por lo tanto, este validador solo comprueba lo esencial:
 * - Tamaño del código (100 KB máximo)
 * - Presencia de la función processNotification
 */
public final class BotValidator {

    private static final String TAG = "BotValidator";
    private static final int MAX_BOT_SIZE_BYTES = 102_400; // 100 KB

    private BotValidator() {
        // Utility class
    }

    /**
     * Valida el código del bot.
     * 
     * @param jsCode Código JavaScript del bot
     * @return true si el código es válido, false en caso contrario
     */
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

        // Validar tamaño
        int botSize = jsCode.getBytes(StandardCharsets.UTF_8).length;
        if (botSize > MAX_BOT_SIZE_BYTES) {
            String message = String.format("Bot too large: %d bytes (max: %d bytes)", botSize, MAX_BOT_SIZE_BYTES);
            Log.w(TAG, message);
            if (BotLogCapture.isEnabled()) {
                BotLogCapture.addLog("error", message);
            }
            return false;
        }

        // Validar que existe la función processNotification
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
