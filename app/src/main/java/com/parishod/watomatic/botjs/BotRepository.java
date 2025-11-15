package com.parishod.watomagic.botjs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Repositorio para descargar y gestionar bots JavaScript.
 * Maneja la descarga, validación, almacenamiento y actualización de bots.
 */
public class BotRepository {
    private static final String TAG = "BotRepository";
    private static final String BOT_METADATA_PREFS = "bot_metadata";
    private static final String KEY_BOT_URL = "bot_url";
    private static final String KEY_BOT_HASH = "bot_hash";
    private static final String KEY_BOT_TIMESTAMP = "bot_timestamp";
    private static final long DOWNLOAD_RATE_LIMIT_MS = 60 * 60 * 1000; // 1 hora
    private static final String KEY_LAST_DOWNLOAD_TIME = "last_download_time";

    private final Context context;
    private final OkHttpClient httpClient;
    private final File botsDir;
    private final SharedPreferences metadataPrefs;

    public BotRepository(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.botsDir = new File(context.getFilesDir(), "bots");
        this.botsDir.mkdirs();
        this.metadataPrefs = context.getSharedPreferences(BOT_METADATA_PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Resultado de una operación del repositorio
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
        // Validar HTTPS
        if (url == null || !url.startsWith("https://")) {
            return Result.error("Only HTTPS URLs are allowed");
        }

        // Rate limiting de descargas
        long lastDownload = metadataPrefs.getLong(KEY_LAST_DOWNLOAD_TIME, 0);
        long now = System.currentTimeMillis();
        if (now - lastDownload < DOWNLOAD_RATE_LIMIT_MS) {
            return Result.error("Download rate limit. Please wait before downloading again.");
        }

        try {
            // Descargar código
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                return Result.error("Download failed: HTTP " + response.code());
            }

            if (response.body() == null) {
                return Result.error("Download failed: Empty response");
            }

            String jsCode = response.body().string();

            // Validar código
            if (!BotValidator.validate(jsCode)) {
                return Result.error("Bot validation failed");
            }

            // Calcular hash
            String hash = calculateSHA256(jsCode);

            // Guardar en almacenamiento interno
            File botFile = new File(botsDir, "active-bot.js");
            try (FileWriter writer = new FileWriter(botFile)) {
                writer.write(jsCode);
            }

            // Guardar metadata
            long timestamp = System.currentTimeMillis();
            saveBotMetadata(url, hash, timestamp);
            metadataPrefs.edit().putLong(KEY_LAST_DOWNLOAD_TIME, now).apply();

            BotInfo botInfo = new BotInfo(url, timestamp, hash);
            Log.i(TAG, "Bot downloaded successfully from: " + url);
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
        String url = metadataPrefs.getString(KEY_BOT_URL, null);
        if (url == null) {
            return false;
        }

        try {
            // Descargar el bot remoto
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful() || response.body() == null) {
                return false;
            }

            String remoteCode = response.body().string();
            String remoteHash = calculateSHA256(remoteCode);

            // Comparar con hash local
            String localHash = metadataPrefs.getString(KEY_BOT_HASH, null);
            return localHash == null || !localHash.equals(remoteHash);

        } catch (Exception e) {
            Log.e(TAG, "Error checking for updates", e);
            return false;
        }
    }

    /**
     * Obtiene información del bot instalado
     */
    public BotInfo getInstalledBotInfo() {
        String url = metadataPrefs.getString(KEY_BOT_URL, null);
        if (url == null) {
            return null;
        }

        long timestamp = metadataPrefs.getLong(KEY_BOT_TIMESTAMP, 0);
        String hash = metadataPrefs.getString(KEY_BOT_HASH, null);

        return new BotInfo(url, timestamp, hash);
    }

    /**
     * Elimina el bot instalado
     */
    public void deleteBot() {
        File botFile = new File(botsDir, "active-bot.js");
        if (botFile.exists()) {
            botFile.delete();
        }

        metadataPrefs.edit()
                .remove(KEY_BOT_URL)
                .remove(KEY_BOT_HASH)
                .remove(KEY_BOT_TIMESTAMP)
                .apply();

        Log.i(TAG, "Bot deleted");
    }

    /**
     * Guarda metadata del bot
     */
    private void saveBotMetadata(String url, String hash, long timestamp) {
        metadataPrefs.edit()
                .putString(KEY_BOT_URL, url)
                .putString(KEY_BOT_HASH, hash)
                .putLong(KEY_BOT_TIMESTAMP, timestamp)
                .apply();
    }

    /**
     * Calcula el hash SHA-256 de un string
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
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            Log.e(TAG, "Error calculating hash", e);
            return "";
        }
    }
}
