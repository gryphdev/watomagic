package com.parishod.watomagic.botjs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Exposición controlada de funcionalidades Android hacia los bots.
 */
public class BotAndroidAPI {

    private static final String TAG = "BotAndroidAPI";
    private static final String STORAGE_NAME = "bot_storage";
    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final Context context;
    private final SharedPreferences botStorage;
    private final OkHttpClient httpClient;

    public BotAndroidAPI(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.botStorage = this.context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    // Logging
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
                break;
        }
    }

    // Storage
    @Nullable
    public String storageGet(String key) {
        return botStorage.getString(key, null);
    }

    public void storageSet(String key, String value) {
        botStorage.edit().putString(key, value).apply();
    }

    public void storageRemove(String key) {
        botStorage.edit().remove(key).apply();
    }

    public String[] storageKeys() {
        Map<String, ?> all = botStorage.getAll();
        return all.keySet().toArray(new String[0]);
    }

    // HTTP
    public String httpRequest(String optionsJson) throws IOException {
        try {
            JSONObject options = new JSONObject(optionsJson);
            String url = options.getString("url");

            if (!url.startsWith("https://")) {
                throw new IOException("Only HTTPS URLs are allowed");
            }

            String method = options.optString("method", "GET").toUpperCase();
            JSONObject headersObj = options.optJSONObject("headers");
            String bodyString = options.optString("body", null);

            Request.Builder builder = new Request.Builder().url(url);
        if (headersObj != null) {
            Iterator<String> keys = headersObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                builder.addHeader(key, headersObj.optString(key));
            }
        }

            switch (method) {
                case "POST":
                case "PUT":
                    RequestBody requestBody = RequestBody.create(
                            bodyString != null ? bodyString : "",
                            extractMediaType(headersObj)
                    );
                    builder.method(method, requestBody);
                    break;
                case "DELETE":
                    builder.delete();
                    break;
                default:
                    builder.get();
                    break;
            }

            Response response = httpClient.newCall(builder.build()).execute();
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + response.message());
            }

            if (response.body() == null) {
                return "";
            }
            return response.body().string();

        } catch (JSONException e) {
            throw new IOException("Invalid options JSON", e);
        }
    }

    private MediaType extractMediaType(@Nullable JSONObject headers) {
        if (headers == null) return DEFAULT_MEDIA_TYPE;
        String contentType = headers.optString("Content-Type", null);
        if (contentType == null) {
            return DEFAULT_MEDIA_TYPE;
        }
        MediaType mediaType = MediaType.parse(contentType);
        return mediaType != null ? mediaType : DEFAULT_MEDIA_TYPE;
    }

    // Utils
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public String getAppName(String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            CharSequence label = pm.getApplicationLabel(appInfo);
            return label != null ? label.toString() : packageName;
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }
}
