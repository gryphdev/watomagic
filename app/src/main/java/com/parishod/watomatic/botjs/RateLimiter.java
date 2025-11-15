package com.parishod.watomagic.botjs;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Rate limiter para controlar la frecuencia de ejecución de bots.
 * Previene abusos y sobrecarga del sistema.
 */
public class RateLimiter {
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
            return false;
        }

        // Registrar esta ejecución
        executionTimes.offer(now);
        return true;
    }

    /**
     * Obtiene el número de ejecuciones en la ventana actual
     */
    public synchronized int getCurrentCount() {
        long now = System.currentTimeMillis();
        while (!executionTimes.isEmpty() && 
               executionTimes.peek() < now - windowMs) {
            executionTimes.poll();
        }
        return executionTimes.size();
    }
}
