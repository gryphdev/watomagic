package com.parishod.watomagic.botjs;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Captura logs de ejecución de bots para mostrar en la UI.
 *
 * Esta clase mantiene un buffer en memoria de logs generados durante
 * la ejecución de bots. Se usa solo cuando el modo debug está habilitado.
 *
 * Thread-safe: Usa synchronization para acceso concurrente.
 */
public class BotLogCapture {

    private static final int MAX_LOGS = 500;
    private static final List<LogEntry> logs = Collections.synchronizedList(new ArrayList<>());
    private static boolean enabled = false;

    /**
     * Entrada de log individual con timestamp, nivel y mensaje.
     */
    public static class LogEntry {
        public final long timestamp;
        public final String level;
        public final String message;

        public LogEntry(long timestamp, @NonNull String level, @NonNull String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.message = message;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("[%s] %s", level.toUpperCase(), message);
        }
    }

    /**
     * Habilita o deshabilita la captura de logs.
     * Cuando se deshabilita, se limpian todos los logs existentes.
     */
    public static void setEnabled(boolean enabled) {
        BotLogCapture.enabled = enabled;
        if (!enabled) {
            clear();
        }
    }

    /**
     * @return true si la captura de logs está habilitada
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Agrega un log al buffer.
     * Solo se captura si el modo debug está habilitado.
     *
     * Si el buffer está lleno, se elimina el log más antiguo.
     */
    public static void addLog(@NonNull String level, @NonNull String message) {
        if (!enabled) {
            return;
        }

        synchronized (logs) {
            // Si alcanzamos el límite, eliminar el más antiguo
            if (logs.size() >= MAX_LOGS) {
                logs.remove(0);
            }
            logs.add(new LogEntry(System.currentTimeMillis(), level, message));
        }
    }

    /**
     * Obtiene una copia de todos los logs capturados.
     * @return Lista de logs (copia defensiva, thread-safe)
     */
    @NonNull
    public static List<LogEntry> getLogs() {
        synchronized (logs) {
            return new ArrayList<>(logs);
        }
    }

    /**
     * Limpia todos los logs capturados.
     */
    public static void clear() {
        synchronized (logs) {
            logs.clear();
        }
    }

    /**
     * @return Cantidad de logs actualmente en el buffer
     */
    public static int getLogCount() {
        synchronized (logs) {
            return logs.size();
        }
    }
}
