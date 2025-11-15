package com.parishod.watomagic.botjs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * API de Android expuesta a los bots JavaScript
 * Proporciona acceso seguro a funcionalidades del sistema
 */
public class BotAndroidAPI {
    private static final String TAG = "BotAndroidAPI";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final Context context;
    private final SharedPreferences botStorage;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public BotAndroidAPI(Context context) {
        this.context = context;
        this.botStorage = context.getSharedPreferences("botjs_storage", Context.MODE_PRIVATE);
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
    }

    /**
     * Registra un mensaje en los logs
     */
    public void log(String level, String message) {
        switch (level) {
            case "error":
                Log.e("BotJS", message);
                break;
            case "warn":
                Log.w("BotJS", message);
                break;
            case "info":
                Log.i("BotJS", message);
                break;
            default:
                Log.d("BotJS", message);
        }
    }

    /**
     * Obtiene un valor almacenado
     */
    public String storageGet(String key) {
        return botStorage.getString(key, null);
    }

    /**
     * Almacena un valor
     */
    public void storageSet(String key, String value) {
        botStorage.edit().putString(key, value).apply();
    }

    /**
     * Elimina un valor almacenado
     */
    public void storageRemove(String key) {
        botStorage.edit().remove(key).apply();
    }

    /**
     * Obtiene todas las claves almacenadas
     */
    public String[] storageKeys() {
        return botStorage.getAll().keySet().toArray(new String[0]);
    }

    /**
     * Realiza una petición HTTP
     * @param optionsJson JSON string con las opciones: {url, method?, headers?, body?}
     */
    public String httpRequest(String optionsJson) throws IOException {
        JsonObject options = JsonParser.parseString(optionsJson).getAsJsonObject();
        
        String url = options.get("url").getAsString();
        if (!url.startsWith("https://")) {
            throw new IOException("Only HTTPS URLs are allowed");
        }

        String method = options.has("method") ? options.get("method").getAsString() : "GET";
        RequestBody body = null;
        
        if (options.has("body")) {
            body = RequestBody.create(options.get("body").getAsString(), JSON);
        }

        Request.Builder requestBuilder = new Request.Builder().url(url);
        
        if (options.has("headers")) {
            JsonObject headers = options.getAsJsonObject("headers");
            headers.entrySet().forEach(entry -> {
                requestBuilder.addHeader(entry.getKey(), entry.getValue().getAsString());
            });
        }

        Request request = requestBuilder.method(method, body).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + response.message());
            }
            
            String responseBody = response.body() != null ? response.body().string() : "";
            return responseBody;
        }
    }

    /**
     * Obtiene el timestamp actual en milisegundos
     */
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    /**
     * Obtiene el nombre legible de una app
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
}
