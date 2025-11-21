package com.parishod.watomagic.botjs;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.parishod.watomagic.replyproviders.model.NotificationData;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Motor JavaScript usando Rhino para ejecutar bots.
 *
 * Rhino proporciona:
 * - Interoperabilidad completa Java↔JavaScript
 * - API estándar JSR-223 de Java
 * - Tamaño pequeño (~1.5 MB)
 * - 100% Java (sin binarios nativos)
 */
public class BotJsEngine {

    private static final String TAG = "BotJsEngine";
    private static final int EXECUTION_TIMEOUT_MS = 5_000;

    private final Context context;
    private org.mozilla.javascript.Context rhinoContext;
    private Scriptable scope;
    private BotAndroidAPI androidAPI;

    public BotJsEngine(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void initialize() {
        // Entrar en contexto de Rhino
        rhinoContext = org.mozilla.javascript.Context.enter();

        // Configurar para Android (modo interpretado, no compilado)
        rhinoContext.setOptimizationLevel(-1);

        // Configurar límites de seguridad
        rhinoContext.setInstructionObserverThreshold(10000);
        rhinoContext.setMaximumInterpreterStackDepth(100);

        // Crear scope global con objetos estándar de JavaScript
        scope = rhinoContext.initStandardObjects();

        // Crear e inyectar API de Android
        androidAPI = new BotAndroidAPI(context);
        injectAndroidAPIs();

        Log.i(TAG, "Rhino engine initialized successfully");
    }

    public String executeBot(@NonNull String jsCode,
                             @NonNull NotificationData notificationData)
            throws BotExecutionException {
        ensureInitialized();

        Callable<String> task = () -> {
            try {
                // Evaluar el código del bot (define la función processNotification)
                rhinoContext.evaluateString(scope, jsCode, "bot.js", 1, null);

                // Verificar que processNotification existe
                Object processNotifObj = scope.get("processNotification", scope);
                if (!(processNotifObj instanceof Function)) {
                    throw new BotExecutionException(
                        "Bot must export processNotification function",
                        "processNotification not found or not a function",
                        ""
                    );
                }

                // Convertir NotificationData a objeto JavaScript
                String notificationJson = BotNotificationMapper.toJson(notificationData);
                Object notificationObj = NativeJSON.parse(rhinoContext, scope, notificationJson,
                    (cx, sc, thisObj, args) -> args[1]);

                // Llamar a processNotification con el objeto de notificación
                Function processNotification = (Function) processNotifObj;
                Object result = processNotification.call(rhinoContext, scope, scope,
                    new Object[]{notificationObj});

                // Convertir resultado a JSON
                if (result instanceof NativeObject) {
                    return (String) NativeJSON.stringify(rhinoContext, scope, result, null, null);
                } else {
                    return org.mozilla.javascript.Context.toString(result);
                }

            } catch (Exception e) {
                String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
                throw new BotExecutionException(
                        "Bot execution failed",
                        errorMessage,
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
        if (rhinoContext != null) {
            org.mozilla.javascript.Context.exit();
            rhinoContext = null;
            scope = null;
        }
    }

    private void injectAndroidAPIs() {
        try {
            // Crear objeto JavaScript AndroidWrapper que expone las APIs
            // Rhino permite exponer objetos Java directamente a JavaScript
            Object wrappedAndroid = org.mozilla.javascript.Context.javaToJS(androidAPI, scope);
            ScriptableObject.putProperty(scope, "Android", wrappedAndroid);

            // Crear e inyectar localStorage wrapper que usa Android.storage* internamente
            injectLocalStorage();

            Log.i(TAG, "Android APIs injected successfully via Rhino");
            Log.i(TAG, "Available APIs: log, storageGet, storageSet, storageRemove, " +
                      "storageKeys, httpRequest, getCurrentTime, getAppName");
            Log.i(TAG, "localStorage API available (wraps Android.storage*)");

        } catch (Exception e) {
            Log.e(TAG, "Failed to inject Android APIs", e);
            throw new RuntimeException("Android API injection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Inyecta un objeto localStorage global que usa Android.storage* internamente.
     * Proporciona la API estándar de localStorage (getItem, setItem, removeItem, clear, key, length).
     */
    private void injectLocalStorage() {
        // Crear funciones una vez para reutilizarlas (comportamiento estándar de localStorage)
        final org.mozilla.javascript.BaseFunction getItemFunc = new org.mozilla.javascript.BaseFunction() {
            @Override
            public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length == 0 || args[0] == null) {
                    return null;
                }
                String key = org.mozilla.javascript.Context.toString(args[0]);
                String value = androidAPI.storageGet(key);
                return value != null ? value : null;
            }
        };
        
        final org.mozilla.javascript.BaseFunction setItemFunc = new org.mozilla.javascript.BaseFunction() {
            @Override
            public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 2) {
                    throw new org.mozilla.javascript.EcmaError(
                        cx.createError("TypeError", "Failed to execute 'setItem' on 'Storage': 2 arguments required")
                    );
                }
                String key = org.mozilla.javascript.Context.toString(args[0]);
                String value = org.mozilla.javascript.Context.toString(args[1]);
                androidAPI.storageSet(key, value);
                return org.mozilla.javascript.Context.getUndefinedValue();
            }
        };
        
        final org.mozilla.javascript.BaseFunction removeItemFunc = new org.mozilla.javascript.BaseFunction() {
            @Override
            public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length == 0 || args[0] == null) {
                    return org.mozilla.javascript.Context.getUndefinedValue();
                }
                String key = org.mozilla.javascript.Context.toString(args[0]);
                androidAPI.storageRemove(key);
                return org.mozilla.javascript.Context.getUndefinedValue();
            }
        };
        
        final org.mozilla.javascript.BaseFunction clearFunc = new org.mozilla.javascript.BaseFunction() {
            @Override
            public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                String[] keys = androidAPI.storageKeys();
                for (String key : keys) {
                    androidAPI.storageRemove(key);
                }
                return org.mozilla.javascript.Context.getUndefinedValue();
            }
        };
        
        final org.mozilla.javascript.BaseFunction keyFunc = new org.mozilla.javascript.BaseFunction() {
            @Override
            public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length == 0) {
                    return null;
                }
                int index = (int) org.mozilla.javascript.Context.toNumber(args[0]);
                String[] keys = androidAPI.storageKeys();
                if (index < 0 || index >= keys.length) {
                    return null;
                }
                return keys[index];
            }
        };
        
        // Crear un ScriptableObject personalizado que implementa la API de localStorage
        ScriptableObject localStorage = new ScriptableObject() {
            @Override
            public String getClassName() {
                return "Storage";
            }

            @Override
            public Object get(String name, Scriptable start) {
                // Propiedad length: calculada dinámicamente
                if ("length".equals(name)) {
                    return androidAPI.storageKeys().length;
                }
                
                // Métodos: retornar las funciones reutilizables
                if ("getItem".equals(name)) {
                    return getItemFunc;
                }
                if ("setItem".equals(name)) {
                    return setItemFunc;
                }
                if ("removeItem".equals(name)) {
                    return removeItemFunc;
                }
                if ("clear".equals(name)) {
                    return clearFunc;
                }
                if ("key".equals(name)) {
                    return keyFunc;
                }
                
                // Propiedad no encontrada
                return Scriptable.NOT_FOUND;
            }

            @Override
            public void put(String name, Scriptable start, Object value) {
                // localStorage es de solo lectura: no se pueden agregar/modificar propiedades
                // El comportamiento estándar es que intentar establecer propiedades no hace nada
                // (no lanza error, simplemente ignora)
                // Esto permite que el código JavaScript intente establecer propiedades sin fallar
            }

            @Override
            public boolean has(String name, Scriptable start) {
                // Verificar si la propiedad existe
                return "length".equals(name) || "getItem".equals(name) || "setItem".equals(name) ||
                       "removeItem".equals(name) || "clear".equals(name) || "key".equals(name);
            }

            @Override
            public Object[] getIds() {
                // Retornar todas las propiedades disponibles
                return new Object[]{"length", "getItem", "setItem", "removeItem", "clear", "key"};
            }

            @Override
            public void delete(String name) {
                // localStorage no permite eliminar propiedades
                throw new org.mozilla.javascript.EcmaError(
                    rhinoContext.createError("TypeError", "Cannot delete property '" + name + "' on Storage object")
                );
            }
        };

        // Inyectar localStorage como objeto global
        ScriptableObject.putProperty(scope, "localStorage", localStorage);
    }

    private void ensureInitialized() {
        if (rhinoContext == null || scope == null) {
            throw new IllegalStateException("BotJsEngine is not initialized");
        }
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
            builder.append("\"isGroup\":").append(data.getStatusBarNotification().getNotification().extras.getBoolean("android.isGroupConversation", false));
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
