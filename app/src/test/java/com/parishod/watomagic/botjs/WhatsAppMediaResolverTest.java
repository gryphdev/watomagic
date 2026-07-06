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

    @Test
    public void isCloserMatch_prefersSmallerDistance() {
        long notificationTimestamp = 10_000L;
        long bestModified = 9_600L;
        long bestDistance = Math.abs(bestModified - notificationTimestamp);

        assertTrue(WhatsAppMediaResolver.isCloserMatch(
                9_950L, notificationTimestamp, bestModified, bestDistance));
        assertFalse(WhatsAppMediaResolver.isCloserMatch(
                10_200L, notificationTimestamp, 9_950L,
                Math.abs(9_950L - notificationTimestamp)));
    }

    @Test
    public void isCloserMatch_tieBreaksByLatestModified() {
        long notificationTimestamp = 10_000L;
        long distance = 100L;
        assertTrue(WhatsAppMediaResolver.isCloserMatch(
                10_100L, notificationTimestamp, 9_900L, distance));
        assertFalse(WhatsAppMediaResolver.isCloserMatch(
                9_900L, notificationTimestamp, 10_100L, distance));
    }

    @Test
    public void isCloserMatch_rejectsWhenDistanceIsWorse() {
        long notificationTimestamp = 10_000L;
        long bestModified = 9_950L;
        long bestDistance = Math.abs(bestModified - notificationTimestamp);

        assertFalse(WhatsAppMediaResolver.isCloserMatch(
                9_600L, notificationTimestamp, bestModified, bestDistance));
    }
}
