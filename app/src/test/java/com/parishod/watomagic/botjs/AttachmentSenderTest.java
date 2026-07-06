package com.parishod.watomagic.botjs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;

import androidx.core.app.RemoteInput;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AttachmentSenderTest {

    @Test
    public void matchMimeType_exactMatch() {
        Set<String> allowed = new HashSet<>(Collections.singletonList("image/jpeg"));
        assertEquals("image/jpeg", AttachmentSender.matchMimeType(allowed, "image/jpeg"));
    }

    @Test
    public void matchMimeType_wildcardImage() {
        Set<String> allowed = new HashSet<>(Collections.singletonList("image/*"));
        assertEquals("image/*", AttachmentSender.matchMimeType(allowed, "image/png"));
    }

    @Test
    public void matchMimeType_noMatch() {
        Set<String> allowed = new HashSet<>(Collections.singletonList("audio/*"));
        assertNull(AttachmentSender.matchMimeType(allowed, "image/jpeg"));
    }

    @Test
    public void matchMimeType_specificImageTypeInAllowedSet() {
        Set<String> allowed = new HashSet<>();
        allowed.add("image/png");
        allowed.add("image/gif");
        assertEquals("image/png", AttachmentSender.matchMimeType(allowed, "image/jpeg"));
    }

    @Test
    public void addAttachmentsToIntent_usesDataResultNotExtraStream() {
        RemoteInput remoteInput = new RemoteInput.Builder("reply_key")
                .setLabel("Reply")
                .setAllowDataType("image/*", true)
                .build();

        Intent intent = new Intent();
        Map<String, Uri> dataResults = new java.util.HashMap<>();
        Uri testUri = Uri.parse("content://com.parishod.watomagic.fileprovider/bot_attachments/test.jpg");
        dataResults.put("image/*", testUri);
        RemoteInput.addDataResultToIntent(remoteInput, intent, dataResults);

        assertNull(intent.getParcelableExtra(Intent.EXTRA_STREAM));
        Map<String, Uri> retrieved = RemoteInput.getDataResultsFromIntent(intent, "reply_key");
        assertNotNull(retrieved);
        assertEquals(testUri, retrieved.get("image/*"));
    }

    @Test
    public void remoteInput_withoutAllowedDataTypes_isEmpty() {
        RemoteInput remoteInput = new RemoteInput.Builder("reply_key")
                .setLabel("Reply")
                .build();

        assertTrue(remoteInput.getAllowedDataTypes().isEmpty());
    }
}
