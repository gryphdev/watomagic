package com.parishod.watomagic.botjs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AttachmentExtractorTest {

    @Before
    public void setUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = RuntimeEnvironment.getApplication()
                    .getSystemService(NotificationManager.class);
            nm.createNotificationChannel(
                    new NotificationChannel("test", "Test", NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    @Test
    public void isDuplicateBitmap_sameReference() {
        Bitmap bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
        List<Bitmap> seen = new ArrayList<>();
        seen.add(bitmap);
        assertTrue(AttachmentExtractor.isDuplicateBitmap(bitmap, seen));
        bitmap.recycle();
    }

    @Test
    public void isDuplicateBitmap_sameContent() {
        Bitmap first = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
        Bitmap second = first.copy(Bitmap.Config.ARGB_8888, false);
        List<Bitmap> seen = new ArrayList<>();
        seen.add(first);
        assertTrue(AttachmentExtractor.isDuplicateBitmap(second, seen));
        second.recycle();
        first.recycle();
    }

    @Test
    public void isDuplicateBitmap_differentContent() {
        Bitmap first = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
        Bitmap second = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        List<Bitmap> seen = new ArrayList<>();
        seen.add(first);
        assertFalse(AttachmentExtractor.isDuplicateBitmap(second, seen));
        second.recycle();
        first.recycle();
    }

    @Test
    public void extractAttachments_ignoresLargeIcon() {
        Bitmap avatar = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        Notification notification = new Notification.Builder(RuntimeEnvironment.getApplication(), "test")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Test")
                .setLargeIcon(Icon.createWithBitmap(avatar))
                .build();

        StatusBarNotification sbn = mock(StatusBarNotification.class);
        when(sbn.getNotification()).thenReturn(notification);

        AttachmentExtractor extractor = new AttachmentExtractor(RuntimeEnvironment.getApplication());
        assertTrue(extractor.extractAttachments(sbn).isEmpty());

        avatar.recycle();
    }

    @Test
    public void extractAttachments_deduplicatesPictureAndBigPicture() {
        Bitmap shared = Bitmap.createBitmap(6, 6, Bitmap.Config.ARGB_8888);
        Notification notification = new NotificationCompat.Builder(RuntimeEnvironment.getApplication(), "test")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Test")
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(shared)
                        .setBigContentTitle("Image"))
                .build();
        // BigPictureStyle ya escribe EXTRA_PICTURE; duplicar para probar deduplicación
        notification.extras.putParcelable(NotificationCompat.EXTRA_PICTURE, shared);

        StatusBarNotification sbn = mock(StatusBarNotification.class);
        when(sbn.getNotification()).thenReturn(notification);

        AttachmentExtractor extractor = new AttachmentExtractor(RuntimeEnvironment.getApplication());
        assertEquals(1, extractor.extractAttachments(sbn).size());

        shared.recycle();
    }
}
