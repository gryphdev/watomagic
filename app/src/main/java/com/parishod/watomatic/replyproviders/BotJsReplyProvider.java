package com.parishod.watomagic.replyproviders;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.parishod.watomagic.botjs.BotJsEngine;
import com.parishod.watomagic.botjs.BotValidator;
import com.parishod.watomagic.botjs.RateLimiter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Provider de respuestas usando bots JavaScript personalizados
 * Ejecuta bots JavaScript localmente usando QuickJS
 */
public class BotJsReplyProvider implements ReplyProvider {
    private static final String TAG = "BotJsReplyProvider";
    private static final String BOT_FILE_NAME = "active-bot.js";
    private static final int MAX_EXECUTIONS_PER_MINUTE = 100;
    private static final long RATE_LIMIT_WINDOW_MS = 60000; // 1 minuto
    
    // Rate limiter compartido para todas las instancias
    private static final RateLimiter rateLimiter = new RateLimiter(
        MAX_EXECUTIONS_PER_MINUTE, 
        RATE_LIMIT_WINDOW_MS
    );
    
    private final Gson gson = new Gson();

    @Override
    public void generateReply(Context context,
                             String incomingMessage,
                             NotificationData notificationData,
                             ReplyCallback callback) {
        // Ejecutar en thread background para no bloquear el UI thread
        new Thread(() -> {
            try {
                // Verificar rate limiting
                if (!rateLimiter.tryAcquire()) {
                    Log.w(TAG, "Rate limit exceeded, skipping bot execution");
                    callback.onFailure("Rate limit exceeded");
                    return;
                }
                
                // Cargar bot.js desde almacenamiento interno
                String jsCode = loadBotCode(context);
                
                if (jsCode == null || jsCode.trim().isEmpty()) {
                    Log.e(TAG, "Bot code not found or empty");
                    callback.onFailure("Bot code not found");
                    return;
                }

                // Validar código
                if (!BotValidator.validate(jsCode)) {
                    Log.e(TAG, "Bot code validation failed");
                    callback.onFailure("Bot validation failed");
                    return;
                }

                // Ejecutar bot
                BotJsEngine engine = new BotJsEngine(context);
                String responseJson = engine.executeBot(jsCode, notificationData);

                // Parsear respuesta
                JsonObject responseObj = gson.fromJson(responseJson, JsonObject.class);
                String action = responseObj.get("action").getAsString();

                // Manejar acción
                switch (action) {
                    case "REPLY":
                        if (responseObj.has("replyText")) {
                            String replyText = responseObj.get("replyText").getAsString();
                            callback.onSuccess(replyText);
                        } else {
                            Log.e(TAG, "REPLY action missing replyText");
                            callback.onFailure("Bot response missing replyText");
                        }
                        break;
                        
                    case "DISMISS":
                        callback.onFailure("DISMISS");
                        break;
                        
                    case "KEEP":
                        callback.onFailure("KEEP");
                        break;
                        
                    case "SNOOZE":
                        callback.onFailure("SNOOZE");
                        break;
                        
                    default:
                        Log.e(TAG, "Unknown bot action: " + action);
                        callback.onFailure("Unknown action: " + action);
                }

            } catch (TimeoutException e) {
                Log.e(TAG, "Bot execution timeout", e);
                callback.onFailure("Bot execution timeout");
            } catch (Exception e) {
                Log.e(TAG, "Bot execution failed", e);
                callback.onFailure("Bot execution error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Carga el código del bot desde el almacenamiento interno
     */
    private String loadBotCode(Context context) {
        try {
            File botsDir = new File(context.getFilesDir(), "bots");
            File botFile = new File(botsDir, BOT_FILE_NAME);
            
            if (!botFile.exists()) {
                Log.w(TAG, "Bot file not found: " + botFile.getAbsolutePath());
                return null;
            }

            StringBuilder sb = new StringBuilder();
            try (FileReader reader = new FileReader(botFile)) {
                char[] buffer = new char[8192];
                int charsRead;
                while ((charsRead = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, charsRead);
                }
            }
            
            return sb.toString();
            
        } catch (IOException e) {
            Log.e(TAG, "Error loading bot code", e);
            return null;
        }
    }
}
