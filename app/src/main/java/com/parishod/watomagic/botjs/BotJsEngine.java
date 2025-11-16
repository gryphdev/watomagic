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
        // TODO: Inyectar los puentes Java ↔ JS cuando se exponga BotAndroidAPI al runtime.
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
