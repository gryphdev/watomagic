package com.parishod.watomagic.botjs;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.parishod.watomagic.replyproviders.model.NotificationData;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import app.cash.quickjs.QuickJs;
import app.cash.quickjs.QuickJsException;

/**
 * Wrapper simple sobre QuickJS que permitirá ejecutar bot.js en futuras fases.
 */
public class BotJsEngine {

    private static final String TAG = "BotJsEngine";
    private static final int EXECUTION_TIMEOUT_MS = 5_000;

    private final Context context;
    private QuickJs quickJs;
    private BotAndroidAPI androidAPI;

    public BotJsEngine(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void initialize() {
        quickJs = QuickJs.create();
        androidAPI = new BotAndroidAPI(context);
        injectAndroidAPIs();
    }

    public String executeBot(@NonNull String jsCode,
                             @NonNull NotificationData notificationData)
            throws BotExecutionException {
        ensureInitialized();

        final String notificationJson = BotNotificationMapper.toJson(notificationData);
        Callable<String> task = () -> {
            try {
                quickJs.evaluate(jsCode, "bot.js");
                String serializedCall = buildInvocationScript(notificationJson);
                Object result = quickJs.evaluate(serializedCall, "bot-invoke.js");
                return result != null ? result.toString() : "";
            } catch (QuickJsException e) {
                throw new BotExecutionException(
                        "Bot execution failed",
                        e.getMessage(),
                        Log.getStackTraceString(e)
                );
            }
        };

        try {
            return TimeoutExecutor.executeWithTimeout(task, EXECUTION_TIMEOUT_MS);
        } catch (TimeoutException e) {
            throw new BotExecutionException("Bot execution timed out",
                    e.getMessage(), "");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof BotExecutionException) {
                throw (BotExecutionException) e.getCause();
            }
            throw new BotExecutionException("Bot execution failed",
                    e.getMessage(), Log.getStackTraceString(e));
        }
    }

    public void cleanup() {
        if (quickJs != null) {
            quickJs.close();
            quickJs = null;
        }
    }

    private void injectAndroidAPIs() {
        // Inyectar API de Android en el runtime de QuickJS
        // QuickJS de Cash App requiere crear funciones JavaScript que llamen a código Java

        // Crear el objeto Android en el scope global
        String androidWrapper =
            "(function() {" +
            "  globalThis.Android = {" +
            "    _javaLog: null," +
            "    _javaStorageGet: null," +
            "    _javaStorageSet: null," +
            "    _javaStorageRemove: null," +
            "    _javaStorageKeys: null," +
            "    _javaHttpRequest: null," +
            "    _javaGetCurrentTime: null," +
            "    _javaGetAppName: null," +
            "    " +
            "    log: function(level, message) {" +
            "      if (!this._javaLog) throw new Error('Android.log not available');" +
            "      this._javaLog(level, message);" +
            "    }," +
            "    " +
            "    storageGet: function(key) {" +
            "      if (!this._javaStorageGet) throw new Error('Android.storageGet not available');" +
            "      return this._javaStorageGet(key);" +
            "    }," +
            "    " +
            "    storageSet: function(key, value) {" +
            "      if (!this._javaStorageSet) throw new Error('Android.storageSet not available');" +
            "      this._javaStorageSet(key, value);" +
            "    }," +
            "    " +
            "    storageRemove: function(key) {" +
            "      if (!this._javaStorageRemove) throw new Error('Android.storageRemove not available');" +
            "      this._javaStorageRemove(key);" +
            "    }," +
            "    " +
            "    storageKeys: function() {" +
            "      if (!this._javaStorageKeys) throw new Error('Android.storageKeys not available');" +
            "      return this._javaStorageKeys();" +
            "    }," +
            "    " +
            "    httpRequest: function(options) {" +
            "      if (!this._javaHttpRequest) throw new Error('Android.httpRequest not available');" +
            "      var optionsJson = typeof options === 'string' ? options : JSON.stringify(options);" +
            "      return this._javaHttpRequest(optionsJson);" +
            "    }," +
            "    " +
            "    getCurrentTime: function() {" +
            "      if (!this._javaGetCurrentTime) throw new Error('Android.getCurrentTime not available');" +
            "      return this._javaGetCurrentTime();" +
            "    }," +
            "    " +
            "    getAppName: function(packageName) {" +
            "      if (!this._javaGetAppName) throw new Error('Android.getAppName not available');" +
            "      return this._javaGetAppName(packageName);" +
            "    }" +
            "  };" +
            "})();";

        try {
            // Evaluar el wrapper que crea el objeto Android
            quickJs.evaluate(androidWrapper, "android-api-wrapper.js");

            // Inyectar las funciones Java usando la API de QuickJS
            // NOTA: QuickJS de Cash App usa set() para inyectar funciones
            // Cada función se registra como propiedad en Android._javaXXX

            // Log function
            quickJs.set("Android._javaLog", new LogFunction());

            // Storage functions
            quickJs.set("Android._javaStorageGet", new StorageGetFunction());
            quickJs.set("Android._javaStorageSet", new StorageSetFunction());
            quickJs.set("Android._javaStorageRemove", new StorageRemoveFunction());
            quickJs.set("Android._javaStorageKeys", new StorageKeysFunction());

            // HTTP function
            quickJs.set("Android._javaHttpRequest", new HttpRequestFunction());

            // Utility functions
            quickJs.set("Android._javaGetCurrentTime", new GetCurrentTimeFunction());
            quickJs.set("Android._javaGetAppName", new GetAppNameFunction());

            Log.i(TAG, "Android APIs injected successfully into QuickJS runtime");

        } catch (Exception e) {
            Log.e(TAG, "Failed to inject Android APIs", e);
            throw new RuntimeException("Android API injection failed: " + e.getMessage(), e);
        }
    }

    // Clases para implementar las funciones que QuickJS puede llamar
    // Estas implementan la interfaz funcional que QuickJS espera

    private class LogFunction implements java.util.function.BiConsumer<String, String> {
        @Override
        public void accept(String level, String message) {
            androidAPI.log(level, message);
        }
    }

    private class StorageGetFunction implements java.util.function.Function<String, String> {
        @Override
        public String apply(String key) {
            return androidAPI.storageGet(key);
        }
    }

    private class StorageSetFunction implements java.util.function.BiConsumer<String, String> {
        @Override
        public void accept(String key, String value) {
            androidAPI.storageSet(key, value);
        }
    }

    private class StorageRemoveFunction implements java.util.function.Consumer<String> {
        @Override
        public void accept(String key) {
            androidAPI.storageRemove(key);
        }
    }

    private class StorageKeysFunction implements java.util.function.Supplier<String[]> {
        @Override
        public String[] get() {
            return androidAPI.storageKeys();
        }
    }

    private class HttpRequestFunction implements java.util.function.Function<String, String> {
        @Override
        public String apply(String optionsJson) {
            try {
                return androidAPI.httpRequest(optionsJson);
            } catch (IOException e) {
                throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
            }
        }
    }

    private class GetCurrentTimeFunction implements java.util.function.Supplier<Long> {
        @Override
        public Long get() {
            return androidAPI.getCurrentTime();
        }
    }

    private class GetAppNameFunction implements java.util.function.Function<String, String> {
        @Override
        public String apply(String packageName) {
            return androidAPI.getAppName(packageName);
        }
    }

    private void ensureInitialized() {
        if (quickJs == null) {
            throw new IllegalStateException("BotJsEngine is not initialized");
        }
    }

    private String buildInvocationScript(String notificationJson) {
        String escapedJson = escapeForSingleQuotes(notificationJson);
        return "JSON.stringify(processNotification(JSON.parse('"
                + escapedJson + "')))";
    }

    private String escapeForSingleQuotes(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'");
    }

    /**
     * Utilidad interna para mapear NotificationData a JSON.
     */
    private static final class BotNotificationMapper {
        private BotNotificationMapper() {
        }

        static String toJson(NotificationData data) {
            StringBuilder builder = new StringBuilder();
            builder.append('{');
            builder.append("\"id\":").append(data.getStatusBarNotification().getId()).append(',');
            builder.append("\"appPackage\":\"").append(escape(data.getStatusBarNotification().getPackageName())).append("\",");
            builder.append("\"title\":\"").append(escape(safeString(data.getStatusBarNotification().getNotification().extras.getCharSequence("android.title")))).append("\",");
            builder.append("\"body\":\"").append(escape(safeString(data.getStatusBarNotification().getNotification().extras.getCharSequence("android.text")))).append("\",");
            builder.append("\"timestamp\":").append(data.getStatusBarNotification().getPostTime()).append(',');
            builder.append("\"isGroup\":").append(data.getStatusBarNotification().getNotification().extras.getBoolean("android.isGroupConversation", false)).append(',');
            builder.append("\"actions\":[]"); // Placeholder hasta mapear RemoteInputs.
            builder.append('}');
            return builder.toString();
        }

        private static String safeString(CharSequence value) {
            return value != null ? value.toString() : "";
        }

        private static String escape(String value) {
            return value
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
        }
    }
}
