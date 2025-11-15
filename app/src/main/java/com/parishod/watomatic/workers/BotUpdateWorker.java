package com.parishod.watomagic.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.parishod.watomagic.R;
import com.parishod.watomagic.botjs.BotRepository;
import com.parishod.watomagic.model.preferences.PreferencesManager;

/**
 * Worker para actualizar bots automáticamente en background
 * Se ejecuta cada 6 horas si está habilitado
 */
public class BotUpdateWorker extends Worker {
    private static final String TAG = "BotUpdateWorker";
    private static final String CHANNEL_ID = "bot_update_channel";

    public BotUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        PreferencesManager prefs = PreferencesManager.getPreferencesInstance(getApplicationContext());

        if (!prefs.isBotJsEnabled() || !prefs.isBotJsAutoUpdateEnabled()) {
            Log.d(TAG, "BotJS auto-update disabled, skipping");
            return Result.success();
        }

        String botUrl = prefs.getBotJsUrl();
        if (botUrl == null || botUrl.trim().isEmpty()) {
            Log.d(TAG, "No bot URL configured, skipping update");
            return Result.success();
        }

        BotRepository repository = new BotRepository(getApplicationContext());

        try {
            if (repository.checkForUpdates()) {
                Log.i(TAG, "Bot update available, downloading...");
                BotRepository.Result<BotRepository.BotInfo> result = repository.downloadBot(botUrl);

                if (result.isSuccess()) {
                    showUpdateNotification();
                    Log.i(TAG, "Bot updated successfully");
                    return Result.success();
                } else {
                    Log.e(TAG, "Bot update failed: " + result.getError());
                    return Result.retry();
                }
            } else {
                Log.d(TAG, "No bot updates available");
                return Result.success();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during bot update", e);
            return Result.retry();
        }
    }

    private void showUpdateNotification() {
        NotificationManager notificationManager = (NotificationManager) 
            getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Bot Updates",
                NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
            getApplicationContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Bot Actualizado")
            .setContentText("Tu bot JavaScript se ha actualizado automáticamente")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }
}
