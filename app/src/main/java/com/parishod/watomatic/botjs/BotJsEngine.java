package com.parishod.watomagic.botjs;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import app.cash.quickjs.QuickJs;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Motor JavaScript que ejecuta bots usando QuickJS.
 * Proporciona un sandbox seguro para ejecutar código JavaScript.
 */
public class BotJsEngine {
    private static final String TAG = "BotJsEngine";
    private static final int EXECUTION_TIMEOUT_MS = 5000;

    private final Context context;
    private QuickJs quickJs;
    private BotAndroidAPI androidAPI;
    private final Gson gson;

    public BotJsEngine(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    /**
     * Inicializa el motor JavaScript e inyecta las APIs de Android
     */
    public void initialize() {
        quickJs = QuickJs.create();
        androidAPI = new BotAndroidAPI(context);
        injectAndroidAPIs();
    }

    /**
     * Ejecuta el bot con los datos de notificación proporcionados
     * @param jsCode Código JavaScript del bot
     * @param notificationDataJson JSON con los datos de la notificación
     * @return JSON string con la respuesta del bot
     * @throws ExecutionException Si hay un error ejecutando el bot
     */
    public String executeBot(String jsCode, String notificationDataJson) throws ExecutionException {
        if (quickJs == null) {
            throw new IllegalStateException("Engine not initialized. Call initialize() first.");
        }

        try {
            // Ejecutar con timeout en un thread separado
            return executeWithTimeout(() -> {
                // Evaluar el código del bot
                quickJs.evaluate(jsCode);

                // Crear el wrapper para ejecutar processNotification
                String wrapperCode = String.format(
                    "(function() {" +
                    "  try {" +
                    "    const notification = %s;" +
                    "    const result = processNotification(notification);" +
                    "    if (result instanceof Promise) {" +
                    "      return result.then(r => JSON.stringify(r)).catch(e => JSON.stringify({action: 'KEEP', reason: e.message}));" +
                    "    } else {" +
                    "      return JSON.stringify(result);" +
                    "    }" +
                    "  } catch (e) {" +
                    "    return JSON.stringify({action: 'KEEP', reason: e.message});" +
                    "  }" +
                    "})()",
                    notificationDataJson
                );

                String result = quickJs.evaluate(wrapperCode);
                return result;
            }, EXECUTION_TIMEOUT_MS);
        } catch (TimeoutException e) {
            Log.e(TAG, "Bot execution timeout after " + EXECUTION_TIMEOUT_MS + "ms");
            throw new ExecutionException("Bot execution timeout", e);
        } catch (Exception e) {
            Log.e(TAG, "Bot execution error", e);
            throw new ExecutionException("Bot execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Inyecta las APIs de Android en el contexto JavaScript
     */
    private void injectAndroidAPIs() {
        // Crear objeto Android con todas las APIs
        String androidApiCode = String.format(
            "const Android = {" +
            "  log: function(level, message) { __android_log(level, message); }," +
            "  storageGet: function(key) { return __android_storageGet(key); }," +
            "  storageSet: function(key, value) { __android_storageSet(key, value); }," +
            "  storageRemove: function(key) { __android_storageRemove(key); }," +
            "  storageKeys: function() { return __android_storageKeys(); }," +
            "  httpRequest: function(options) {" +
            "    return new Promise(function(resolve, reject) {" +
            "      try {" +
            "        const optionsJson = JSON.stringify(options);" +
            "        const result = __android_httpRequest(optionsJson);" +
            "        resolve(result);" +
            "      } catch (e) {" +
            "        reject(e);" +
            "      }" +
            "    });" +
            "  }," +
            "  getCurrentTime: function() { return __android_getCurrentTime(); }," +
            "  getAppName: function(packageName) { return __android_getAppName(packageName); }" +
            "};"
        );

        quickJs.evaluate(androidApiCode);

        // Registrar funciones nativas
        quickJs.set("__android_log", (level, message) -> {
            androidAPI.log(level, message);
            return null;
        });

        quickJs.set("__android_storageGet", (key) -> androidAPI.storageGet(key));
        quickJs.set("__android_storageSet", (key, value) -> {
            androidAPI.storageSet(key, value);
            return null;
        });
        quickJs.set("__android_storageRemove", (key) -> {
            androidAPI.storageRemove(key);
            return null;
        });
        quickJs.set("__android_storageKeys", () -> {
            String[] keys = androidAPI.storageKeys();
            return gson.toJson(keys);
        });

        quickJs.set("__android_httpRequest", (optionsJson) -> {
            try {
                return androidAPI.httpRequest(optionsJson);
            } catch (Exception e) {
                throw new RuntimeException("HTTP request failed: " + e.getMessage());
            }
        });

        quickJs.set("__android_getCurrentTime", () -> androidAPI.getCurrentTime());
        quickJs.set("__android_getAppName", (packageName) -> androidAPI.getAppName(packageName));
    }

    /**
     * Ejecuta una tarea con timeout
     */
    private <T> T executeWithTimeout(Callable<T> task, long timeoutMs) 
            throws TimeoutException, ExecutionException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(task);

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Bot execution exceeded " + timeoutMs + "ms");
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new ExecutionException("Bot execution interrupted", e);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Limpia los recursos del motor
     */
    public void cleanup() {
        if (quickJs != null) {
            try {
                quickJs.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing QuickJS", e);
            }
            quickJs = null;
        }
    }
}
