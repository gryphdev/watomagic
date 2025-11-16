package com.parishod.watomagic.botjs;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests para BotRepository
 * Nota: Estos tests requieren un contexto Android, por lo que usan Robolectric
 */
@RunWith(RobolectricTestRunner.class)
public class BotRepositoryTest {
    
    private BotRepository repository;
    private Context context;
    
    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        repository = new BotRepository(context);
    }
    
    @Test
    public void testGetInstalledBotInfo_NoBotInstalled() {
        // Act
        BotRepository.BotInfo info = repository.getInstalledBotInfo();
        
        // Assert
        assertNull("Should return null when no bot is installed", info);
    }
    
    @Test
    public void testDeleteBot_NoBotInstalled() {
        // Act - No debería lanzar excepción
        repository.deleteBot();
        
        // Assert
        BotRepository.BotInfo info = repository.getInstalledBotInfo();
        assertNull("Should still return null after delete", info);
    }
    
    @Test
    public void testCheckForUpdates_NoBotInstalled() {
        // Act
        boolean hasUpdates = repository.checkForUpdates();
        
        // Assert
        assertFalse("Should return false when no bot is installed", hasUpdates);
    }
    
    @Test
    public void testDownloadBot_InvalidUrl_Http() {
        // Act
        BotRepository.Result<BotRepository.BotInfo> result = 
            repository.downloadBot("http://example.com/bot.js");
        
        // Assert
        assertFalse("Should fail for HTTP URL", result.isSuccess());
        assertTrue("Error should mention HTTPS", 
                   result.getError().toLowerCase().contains("https"));
    }
    
    @Test
    public void testDownloadBot_InvalidUrl_Empty() {
        // Act
        BotRepository.Result<BotRepository.BotInfo> result = 
            repository.downloadBot("");
        
        // Assert
        assertFalse("Should fail for empty URL", result.isSuccess());
    }
    
    @Test
    public void testDownloadBot_InvalidUrl_Null() {
        // Act
        BotRepository.Result<BotRepository.BotInfo> result = 
            repository.downloadBot(null);
        
        // Assert
        assertFalse("Should fail for null URL", result.isSuccess());
    }
    
    @Test
    public void testResult_Success() {
        // Arrange
        BotRepository.BotInfo info = new BotRepository.BotInfo("https://example.com/bot.js", 1234567890L);
        
        // Act
        BotRepository.Result<BotRepository.BotInfo> result = 
            BotRepository.Result.success(info);
        
        // Assert
        assertTrue("Should be successful", result.isSuccess());
        assertNotNull("Data should not be null", result.getData());
        assertEquals("URL should match", "https://example.com/bot.js", result.getData().url);
        assertNull("Error should be null", result.getError());
    }
    
    @Test
    public void testResult_Error() {
        // Act
        BotRepository.Result<BotRepository.BotInfo> result = 
            BotRepository.Result.error("Test error");
        
        // Assert
        assertFalse("Should not be successful", result.isSuccess());
        assertNull("Data should be null", result.getData());
        assertEquals("Error should match", "Test error", result.getError());
    }
    
    @Test
    public void testBotInfo_Constructor() {
        // Act
        BotRepository.BotInfo info = new BotRepository.BotInfo(
            "https://example.com/bot.js", 
            1234567890L
        );
        
        // Assert
        assertEquals("URL should match", "https://example.com/bot.js", info.url);
        assertEquals("Timestamp should match", 1234567890L, info.timestamp);
    }
}
