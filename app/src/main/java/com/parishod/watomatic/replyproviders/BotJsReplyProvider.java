package com.parishod.watomagic.replyproviders;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.parishod.watomagic.botjs.BotAndroidAPI;
import com.parishod.watomagic.botjs.BotJsEngine;
import com.parishod.watomagic.botjs.BotResponse;
import com.parishod.watomagic.botjs.BotValidator;
import com.parishod.watomagic.botjs.RateLimiter;
import com.parishod.watomagic.model.NotificationData;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Provider que genera respuestas usando un bot JavaScript personalizado.
 * Ejecuta el bot en un sandbox seguro usando QuickJS.
 */
public class BotJsReplyProvider implements ReplyProvider {
    private static final String TAG = "BotJsReplyProvider";
    private static final int MAX_EXECUTIONS_PER_MINUTE = 100;
    private static final long RATE_LIMIT_WINDOW_MS = 60 * 1000; // 1 minuto

    private static final RateLimiter rateLimiter = new RateLimiter(MAX_EXECUTIONS_PER_MINUTE, RATE_LIMIT_WINDOW_MS);
    private final Gson gson = new Gson();

    @Override
    public void generateReply(Context context,
                             String incomingMessage,
                             NotificationData notificationData,
                             ReplyCallback callback) {
        // Verificar rate limiting
        if (!rateLimiter.tryAcquire()) {
            Log.w(TAG, "Rate limit exceeded. Falling back to static reply.");
            callback.onFailure("Rate limit exceeded");
            return;
        }

        // Ejecutar en thread background
        new Thread(() -> {
            BotJsEngine engine = null;
            try {
                // Cargar bot.js
                String jsCode = loadBotCode(context);
                if (jsCode == null) {
                    Log.e(TAG, "Bot code not found");
                    callback.onFailure("Bot code not found");
                    return;
                }

                // Validar código
                if (!BotValidator.validate(jsCode)) {
                    Log.e(TAG, "Bot code validation failed");
                    callback.onFailure("Bot validation failed");
                    return;
                }

                // Inicializar motor
                engine = new BotJsEngine(context);
                engine.initialize();

                // Convertir NotificationData a JSON
                String notificationJson = convertToJson(notificationData);

                // Ejecutar bot
                String responseJson = engine.executeBot(jsCode, notificationJson);

                // Parsear respuesta
                BotResponse response = parseResponse(responseJson);
                if (response == null || !response.isValid()) {
                    Log.e(TAG, "Invalid bot response: " + responseJson);
                    callback.onFailure("Invalid bot response");
                    return;
                }

                // Manejar acción
                handleBotResponse(response, callback);

            } catch (ExecutionException e) {
                Log.e(TAG, "Bot execution failed", e);
                callback.onFailure("Execution error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error in bot execution", e);
                callback.onFailure("Error: " + e.getMessage());
            } finally {
                if (engine != null) {
                    engine.cleanup();
                }
            }
        }).start();
    }

    /**
     * Carga el código del bot desde el almacenamiento interno
     */
    private String loadBotCode(Context context) {
        try {
            File botsDir = new File(context.getFilesDir(), "bots");
            File botFile = new File(botsDir, "active-bot.js");
            
            if (!botFile.exists()) {
                Log.w(TAG, "Bot file not found: " + botFile.getAbsolutePath());
                return null;
            }

            StringBuilder code = new StringBuilder();
            try (FileReader reader = new FileReader(botFile)) {
                char[] buffer = new char[8192];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    code.append(buffer, 0, read);
                }
            }

            return code.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error loading bot code", e);
            return null;
        }
    }

    /**
     * Convierte NotificationData a JSON
     */
    private String convertToJson(NotificationData notificationData) {
        // Crear objeto JSON manualmente para evitar problemas con Gson y tipos primitivos
        return String.format(
            "{" +
            "\"id\": %d," +
            "\"appPackage\": \"%s\"," +
            "\"title\": %s," +
            "\"body\": %s," +
            "\"timestamp\": %d," +
            "\"isGroup\": %s," +
            "\"actions\": []" +
            "}",
            notificationData.getId(),
            escapeJson(notificationData.getAppPackage()),
            escapeJson(notificationData.getTitle()),
            escapeJson(notificationData.getBody()),
            notificationData.getTimestamp(),
            notificationData.isGroup()
        );
    }

    /**
     * Escapa un string para JSON
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "null";
        }
        return "\"" + str.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t") + "\"";
    }

    /**
     * Parsea la respuesta JSON del bot
     */
    private BotResponse parseResponse(String responseJson) {
        try {
            return gson.fromJson(responseJson, BotResponse.class);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing bot response: " + responseJson, e);
            return null;
        }
    }

    /**
     * Maneja la respuesta del bot según el tipo de acción
     */
    private void handleBotResponse(BotResponse response, ReplyCallback callback) {
        String action = response.getAction().toUpperCase();

        switch (action) {
            case "REPLY":
                callback.onSuccess(response.getReplyText());
                break;
            case "DISMISS":
                callback.onFailure("DISMISS");
                break;
            case "KEEP":
                callback.onFailure("KEEP");
                break;
            case "SNOOZE":
                // Por ahora, tratar SNOOZE como KEEP
                // TODO: Implementar lógica de snooze en Fase futura
                Log.d(TAG, "Bot requested SNOOZE for " + response.getSnoozeMinutes() + " minutes");
                callback.onFailure("SNOOZE");
                break;
            default:
                Log.w(TAG, "Unknown bot action: " + action);
                callback.onFailure("Unknown action: " + action);
        }
    }
}
