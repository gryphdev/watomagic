package com.parishod.watomagic.botjs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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
    private static final long DOWNLOAD_RATE_LIMIT_MS = 180000; // 3 minutos
    
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
     * @param url URL HTTPS del bot
     * @return Resultado de la descarga
     */
    public Result<BotInfo> downloadBot(String url) {
        return downloadBot(url, null);
    }

    /**
     * Descarga un bot desde una URL con validación SHA-256 opcional
     * @param url URL HTTPS del bot
     * @param expectedSha256 SHA-256 esperado (opcional, null para omitir validación)
     * @return Resultado de la descarga
     */
    public Result<BotInfo> downloadBot(String url, String expectedSha256) {
        try {
            // Validar HTTPS
            if (!url.startsWith("https://")) {
                return Result.error("Only HTTPS URLs are allowed");
            }

            // Rate limiting: verificar última descarga (bypass si es la misma URL para actualizaciones)
            SharedPreferences prefs = context.getSharedPreferences("bot_metadata", Context.MODE_PRIVATE);
            long lastDownload = prefs.getLong("last_download_time", 0);
            String lastUrl = prefs.getString("url", null);
            long now = System.currentTimeMillis();
            
            // Permitir bypass del rate limit si es la misma URL (actualización del mismo bot)
            boolean isSameUrl = lastUrl != null && url.equals(lastUrl);
            
            if (!isSameUrl && lastDownload > 0 && (now - lastDownload) < DOWNLOAD_RATE_LIMIT_MS) {
                long remainingMs = DOWNLOAD_RATE_LIMIT_MS - (now - lastDownload);
                long remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingMs);
                long remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMs) % 60;
                
                String timeRemaining;
                if (remainingMinutes > 0) {
                    timeRemaining = String.format("%d minute%s and %d second%s", 
                        remainingMinutes, remainingMinutes != 1 ? "s" : "",
                        remainingSeconds, remainingSeconds != 1 ? "s" : "");
                } else {
                    timeRemaining = String.format("%d second%s", 
                        remainingSeconds, remainingSeconds != 1 ? "s" : "");
                }
                
                return Result.error("Rate limit: Please wait " + timeRemaining + " before downloading again");
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
                return Result.error("Bot validation failed. Enable debug mode in bot settings to see detailed error messages, or check Logcat with tag 'BotValidator'");
            }

            // Calcular hash
            String hash = calculateSHA256(jsCode);

            // Validar SHA-256 si se proporcionó
            if (expectedSha256 != null && !expectedSha256.equalsIgnoreCase(hash)) {
                return Result.error("SHA-256 hash mismatch. Expected: " + expectedSha256 + ", got: " + hash);
            }

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

