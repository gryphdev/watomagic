package com.parishod.watomagic.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;

/**
 * Worker que limpia archivos adjuntos antiguos (>24 horas) cada 6 horas.
 */
public class AttachmentCleanupWorker extends Worker {
    private static final String TAG = "AttachmentCleanupWorker";
    private static final long MAX_AGE_MS = 24 * 60 * 60 * 1000; // 24 horas

    public AttachmentCleanupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            File attachmentsDir = new File(getApplicationContext().getExternalFilesDir(null), "bot_attachments");
            if (!attachmentsDir.exists() || !attachmentsDir.isDirectory()) {
                Log.d(TAG, "Attachments directory does not exist");
                return Result.success();
            }

            long currentTime = System.currentTimeMillis();
            int deletedCount = 0;
            long totalFreedBytes = 0;

            File[] files = attachmentsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        long fileAge = currentTime - file.lastModified();
                        if (fileAge > MAX_AGE_MS) {
                            long fileSize = file.length();
                            if (file.delete()) {
                                deletedCount++;
                                totalFreedBytes += fileSize;
                                Log.d(TAG, "Deleted old attachment: " + file.getName());
                            } else {
                                Log.w(TAG, "Failed to delete: " + file.getName());
                            }
                        }
                    }
                }
            }

            Log.i(TAG, String.format("Cleanup completed: %d files deleted, %d bytes freed", 
                    deletedCount, totalFreedBytes));
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error during attachment cleanup", e);
            return Result.retry();
        }
    }
}

