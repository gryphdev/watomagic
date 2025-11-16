package com.parishod.watomagic.botjs;

/**
 * Excepción específica para errores en la ejecución de bots JavaScript
 */
public class BotExecutionException extends Exception {
    private final String jsError;
    private final String jsStackTrace;
    
    public BotExecutionException(String message) {
        super(message);
        this.jsError = null;
        this.jsStackTrace = null;
    }
    
    public BotExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.jsError = null;
        this.jsStackTrace = null;
    }
    
    public BotExecutionException(String message, String jsError, String jsStackTrace) {
        super(message);
        this.jsError = jsError;
        this.jsStackTrace = jsStackTrace;
    }
    
    public BotExecutionException(String message, String jsError, String jsStackTrace, Throwable cause) {
        super(message, cause);
        this.jsError = jsError;
        this.jsStackTrace = jsStackTrace;
    }
    
    /**
     * Obtiene el error de JavaScript si está disponible
     * @return Error de JavaScript o null
     */
    public String getJsError() {
        return jsError;
    }
    
    /**
     * Obtiene el stack trace de JavaScript si está disponible
     * @return Stack trace de JavaScript o null
     */
    public String getJsStackTrace() {
        return jsStackTrace;
    }
    
    /**
     * Obtiene un mensaje detallado con toda la información disponible
     * @return Mensaje completo con error y stack trace
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Bot Error: ").append(getMessage());
        
        if (jsError != null) {
            sb.append("\nJS Error: ").append(jsError);
        }
        
        if (jsStackTrace != null) {
            sb.append("\nStack Trace: ").append(jsStackTrace);
        }
        
        if (getCause() != null) {
            sb.append("\nCause: ").append(getCause().getMessage());
        }
        
        return sb.toString();
    }
}
