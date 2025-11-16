package com.parishod.watomagic.botjs;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
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
     * Usa JSFunction de QuickJS para exponer métodos Java como funciones JavaScript
     */
    private void injectAndroidAPIs(QuickJs quickJs) {
        try {
            // Crear funciones wrapper que llamen a los métodos Java
            // QuickJS requiere usar JSFunction con la firma correcta
            app.cash.quickjs.JSFunction logFunction = new app.cash.quickjs.JSFunction() {
                @Override
                public Object call(Object... args) {
                    androidAPI.log(args[0].toString(), args[1].toString());
                    return null;
                }
            };
            
            app.cash.quickjs.JSFunction storageGetFunction = new app.cash.quickjs.JSFunction() {
                @Override
                public Object call(Object... args) {
                    return androidAPI.storageGet(args[0].toString());
                }
            };
            
            app.cash.quickjs.JSFunction storageSetFunction = new app.cash.quickjs.JSFunction() {
                @Override
                public Object call(Object... args) {
                    androidAPI.storageSet(args[0].toString(), args[1].toString());
                    return null;
                }
            };
            
            app.cash.quickjs.JSFunction storageRemoveFunction = new app.cash.quickjs.JSFunction() {
                @Override
                public Object call(Object... args) {
                    androidAPI.storageRemove(args[0].toString());
                    return null;
                }
            };
            
            app.cash.quickjs.JSFunction storageKeysFunction = new app.cash.quickjs.JSFunction() {
                @Override
                public Object call(Object... args) {
                    return androidAPI.storageKeys();
                }
            };
            
            app.cash.quickjs.JSFunction httpRequestFunction = new app.cash.quickjs.JSFunction() {
                @Override
                public Object call(Object... args) {
                    try {
                        return androidAPI.httpRequest(args[0].toString());
                    } catch (IOException e) {
                        throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
                    }
                }
            };
            
            app.cash.quickjs.JSFunction getCurrentTimeFunction = new app.cash.quickjs.JSFunction() {
                @Override
                public Object call(Object... args) {
                    return androidAPI.getCurrentTime();
                }
            };
            
            app.cash.quickjs.JSFunction getAppNameFunction = new app.cash.quickjs.JSFunction() {
                @Override
                public Object call(Object... args) {
                    return androidAPI.getAppName(args[0].toString());
                }
            };
            
            // Registrar funciones usando set() con JSFunction.class
            quickJs.set("AndroidAPI_log", app.cash.quickjs.JSFunction.class, logFunction);
            quickJs.set("AndroidAPI_storageGet", app.cash.quickjs.JSFunction.class, storageGetFunction);
            quickJs.set("AndroidAPI_storageSet", app.cash.quickjs.JSFunction.class, storageSetFunction);
            quickJs.set("AndroidAPI_storageRemove", app.cash.quickjs.JSFunction.class, storageRemoveFunction);
            quickJs.set("AndroidAPI_storageKeys", app.cash.quickjs.JSFunction.class, storageKeysFunction);
            quickJs.set("AndroidAPI_httpRequest", app.cash.quickjs.JSFunction.class, httpRequestFunction);
            quickJs.set("AndroidAPI_getCurrentTime", app.cash.quickjs.JSFunction.class, getCurrentTimeFunction);
            quickJs.set("AndroidAPI_getAppName", app.cash.quickjs.JSFunction.class, getAppNameFunction);
            
            // Crear objeto Android con todas las APIs que llama a las funciones Java
            String androidApiScript = 
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
                "};";
            
            quickJs.evaluate(androidApiScript);
            
        } catch (Exception e) {
            Log.e(TAG, "Error injecting Android APIs", e);
            throw new RuntimeException("Failed to inject Android APIs", e);
        }
    }
}
