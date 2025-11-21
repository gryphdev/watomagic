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

            Log.i(TAG, "Android APIs injected successfully via Rhino");
            Log.i(TAG, "Available APIs: log, storageGet, storageSet, storageRemove, " +
                      "storageKeys, httpRequest, getCurrentTime, getAppName");

        } catch (Exception e) {
            Log.e(TAG, "Failed to inject Android APIs", e);
            throw new RuntimeException("Android API injection failed: " + e.getMessage(), e);
        }
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
