package com.parishod.watomagic.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.parishod.watomagic.R;
import com.parishod.watomagic.botjs.BotRepository;
import com.parishod.watomagic.model.preferences.PreferencesManager;

/**
 * Worker que verifica y descarga actualizaciones de bots en background.
 * Se ejecuta periódicamente cada 6 horas.
 */
public class BotUpdateWorker extends Worker {
    private static final String TAG = "BotUpdateWorker";
    private static final String NOTIFICATION_CHANNEL_ID = "bot_update_channel";
    private static final int NOTIFICATION_ID = 1001;

    public BotUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        PreferencesManager prefs = PreferencesManager.getPreferencesInstance(getApplicationContext());

        // Verificar si BotJS está habilitado
        if (!prefs.isBotJsEnabled()) {
            Log.d(TAG, "BotJS disabled, skipping update check");
            return Result.success();
        }

        // Verificar si auto-update está habilitado
        if (!prefs.isBotAutoUpdateEnabled()) {
            Log.d(TAG, "Bot auto-update disabled, skipping update check");
            return Result.success();
        }

        String botUrl = prefs.getBotJsUrl();
        if (botUrl == null || botUrl.trim().isEmpty()) {
            Log.d(TAG, "No bot URL configured, skipping update check");
            return Result.success();
        }

        BotRepository repository = new BotRepository(getApplicationContext());

        // Verificar si hay actualizaciones
        if (repository.checkForUpdates()) {
            Log.i(TAG, "Bot update available, downloading...");
            
            BotRepository.Result<BotRepository.BotInfo> result = repository.downloadBot(botUrl);

            if (result.isSuccess()) {
                Log.i(TAG, "Bot updated successfully");
                showUpdateNotification();
                return Result.success();
            } else {
                Log.e(TAG, "Bot update failed: " + result.getError());
                // No fallar el worker, solo loguear el error
                return Result.success();
            }
        } else {
            Log.d(TAG, "No bot updates available");
            return Result.success();
        }
    }

    /**
     * Muestra una notificación informando que el bot fue actualizado
     */
    private void showUpdateNotification() {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(
                    getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Bot actualizado")
                    .setContentText("Tu bot JavaScript ha sido actualizado exitosamente")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error showing update notification", e);
        }
    }

    /**
     * Programa el worker para ejecutarse periódicamente cada 6 horas
     */
    public static void schedule(Context context) {
        PeriodicWorkRequest botUpdateWork = new PeriodicWorkRequest.Builder(
                BotUpdateWorker.class,
                6, java.util.concurrent.TimeUnit.HOURS
        ).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "BotUpdateWork",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                botUpdateWork
        );
        Log.d(TAG, "BotUpdateWorker scheduled");
    }
}
