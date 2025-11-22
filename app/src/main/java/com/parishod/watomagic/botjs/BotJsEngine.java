package com.parishod.watomagic.botjs;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.parishod.watomagic.replyproviders.model.NotificationData;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.ScriptRuntime;

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
    private final BotAndroidAPI androidAPI;

    public BotJsEngine(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.androidAPI = new BotAndroidAPI(this.context);
    }

    public void initialize() {
        // No-op: Context is now created per-execution thread
        // This method is kept for API compatibility
        Log.i(TAG, "BotJsEngine ready (Context will be created per execution)");
    }

    public String executeBot(@NonNull String jsCode,
                             @NonNull NotificationData notificationData)
            throws BotExecutionException {
        Callable<String> task = () -> {
            // Create Rhino Context on the execution thread (required for thread-local Context)
            org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
            try {
                // Configurar para Android (modo interpretado, no compilado)
                rhinoContext.setOptimizationLevel(-1);

                // Configurar límites de seguridad
                rhinoContext.setInstructionObserverThreshold(10000);
                rhinoContext.setMaximumInterpreterStackDepth(100);

                // Crear scope global con objetos estándar de JavaScript
                Scriptable scope = rhinoContext.initStandardObjects();

                // Inyectar API de Android en este scope
                injectAndroidAPIs(rhinoContext, scope);

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
            } finally {
                // Always exit Context on the execution thread
                org.mozilla.javascript.Context.exit();
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
        // No-op: Context is now created per-execution and cleaned up automatically
        // This method is kept for API compatibility
    }

    private void injectAndroidAPIs(org.mozilla.javascript.Context rhinoContext, Scriptable scope) {
        try {
            // Crear un ScriptableObject personalizado que expone los métodos de BotAndroidAPI
            ScriptableObject androidObject = new ScriptableObject() {
                @Override
                public String getClassName() {
                    return "Android";
                }

                @Override
                public Object get(String name, Scriptable start) {
                    // Exponer métodos como funciones usando BaseFunction
                    if ("log".equals(name)) {
                        return new org.mozilla.javascript.BaseFunction() {
                            @Override
                            public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                                if (args.length < 2) {
                                    throw ScriptRuntime.constructError("TypeError", "log requires 2 arguments: level and message");
                                }
                                String level = org.mozilla.javascript.Context.toString(args[0]);
                                String message = org.mozilla.javascript.Context.toString(args[1]);
                                androidAPI.log(level, message);
                                return org.mozilla.javascript.Context.getUndefinedValue();
                            }
                        };
                    } else if ("storageGet".equals(name)) {
                        return new org.mozilla.javascript.BaseFunction() {
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
                    } else if ("storageSet".equals(name)) {
                        return new org.mozilla.javascript.BaseFunction() {
                            @Override
                            public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                                if (args.length < 2) {
                                    throw ScriptRuntime.constructError("TypeError", "storageSet requires 2 arguments: key and value");
                                }
                                String key = org.mozilla.javascript.Context.toString(args[0]);
                                String value = org.mozilla.javascript.Context.toString(args[1]);
                                androidAPI.storageSet(key, value);
                                return org.mozilla.javascript.Context.getUndefinedValue();
                            }
                        };
                    } else if ("storageRemove".equals(name)) {
                        return new org.mozilla.javascript.BaseFunction() {
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
                    } else if ("storageKeys".equals(name)) {
                        return new org.mozilla.javascript.BaseFunction() {
                            @Override
                            public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                                String[] keys = androidAPI.storageKeys();
                                // Convertir array Java a array JavaScript
                                return cx.newArray(scope, keys);
                            }
                        };
                    } else if ("httpRequest".equals(name)) {
                        // Wrapper personalizado para httpRequest que convierte objetos JS a JSON
                        return new org.mozilla.javascript.BaseFunction() {
                            @Override
                            public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                                try {
                                    // Convertir argumento a JSON string si es un objeto
                                    String optionsJson;
                                    if (args.length == 0 || args[0] == null) {
                                        throw ScriptRuntime.constructError("TypeError", "httpRequest requires an options object");
                                    }
                                    
                                    if (args[0] instanceof NativeObject || args[0] instanceof Scriptable) {
                                        // Es un objeto JavaScript, convertirlo a JSON
                                        optionsJson = (String) NativeJSON.stringify(cx, scope, args[0], null, null);
                                    } else {
                                        // Ya es un string
                                        optionsJson = org.mozilla.javascript.Context.toString(args[0]);
                                    }
                                    
                                    // Llamar al método Java
                                    return androidAPI.httpRequest(optionsJson);
                                } catch (java.io.IOException e) {
                                    throw ScriptRuntime.constructError("Error", "HTTP request failed: " + e.getMessage());
                                } catch (Exception e) {
                                    throw ScriptRuntime.constructError("Error", "httpRequest error: " + e.getMessage());
                                }
                            }
                        };
                    } else if ("getCurrentTime".equals(name)) {
                        return new org.mozilla.javascript.BaseFunction() {
                            @Override
                            public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                                return androidAPI.getCurrentTime();
                            }
                        };
                    } else if ("getAppName".equals(name)) {
                        return new org.mozilla.javascript.BaseFunction() {
                            @Override
                            public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                                if (args.length == 0 || args[0] == null) {
                                    throw ScriptRuntime.constructError("TypeError", "getAppName requires a package name");
                                }
                                String packageName = org.mozilla.javascript.Context.toString(args[0]);
                                return androidAPI.getAppName(packageName);
                            }
                        };
                    }
                    return Scriptable.NOT_FOUND;
                }

                @Override
                public boolean has(String name, Scriptable start) {
                    return "log".equals(name) || "storageGet".equals(name) || 
                           "storageSet".equals(name) || "storageRemove".equals(name) ||
                           "storageKeys".equals(name) || "httpRequest".equals(name) ||
                           "getCurrentTime".equals(name) || "getAppName".equals(name);
                }

                @Override
                public Object[] getIds() {
                    return new Object[]{"log", "storageGet", "storageSet", "storageRemove",
                                      "storageKeys", "httpRequest", "getCurrentTime", "getAppName"};
                }
            };

            // Establecer el prototipo y el scope
            androidObject.setPrototype(ScriptableObject.getObjectPrototype(scope));
            androidObject.setParentScope(scope);

            // Inyectar el objeto Android en el scope global
            ScriptableObject.putProperty(scope, "Android", androidObject);

            // Crear e inyectar localStorage wrapper que usa Android.storage* internamente
            injectLocalStorage(rhinoContext, scope);

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
    private void injectLocalStorage(org.mozilla.javascript.Context rhinoContext, Scriptable scope) {
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
                    throw ScriptRuntime.constructError("TypeError", "Failed to execute 'setItem' on 'Storage': 2 arguments required");
                }
                // Validar que key no sea null/undefined
                if (args[0] == null || args[0] == org.mozilla.javascript.Context.getUndefinedValue()) {
                    throw ScriptRuntime.constructError("TypeError", "Failed to execute 'setItem' on 'Storage': key cannot be null or undefined");
                }
                String key = org.mozilla.javascript.Context.toString(args[0]);
                // value puede ser null/undefined (se convierte a string "null" o "undefined")
                String value = args[1] == null || args[1] == org.mozilla.javascript.Context.getUndefinedValue()
                    ? "null"
                    : org.mozilla.javascript.Context.toString(args[1]);
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
                if (args.length == 0 || args[0] == null || args[0] == org.mozilla.javascript.Context.getUndefinedValue()) {
                    return null;
                }
                // Validar que el argumento sea un número válido
                double numValue = org.mozilla.javascript.Context.toNumber(args[0]);
                if (Double.isNaN(numValue) || Double.isInfinite(numValue)) {
                    return null;
                }
                int index = (int) numValue;
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
                throw ScriptRuntime.constructError("TypeError", "Cannot delete property '" + name + "' on Storage object");
            }
        };

        // Inyectar localStorage como objeto global
        ScriptableObject.putProperty(scope, "localStorage", localStorage);
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
