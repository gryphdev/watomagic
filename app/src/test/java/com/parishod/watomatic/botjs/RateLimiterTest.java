package com.parishod.watomagic.botjs;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests para RateLimiter
 */
public class RateLimiterTest {
    
    private RateLimiter rateLimiter;
    private static final int MAX_EXECUTIONS = 5;
    private static final long WINDOW_MS = 60000; // 1 minuto
    
    @Before
    public void setUp() {
        rateLimiter = new RateLimiter(MAX_EXECUTIONS, WINDOW_MS);
    }
    
    @Test
    public void testTryAcquire_WithinLimit() {
        // Act & Assert
        for (int i = 0; i < MAX_EXECUTIONS; i++) {
            assertTrue("Should allow execution " + (i + 1), rateLimiter.tryAcquire());
        }
    }
    
    @Test
    public void testTryAcquire_ExceedsLimit() {
        // Arrange - Llenar el límite
        for (int i = 0; i < MAX_EXECUTIONS; i++) {
            rateLimiter.tryAcquire();
        }
        
        // Act
        boolean result = rateLimiter.tryAcquire();
        
        // Assert
        assertFalse("Should reject execution when limit exceeded", result);
    }
    
    @Test
    public void testGetCurrentCount_Empty() {
        // Act
        int count = rateLimiter.getCurrentCount();
        
        // Assert
        assertEquals("Initial count should be 0", 0, count);
    }
    
    @Test
    public void testGetCurrentCount_AfterAcquires() {
        // Arrange
        rateLimiter.tryAcquire();
        rateLimiter.tryAcquire();
        rateLimiter.tryAcquire();
        
        // Act
        int count = rateLimiter.getCurrentCount();
        
        // Assert
        assertEquals("Count should match number of acquires", 3, count);
    }
    
    @Test
    public void testReset() {
        // Arrange
        rateLimiter.tryAcquire();
        rateLimiter.tryAcquire();
        
        // Act
        rateLimiter.reset();
        
        // Assert
        assertEquals("Count should be 0 after reset", 0, rateLimiter.getCurrentCount());
        assertTrue("Should allow acquire after reset", rateLimiter.tryAcquire());
    }
    
    @Test
    public void testWindowExpiration() throws InterruptedException {
        // Arrange - Crear un rate limiter con ventana muy corta
        RateLimiter shortWindowLimiter = new RateLimiter(2, 100); // 2 ejecuciones en 100ms
        
        // Act - Llenar el límite
        assertTrue(shortWindowLimiter.tryAcquire());
        assertTrue(shortWindowLimiter.tryAcquire());
        assertFalse("Should reject third execution", shortWindowLimiter.tryAcquire());
        
        // Esperar a que expire la ventana
        Thread.sleep(150);
        
        // Assert - Ahora debería permitir más ejecuciones
        assertTrue("Should allow execution after window expires", 
                   shortWindowLimiter.tryAcquire());
    }
    
    @Test
    public void testConcurrentAccess() throws InterruptedException {
        // Arrange
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        final boolean[] results = new boolean[numThreads];
        
        // Act - Múltiples threads intentando adquirir
        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = rateLimiter.tryAcquire();
            });
            threads[i].start();
        }
        
        // Esperar a que todos terminen
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Assert - Solo MAX_EXECUTIONS deberían haber sido exitosas
        int successCount = 0;
        for (boolean result : results) {
            if (result) successCount++;
        }
        
        assertEquals("Only " + MAX_EXECUTIONS + " should succeed", 
                    MAX_EXECUTIONS, successCount);
    }
}
