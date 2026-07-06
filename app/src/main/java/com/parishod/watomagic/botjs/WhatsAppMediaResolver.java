package com.parishod.watomagic.botjs;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.documentfile.provider.DocumentFile;

import com.parishod.watomagic.model.preferences.PreferencesManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Lee imágenes de WhatsApp desde una carpeta concedida vía SAF (ACTION_OPEN_DOCUMENT_TREE).
 */
public class WhatsAppMediaResolver {

    private static final String TAG = "WhatsAppMediaResolver";
    static final long TIMESTAMP_MARGIN_MS = 5_000L;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".webp"
    ));

    private final Context context;

    public WhatsAppMediaResolver(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean hasMediaFolderAccess() {
        return PreferencesManager.getPreferencesInstance(context).getBotJsWhatsAppMediaTreeUri() != null;
    }

    @Nullable
    public String readLatestImageBase64(long notificationTimestamp) {
        String treeUriString = PreferencesManager.getPreferencesInstance(context)
                .getBotJsWhatsAppMediaTreeUri();
        if (treeUriString == null) {
            return null;
        }

        DocumentFile tree = DocumentFile.fromTreeUri(context, Uri.parse(treeUriString));
        if (tree == null || !tree.canRead()) {
            Log.w(TAG, "SAF tree not readable");
            return null;
        }

        DocumentFile bestMatch = findBestMatchingImage(tree, notificationTimestamp);
        if (bestMatch == null) {
            Log.d(TAG, "No image found near timestamp " + notificationTimestamp);
            return null;
        }

        return readFileAsBase64(bestMatch);
    }

    @Nullable
    @VisibleForTesting
    DocumentFile findBestMatchingImage(@NonNull DocumentFile root, long notificationTimestamp) {
        long windowStart = notificationTimestamp - TIMESTAMP_MARGIN_MS;
        long windowEnd = notificationTimestamp + TIMESTAMP_MARGIN_MS;

        DocumentFile best = null;
        long bestModified = Long.MIN_VALUE;

        Deque<DocumentFile> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            DocumentFile current = queue.removeFirst();
            DocumentFile[] children = current.listFiles();
            if (children == null) {
                continue;
            }
            for (DocumentFile child : children) {
                if (child.isDirectory()) {
                    queue.addLast(child);
                } else if (child.isFile() && isImageFile(child.getName())) {
                    long modified = child.lastModified();
                    if (modified >= windowStart && modified <= windowEnd && modified > bestModified) {
                        bestModified = modified;
                        best = child;
                    }
                }
            }
        }
        return best;
    }

    @VisibleForTesting
    static boolean isImageFile(@Nullable String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String ext : IMAGE_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private String readFileAsBase64(@NonNull DocumentFile file) {
        try {
            long length = file.length();
            if (length <= 0 || length > MAX_FILE_SIZE) {
                return null;
            }

            InputStream inputStream = context.getContentResolver().openInputStream(file.getUri());
            if (inputStream == null) {
                return null;
            }

            try (InputStream in = inputStream; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[8192];
                int read;
                while ((read = in.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                    if (buffer.size() > MAX_FILE_SIZE) {
                        return null;
                    }
                }
                return Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading SAF image", e);
            return null;
        }
    }
}
