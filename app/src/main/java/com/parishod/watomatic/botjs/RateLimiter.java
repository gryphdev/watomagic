package com.parishod.watomagic.botjs;

import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Rate limiter para controlar la frecuencia de ejecuciones de bots
 * Implementa un algoritmo de ventana deslizante
 */
public class RateLimiter {
    private static final String TAG = "RateLimiter";
    
    private final int maxExecutions;
    private final long windowMs;
    private final Queue<Long> executionTimes;
    
    /**
     * Crea un nuevo rate limiter
     * @param maxExecutions Número máximo de ejecuciones permitidas
     * @param windowMs Ventana de tiempo en milisegundos
     */
    public RateLimiter(int maxExecutions, long windowMs) {
        this.maxExecutions = maxExecutions;
        this.windowMs = windowMs;
        this.executionTimes = new LinkedList<>();
    }
    
    /**
     * Intenta adquirir un permiso para ejecutar
     * @return true si se puede ejecutar, false si se excedió el límite
     */
    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        
        // Limpiar ejecuciones antiguas fuera de la ventana
        while (!executionTimes.isEmpty() && 
               executionTimes.peek() < now - windowMs) {
            executionTimes.poll();
        }
        
        // Verificar si se puede ejecutar
        if (executionTimes.size() >= maxExecutions) {
            Log.w(TAG, "Rate limit exceeded: " + executionTimes.size() + 
                  " executions in last " + windowMs + "ms");
            return false;
        }
        
        // Registrar esta ejecución
        executionTimes.offer(now);
        return true;
    }
    
    /**
     * Obtiene el número de ejecuciones en la ventana actual
     * @return número de ejecuciones
     */
    public synchronized int getCurrentCount() {
        long now = System.currentTimeMillis();
        
        // Limpiar ejecuciones antiguas
        while (!executionTimes.isEmpty() && 
               executionTimes.peek() < now - windowMs) {
            executionTimes.poll();
        }
        
        return executionTimes.size();
    }
    
    /**
     * Limpia todas las ejecuciones registradas
     */
    public synchronized void reset() {
        executionTimes.clear();
    }
}
