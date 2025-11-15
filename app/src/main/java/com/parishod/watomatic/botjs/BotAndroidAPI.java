package com.parishod.watomagic.botjs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * API de Android expuesta a los bots JavaScript.
 * Proporciona funcionalidades de logging, storage, HTTP y utilidades.
 */
public class BotAndroidAPI {
    private static final String TAG = "BotAndroidAPI";
    private static final String BOT_STORAGE_PREFS = "botjs_storage";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final Context context;
    private final SharedPreferences botStorage;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public BotAndroidAPI(Context context) {
        this.context = context;
        this.botStorage = context.getSharedPreferences(BOT_STORAGE_PREFS, Context.MODE_PRIVATE);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * Registra un mensaje en los logs de la aplicación
     */
    public void log(String level, String message) {
        switch (level) {
            case "error":
                Log.e(TAG, message);
                break;
            case "warn":
                Log.w(TAG, message);
                break;
            case "info":
                Log.i(TAG, message);
                break;
            default:
                Log.d(TAG, message);
        }
    }

    /**
     * Obtiene un valor del storage persistente
     */
    public String storageGet(String key) {
        return botStorage.getString(key, null);
    }

    /**
     * Guarda un valor en el storage persistente
     */
    public void storageSet(String key, String value) {
        botStorage.edit().putString(key, value).apply();
    }

    /**
     * Elimina un valor del storage persistente
     */
    public void storageRemove(String key) {
        botStorage.edit().remove(key).apply();
    }

    /**
     * Obtiene todas las claves del storage
     */
    public String[] storageKeys() {
        Set<String> keys = botStorage.getAll().keySet();
        return keys.toArray(new String[0]);
    }

    /**
     * Realiza una petición HTTP
     * @param optionsJson JSON string con las opciones: {url, method?, headers?, body?}
     * @return Respuesta como string
     */
    public String httpRequest(String optionsJson) throws IOException {
        HttpRequestOptions options = gson.fromJson(optionsJson, HttpRequestOptions.class);
        
        if (options.url == null || options.url.isEmpty()) {
            throw new IllegalArgumentException("URL is required");
        }

        // Validar que sea HTTPS
        if (!options.url.startsWith("https://")) {
            throw new IllegalArgumentException("Only HTTPS URLs are allowed");
        }

        Request.Builder requestBuilder = new Request.Builder().url(options.url);

        // Método HTTP
        String method = (options.method != null) ? options.method : "GET";
        if (options.body != null && !options.body.isEmpty()) {
            RequestBody body = RequestBody.create(options.body, JSON);
            requestBuilder.method(method, body);
        } else if (!"GET".equals(method)) {
            RequestBody body = RequestBody.create("", JSON);
            requestBuilder.method(method, body);
        }

        // Headers
        if (options.headers != null) {
            for (java.util.Map.Entry<String, String> entry : options.headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Request request = requestBuilder.build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + response.message());
            }
            if (response.body() == null) {
                return "";
            }
            return response.body().string();
        }
    }

    /**
     * Obtiene el tiempo actual en milisegundos
     */
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    /**
     * Obtiene el nombre de una aplicación por su package name
     */
    public String getAppName(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    /**
     * Clase interna para deserializar opciones de HTTP request
     */
    private static class HttpRequestOptions {
        String url;
        String method;
        java.util.Map<String, String> headers;
        String body;
    }
}
