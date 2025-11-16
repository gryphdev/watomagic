package com.parishod.watomagic.botjs;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.parishod.watomagic.replyproviders.NotificationData;

import java.io.IOException;
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
            
            Object resultObj = quickJs.evaluate(callScript);
            String result = resultObj != null ? resultObj.toString() : null;
            
            // Guardar storage de JavaScript de vuelta a Java
            saveStorageFromJS(quickJs);
            
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
     * Implementa las APIs directamente en JavaScript usando datos almacenados en variables globales
     * que se sincronizan con el storage persistente de Java
     */
    private void injectAndroidAPIs(QuickJs quickJs) {
        try {
            // Cargar storage persistente desde Java primero
            String[] storageKeys = androidAPI.storageKeys();
            StringBuilder storageInit = new StringBuilder("var _androidStorage = {");
            if (storageKeys.length > 0) {
                for (int i = 0; i < storageKeys.length; i++) {
                    String value = androidAPI.storageGet(storageKeys[i]);
                    if (value != null) {
                        // Escapar correctamente para JSON
                        String escapedKey = storageKeys[i].replace("\\", "\\\\").replace("\"", "\\\"");
                        String escapedValue = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
                        storageInit.append("\"").append(escapedKey).append("\":\"").append(escapedValue).append("\"");
                        if (i < storageKeys.length - 1) {
                            storageInit.append(",");
                        }
                    }
                }
            }
            storageInit.append("};");
            quickJs.evaluate(storageInit.toString());
            
            // Crear objeto Android con implementación en JavaScript puro
            // Las funciones usan _androidStorage que se sincroniza con Java
            String androidApiScript = 
                "const Android = {\n" +
                "  log: function(level, message) {\n" +
                "    // Logging se maneja en JavaScript (puede extenderse para logging real)\n" +
                "    console.log('[' + level + '] ' + message);\n" +
                "  },\n" +
                "  storageGet: function(key) {\n" +
                "    return _androidStorage[key] || null;\n" +
                "  },\n" +
                "  storageSet: function(key, value) {\n" +
                "    _androidStorage[key] = value;\n" +
                "  },\n" +
                "  storageRemove: function(key) {\n" +
                "    delete _androidStorage[key];\n" +
                "  },\n" +
                "  storageKeys: function() {\n" +
                "    return Object.keys(_androidStorage);\n" +
                "  },\n" +
                "  httpRequest: function(options) {\n" +
                "    // HTTP requests no están disponibles en esta implementación básica\n" +
                "    // Se puede extender en el futuro\n" +
                "    return Promise.reject(new Error('HTTP requests not available in basic implementation'));\n" +
                "  },\n" +
                "  getCurrentTime: function() {\n" +
                "    return Date.now();\n" +
                "  },\n" +
                "  getAppName: function(packageName) {\n" +
                "    return packageName;\n" +
                "  }\n" +
                "};";
            
            quickJs.evaluate(androidApiScript);
            
            Log.d(TAG, "Android APIs injected (JavaScript-only implementation)");
            
        } catch (Exception e) {
            Log.e(TAG, "Error injecting Android APIs", e);
            throw new RuntimeException("Failed to inject Android APIs", e);
        }
    }
    
    /**
     * Guarda el storage de JavaScript de vuelta a Java después de la ejecución
     */
    private void saveStorageFromJS(QuickJs quickJs) {
        try {
            // Leer storage desde JavaScript
            Object storageObj = quickJs.evaluate("JSON.stringify(_androidStorage)");
            if (storageObj != null) {
                String storageJson = storageObj.toString();
                if (!storageJson.equals("{}") && !storageJson.equals("undefined")) {
                    // Parsear JSON y guardar en Java
                    JsonObject storage = gson.fromJson(storageJson, JsonObject.class);
                    if (storage != null) {
                        for (java.util.Map.Entry<String, JsonElement> entry : storage.entrySet()) {
                            String key = entry.getKey();
                            JsonElement valueElement = entry.getValue();
                            String value = valueElement.isJsonNull() ? null : valueElement.getAsString();
                            if (value != null) {
                                androidAPI.storageSet(key, value);
                            } else {
                                androidAPI.storageRemove(key);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error saving storage from JS", e);
        }
    }
}
