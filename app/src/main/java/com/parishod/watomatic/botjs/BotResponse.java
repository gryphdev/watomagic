package com.parishod.watomagic.botjs;

/**
 * Modelo de respuesta del bot JavaScript.
 * Representa la acción que el bot decide tomar para una notificación.
 */
public class BotResponse {
    public enum Action {
        KEEP,      // Mantener la notificación (usar respuesta estática)
        DISMISS,   // Descartar la notificación (no responder)
        REPLY,     // Responder con el texto especificado
        SNOOZE     // Posponer la notificación
    }

    private String action;
    private String replyText;
    private Integer snoozeMinutes;
    private String reason;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReplyText() {
        return replyText;
    }

    public void setReplyText(String replyText) {
        this.replyText = replyText;
    }

    public Integer getSnoozeMinutes() {
        return snoozeMinutes;
    }

    public void setSnoozeMinutes(Integer snoozeMinutes) {
        this.snoozeMinutes = snoozeMinutes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Valida que la respuesta sea correcta según el tipo de acción
     */
    public boolean isValid() {
        if (action == null) {
            return false;
        }

        switch (action.toUpperCase()) {
            case "REPLY":
                return replyText != null && !replyText.trim().isEmpty();
            case "SNOOZE":
                return snoozeMinutes != null && snoozeMinutes > 0;
            case "KEEP":
            case "DISMISS":
                return true;
            default:
                return false;
        }
    }
}
