package com.parishod.watomagic.botjs;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.parishod.watomagic.replyproviders.NotificationData;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import app.cash.quickjs.QuickJs;

/**
 * Motor JavaScript para ejecutar bots usando QuickJS
 */
public class BotJsEngine {
    private static final String TAG = "BotJsEngine";
    private static final int EXECUTION_TIMEOUT_MS = 5000;
    
    private final Context context;
    private final BotAndroidAPI androidAPI;
    private final Gson gson;

    public BotJsEngine(Context context) {
        this.context = context;
        this.androidAPI = new BotAndroidAPI(context);
        this.gson = new Gson();
    }

    /**
     * Ejecuta el bot con los datos de notificación
     * @param jsCode Código JavaScript del bot
     * @param notificationData Datos de la notificación
     * @return Respuesta del bot como JSON string
     * @throws ExecutionException Si hay un error en la ejecución
     * @throws TimeoutException Si la ejecución excede el timeout
     */
    public String executeBot(String jsCode, NotificationData notificationData)
            throws ExecutionException, TimeoutException {
        
        String notificationJson = gson.toJson(notificationData);
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return executeBotSync(jsCode, notificationJson);
            }
        });

        try {
            return future.get(EXECUTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new ExecutionException("Bot execution interrupted", e);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Bot execution exceeded " + EXECUTION_TIMEOUT_MS + "ms");
        } finally {
            executor.shutdownNow();
        }
    }

    private String executeBotSync(String jsCode, String notificationJson) throws Exception {
        QuickJs quickJs = null;
        try {
            quickJs = QuickJs.create();
            
            // Inyectar Android API
            injectAndroidAPIs(quickJs);
            
            // Ejecutar el código del bot
            quickJs.evaluate(jsCode);
            
            // Llamar a processNotification
            String callScript = String.format(
                "JSON.stringify(processNotification(%s))",
                notificationJson
            );
            
            String result = quickJs.evaluate(callScript);
            
            if (result == null || result.trim().isEmpty() || "undefined".equals(result)) {
                throw new Exception("Bot did not return a valid response");
            }
            
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing bot", e);
            throw e;
        } finally {
            if (quickJs != null) {
                try {
                    quickJs.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing QuickJS", e);
                }
            }
        }
    }

    /**
     * Inyecta las APIs de Android en el contexto JavaScript
     */
    private void injectAndroidAPIs(QuickJs quickJs) {
        try {
            // Crear objeto Android con todas las APIs
            String androidApiScript = String.format(
                "const Android = { " +
                "  log: function(level, message) { " +
                "    AndroidAPI_log(level, message); " +
                "  }, " +
                "  storageGet: function(key) { " +
                "    return AndroidAPI_storageGet(key); " +
                "  }, " +
                "  storageSet: function(key, value) { " +
                "    AndroidAPI_storageSet(key, value); " +
                "  }, " +
                "  storageRemove: function(key) { " +
                "    AndroidAPI_storageRemove(key); " +
                "  }, " +
                "  storageKeys: function() { " +
                "    return AndroidAPI_storageKeys(); " +
                "  }, " +
                "  httpRequest: function(options) { " +
                "    return AndroidAPI_httpRequest(JSON.stringify(options)); " +
                "  }, " +
                "  getCurrentTime: function() { " +
                "    return AndroidAPI_getCurrentTime(); " +
                "  }, " +
                "  getAppName: function(packageName) { " +
                "    return AndroidAPI_getAppName(packageName); " +
                "  } " +
                "};"
            );
            
            quickJs.evaluate(androidApiScript);
            
            // Registrar funciones Java como funciones JavaScript
            quickJs.set("AndroidAPI_log", androidAPI::log);
            quickJs.set("AndroidAPI_storageGet", androidAPI::storageGet);
            quickJs.set("AndroidAPI_storageSet", androidAPI::storageSet);
            quickJs.set("AndroidAPI_storageRemove", androidAPI::storageRemove);
            quickJs.set("AndroidAPI_storageKeys", androidAPI::storageKeys);
            quickJs.set("AndroidAPI_httpRequest", androidAPI::httpRequest);
            quickJs.set("AndroidAPI_getCurrentTime", androidAPI::getCurrentTime);
            quickJs.set("AndroidAPI_getAppName", androidAPI::getAppName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error injecting Android APIs", e);
            throw new RuntimeException("Failed to inject Android APIs", e);
        }
    }
}
