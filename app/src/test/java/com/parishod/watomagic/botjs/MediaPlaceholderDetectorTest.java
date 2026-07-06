package com.parishod.watomagic.botjs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MediaPlaceholderDetectorTest {

    @Test
    public void isMediaPlaceholder_directPhoto() {
        assertTrue(MediaPlaceholderDetector.isMediaPlaceholder(
                "com.whatsapp", "📷 Foto", false));
    }

    @Test
    public void isMediaPlaceholder_groupMessage() {
        assertTrue(MediaPlaceholderDetector.isMediaPlaceholder(
                "com.whatsapp", "Juan: 📷 Foto", false));
    }

    @Test
    public void isMediaPlaceholder_emojiOnly() {
        assertTrue(MediaPlaceholderDetector.isMediaPlaceholder(
                "com.whatsapp", "📷", false));
    }

    @Test
    public void isMediaPlaceholder_rejectsMidSentenceEmoji() {
        assertFalse(MediaPlaceholderDetector.isMediaPlaceholder(
                "com.whatsapp", "Me encanta 📷", false));
    }

    @Test
    public void isMediaPlaceholder_rejectsWhenAttachmentsPresent() {
        assertFalse(MediaPlaceholderDetector.isMediaPlaceholder(
                "com.whatsapp", "📷 Foto", true));
    }

    @Test
    public void isMediaPlaceholder_rejectsNonWhatsApp() {
        assertFalse(MediaPlaceholderDetector.isMediaPlaceholder(
                "com.telegram.messenger", "📷 Foto", false));
    }

    @Test
    public void extractMediaText_groupUsesSuffix() {
        assertEquals("📷 Foto",
                MediaPlaceholderDetector.extractMediaText("Grupo: Juan: 📷 Foto"));
    }
}
