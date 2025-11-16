package com.parishod.watomagic.botjs;

/**
 * Excepción específica para fallos ocurridos durante la ejecución del bot JavaScript.
 */
public class BotExecutionException extends Exception {
    private final String jsError;
    private final String jsStackTrace;

    public BotExecutionException(String message, String jsError, String jsStackTrace) {
        super(message);
        this.jsError = jsError;
        this.jsStackTrace = jsStackTrace;
    }

    public String getJsError() {
        return jsError;
    }

    public String getJsStackTrace() {
        return jsStackTrace;
    }

    public String getDetailedMessage() {
        return "Bot Error: " + getMessage()
                + "\nJS Error: " + jsError
                + "\nStack Trace: " + jsStackTrace;
    }
}
