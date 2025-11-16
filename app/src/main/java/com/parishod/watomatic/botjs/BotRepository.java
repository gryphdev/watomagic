package com.parishod.watomagic.botjs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.parishod.watomagic.model.preferences.PreferencesManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Repositorio para descargar y gestionar bots JavaScript
 */
public class BotRepository {
    private static final String TAG = "BotRepository";
    private static final String BOT_FILE_NAME = "active-bot.js";
    private static final long DOWNLOAD_RATE_LIMIT_MS = 3600000; // 1 hora
    
    private final Context context;
    private final OkHttpClient httpClient;
    private final File botsDir;

    public BotRepository(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
        this.botsDir = new File(context.getFilesDir(), "bots");
        botsDir.mkdirs();
    }

    /**
     * Resultado de una operación
     */
    public static class Result<T> {
        private final boolean success;
        private final T data;
        private final String error;

        private Result(boolean success, T data, String error) {
            this.success = success;
            this.data = data;
            this.error = error;
        }

        public static <T> Result<T> success(T data) {
            return new Result<>(true, data, null);
        }

        public static <T> Result<T> error(String error) {
            return new Result<>(false, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public T getData() {
            return data;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * Información del bot instalado
     */
    public static class BotInfo {
        public final String url;
        public final long timestamp;
        public final String hash;

        public BotInfo(String url, long timestamp, String hash) {
            this.url = url;
            this.timestamp = timestamp;
            this.hash = hash;
        }
    }

    /**
     * Descarga un bot desde una URL
     */
    public Result<BotInfo> downloadBot(String url) {
        try {
            // Validar HTTPS
            if (!url.startsWith("https://")) {
                return Result.error("Only HTTPS URLs are allowed");
            }

            // Rate limiting: verificar última descarga
            SharedPreferences prefs = context.getSharedPreferences("bot_metadata", Context.MODE_PRIVATE);
            long lastDownload = prefs.getLong("last_download_time", 0);
            long now = System.currentTimeMillis();
            
            if (lastDownload > 0 && (now - lastDownload) < DOWNLOAD_RATE_LIMIT_MS) {
                return Result.error("Rate limit: Please wait before downloading again");
            }

            // Descargar código
            Request request = new Request.Builder()
                .url(url)
                .build();

            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                return Result.error("Download failed: HTTP " + response.code());
            }

            String jsCode = response.body() != null ? response.body().string() : "";
            if (jsCode == null || jsCode.trim().isEmpty()) {
                return Result.error("Downloaded bot code is empty");
            }

            // Validar código
            if (!BotValidator.validate(jsCode)) {
                return Result.error("Bot validation failed");
            }

            // Calcular hash
            String hash = calculateSHA256(jsCode);

            // Guardar en almacenamiento interno
            File botFile = new File(botsDir, BOT_FILE_NAME);
            try (FileWriter writer = new FileWriter(botFile)) {
                writer.write(jsCode);
            }

            // Guardar metadata
            BotInfo botInfo = new BotInfo(url, now, hash);
            saveBotMetadata(botInfo);
            
            // Actualizar tiempo de última descarga
            prefs.edit().putLong("last_download_time", now).apply();

            return Result.success(botInfo);

        } catch (IOException e) {
            Log.e(TAG, "Download failed", e);
            return Result.error("Download failed: " + e.getMessage());
        }
    }

    /**
     * Verifica si hay actualizaciones disponibles
     */
    public boolean checkForUpdates() {
        try {
            BotInfo installedBot = getInstalledBotInfo();
            if (installedBot == null) {
                return false;
            }

            // Descargar bot remoto para comparar hash
            Request request = new Request.Builder()
                .url(installedBot.url)
                .build();

            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                return false;
            }

            String remoteCode = response.body() != null ? response.body().string() : "";
            if (remoteCode == null || remoteCode.trim().isEmpty()) {
                return false;
            }

            String remoteHash = calculateSHA256(remoteCode);
            return !remoteHash.equals(installedBot.hash);

        } catch (Exception e) {
            Log.e(TAG, "Error checking for updates", e);
            return false;
        }
    }

    /**
     * Obtiene información del bot instalado
     */
    public BotInfo getInstalledBotInfo() {
        SharedPreferences prefs = context.getSharedPreferences("bot_metadata", Context.MODE_PRIVATE);
        String url = prefs.getString("url", null);
        long timestamp = prefs.getLong("timestamp", 0);
        String hash = prefs.getString("hash", null);

        if (url == null) {
            return null;
        }

        return new BotInfo(url, timestamp, hash);
    }

    /**
     * Elimina el bot instalado
     */
    public void deleteBot() {
        File botFile = new File(botsDir, BOT_FILE_NAME);
        if (botFile.exists()) {
            botFile.delete();
        }

        context.getSharedPreferences("bot_metadata", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply();
    }

    /**
     * Guarda metadata del bot
     */
    private void saveBotMetadata(BotInfo info) {
        context.getSharedPreferences("bot_metadata", Context.MODE_PRIVATE)
            .edit()
            .putString("url", info.url)
            .putLong("timestamp", info.timestamp)
            .putString("hash", info.hash)
            .apply();
    }

    /**
     * Calcula SHA-256 hash de un string
     */
    private String calculateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error calculating hash", e);
            return "";
        }
    }
}
