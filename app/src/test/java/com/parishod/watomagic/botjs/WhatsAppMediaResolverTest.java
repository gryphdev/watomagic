package com.parishod.watomagic.botjs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WhatsAppMediaResolverTest {

    @Test
    public void isImageFile_recognizesCommonExtensions() {
        assertTrue(WhatsAppMediaResolver.isImageFile("IMG-20250101-WA0001.jpg"));
        assertTrue(WhatsAppMediaResolver.isImageFile("photo.JPEG"));
        assertTrue(WhatsAppMediaResolver.isImageFile("image.webp"));
    }

    @Test
    public void isImageFile_rejectsNonImages() {
        assertFalse(WhatsAppMediaResolver.isImageFile("document.pdf"));
        assertFalse(WhatsAppMediaResolver.isImageFile(null));
        assertFalse(WhatsAppMediaResolver.isImageFile("notes.txt"));
    }
}
