package com.parishod.watomagic.replyproviders;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.parishod.watomagic.botjs.AttachmentSender;
import com.parishod.watomagic.botjs.BotExecutionException;
import com.parishod.watomagic.botjs.BotJsEngine;
import com.parishod.watomagic.botjs.BotLogCapture;
import com.parishod.watomagic.botjs.BotValidator;
import com.parishod.watomagic.botjs.RateLimiter;
import com.parishod.watomagic.model.preferences.PreferencesManager;
import com.parishod.watomagic.replyproviders.model.NotificationData;
import com.parishod.watomagic.model.utils.NotificationUtils;
import com.parishod.watomagic.service.NotificationService;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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
                             NotificationData notificationData,
                             ReplyCallback callback) {
        // Ejecutar en thread background para no bloquear el UI thread
        new Thread(() -> {
            try {
                // Log de inicio de ejecución
                if (BotLogCapture.isEnabled()) {
                    String title = NotificationUtils.getTitle(notificationData.getStatusBarNotification());
                    String packageName = notificationData.getStatusBarNotification().getPackageName();
                    String notifInfo = String.format("Bot execution started for: %s (package: %s)",
                            title != null ? title : "unknown",
                            packageName);
                    BotLogCapture.addLog("info", notifInfo);
                }

                // Verificar rate limiting
                if (!rateLimiter.tryAcquire()) {
                    Log.w(TAG, "Rate limit exceeded, skipping bot execution");
                    if (BotLogCapture.isEnabled()) {
                        BotLogCapture.addLog("warn", "Rate limit exceeded - bot execution skipped");
                    }
                    callback.onFailure("Rate limit exceeded");
                    return;
                }

                // Cargar bot.js desde almacenamiento interno
                String jsCode = loadBotCode(context);

                if (jsCode == null || jsCode.trim().isEmpty()) {
                    Log.e(TAG, "Bot code not found or empty");
                    if (BotLogCapture.isEnabled()) {
                        BotLogCapture.addLog("error", "Bot code not found or empty");
                    }
                    callback.onFailure("Bot code not found");
                    return;
                }

                if (BotLogCapture.isEnabled()) {
                    BotLogCapture.addLog("info", String.format("Bot code loaded (%d bytes)", jsCode.length()));
                }

                // Validar código
                if (!BotValidator.validate(jsCode)) {
                    Log.e(TAG, "Bot code validation failed");
                    if (BotLogCapture.isEnabled()) {
                        BotLogCapture.addLog("error", "Bot code validation failed");
                    }
                    callback.onFailure("Bot validation failed");
                    return;
                }

                if (BotLogCapture.isEnabled()) {
                    BotLogCapture.addLog("info", "Bot code validated successfully");
                }

                // Ejecutar bot
                BotJsEngine engine = new BotJsEngine(context);
                engine.initialize();
                try {
                    if (BotLogCapture.isEnabled()) {
                        BotLogCapture.addLog("info", "Executing bot script...");
                    }

                    String responseJson = engine.executeBot(jsCode, notificationData);

                    if (BotLogCapture.isEnabled()) {
                        BotLogCapture.addLog("info", "Bot execution completed, parsing response...");
                    }

                    // Parsear respuesta
                    JsonObject responseObj = gson.fromJson(responseJson, JsonObject.class);
                    String action = responseObj.get("action").getAsString();

                    if (BotLogCapture.isEnabled()) {
                        BotLogCapture.addLog("info", String.format("Bot returned action: %s", action));
                    }

                    // Manejar acción
                    switch (action) {
                        case "REPLY":
                            if (responseObj.has("replyText")) {
                                String replyText = responseObj.get("replyText").getAsString();
                                
                                // Check if attachments are included and send images is enabled
                                java.util.List<AttachmentSender.AttachmentToSend> attachmentsToSend = null;
                                if (responseObj.has("attachments")) {
                                    PreferencesManager prefs = PreferencesManager.getPreferencesInstance(context);
                                    if (prefs.isBotJsSendImagesEnabled()) {
                                        try {
                                            com.google.gson.JsonArray attachmentsArray = responseObj.getAsJsonArray("attachments");
                                            attachmentsToSend = new java.util.ArrayList<>();
                                            for (int i = 0; i < attachmentsArray.size(); i++) {
                                                com.google.gson.JsonObject attObj = attachmentsArray.get(i).getAsJsonObject();
                                                String path = attObj.get("path").getAsString();
                                                String mimeType = attObj.get("mimeType").getAsString();
                                                attachmentsToSend.add(new AttachmentSender.AttachmentToSend(path, mimeType));
                                            }
                                            if (BotLogCapture.isEnabled()) {
                                                BotLogCapture.addLog("info", String.format("Bot included %d attachments", attachmentsToSend.size()));
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error parsing attachments", e);
                                        }
                                    }
                                }
                                
                                if (BotLogCapture.isEnabled()) {
                                    BotLogCapture.addLog("info", String.format("Sending reply: %s", replyText));
                                }
                                
                                // Send reply with attachments if available
                                if (attachmentsToSend != null && !attachmentsToSend.isEmpty()) {
                                    callback.onSuccess(replyText, attachmentsToSend);
                                } else {
                                    callback.onSuccess(replyText);
                                }
                            } else {
                                Log.e(TAG, "REPLY action missing replyText");
                                if (BotLogCapture.isEnabled()) {
                                    BotLogCapture.addLog("error", "REPLY action missing replyText field");
                                }
                                callback.onFailure("Bot response missing replyText");
                            }
                            break;

                        case "DISMISS":
                            if (BotLogCapture.isEnabled()) {
                                BotLogCapture.addLog("info", "Bot requested DISMISS - notification will be dismissed");
                            }
                            callback.onFailure("DISMISS");
                            break;

                        case "KEEP":
                            if (BotLogCapture.isEnabled()) {
                                BotLogCapture.addLog("info", "Bot requested KEEP - no action taken");
                            }
                            callback.onFailure("KEEP");
                            break;

                        case "SNOOZE":
                            if (BotLogCapture.isEnabled()) {
                                BotLogCapture.addLog("info", "Bot requested SNOOZE - notification will be snoozed");
                            }
                            callback.onFailure("SNOOZE");
                            break;

                        default:
                            Log.e(TAG, "Unknown bot action: " + action);
                            if (BotLogCapture.isEnabled()) {
                                BotLogCapture.addLog("error", String.format("Unknown bot action: %s", action));
                            }
                            callback.onFailure("Unknown action: " + action);
                    }
                } finally {
                    engine.cleanup();
                }

            } catch (BotExecutionException e) {
                Log.e(TAG, "Bot execution failed", e);
                if (BotLogCapture.isEnabled()) {
                    String errorDetails = String.format("Bot execution failed: %s",
                            e.getDetailedMessage() != null ? e.getDetailedMessage() : e.getMessage());
                    BotLogCapture.addLog("error", errorDetails);
                }
                callback.onFailure("Bot execution error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Bot execution failed", e);
                if (BotLogCapture.isEnabled()) {
                    BotLogCapture.addLog("error", String.format("Unexpected error: %s", e.getMessage()));
                }
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

