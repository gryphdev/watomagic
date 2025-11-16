package com.parishod.watomagic.botjs;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests para BotExecutionException
 */
public class BotExecutionExceptionTest {
    
    @Test
    public void testConstructor_MessageOnly() {
        // Act
        BotExecutionException exception = new BotExecutionException("Test error");
        
        // Assert
        assertEquals("Message should match", "Test error", exception.getMessage());
        assertNull("JS error should be null", exception.getJsError());
        assertNull("JS stack trace should be null", exception.getJsStackTrace());
    }
    
    @Test
    public void testConstructor_WithCause() {
        // Arrange
        Throwable cause = new RuntimeException("Root cause");
        
        // Act
        BotExecutionException exception = new BotExecutionException("Test error", cause);
        
        // Assert
        assertEquals("Message should match", "Test error", exception.getMessage());
        assertEquals("Cause should match", cause, exception.getCause());
        assertNull("JS error should be null", exception.getJsError());
    }
    
    @Test
    public void testConstructor_WithJsError() {
        // Act
        BotExecutionException exception = new BotExecutionException(
            "Test error", 
            "ReferenceError: x is not defined",
            "at processNotification (bot.js:5:10)"
        );
        
        // Assert
        assertEquals("Message should match", "Test error", exception.getMessage());
        assertEquals("JS error should match", "ReferenceError: x is not defined", 
                     exception.getJsError());
        assertEquals("JS stack trace should match", "at processNotification (bot.js:5:10)",
                     exception.getJsStackTrace());
    }
    
    @Test
    public void testConstructor_WithJsErrorAndCause() {
        // Arrange
        Throwable cause = new RuntimeException("Root cause");
        
        // Act
        BotExecutionException exception = new BotExecutionException(
            "Test error",
            "ReferenceError: x is not defined",
            "at processNotification (bot.js:5:10)",
            cause
        );
        
        // Assert
        assertEquals("Message should match", "Test error", exception.getMessage());
        assertEquals("JS error should match", "ReferenceError: x is not defined",
                     exception.getJsError());
        assertEquals("Cause should match", cause, exception.getCause());
    }
    
    @Test
    public void testGetDetailedMessage_Simple() {
        // Arrange
        BotExecutionException exception = new BotExecutionException("Test error");
        
        // Act
        String detailed = exception.getDetailedMessage();
        
        // Assert
        assertNotNull("Detailed message should not be null", detailed);
        assertTrue("Should contain error message", detailed.contains("Test error"));
    }
    
    @Test
    public void testGetDetailedMessage_WithJsError() {
        // Arrange
        BotExecutionException exception = new BotExecutionException(
            "Test error",
            "ReferenceError: x is not defined",
            "at processNotification (bot.js:5:10)"
        );
        
        // Act
        String detailed = exception.getDetailedMessage();
        
        // Assert
        assertTrue("Should contain error message", detailed.contains("Test error"));
        assertTrue("Should contain JS error", detailed.contains("ReferenceError: x is not defined"));
        assertTrue("Should contain stack trace", detailed.contains("at processNotification"));
    }
    
    @Test
    public void testGetDetailedMessage_WithCause() {
        // Arrange
        Throwable cause = new RuntimeException("Root cause");
        BotExecutionException exception = new BotExecutionException("Test error", cause);
        
        // Act
        String detailed = exception.getDetailedMessage();
        
        // Assert
        assertTrue("Should contain error message", detailed.contains("Test error"));
        assertTrue("Should contain cause", detailed.contains("Root cause"));
    }
}
