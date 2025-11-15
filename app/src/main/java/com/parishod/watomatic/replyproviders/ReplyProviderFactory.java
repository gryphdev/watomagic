package com.parishod.watomagic.replyproviders;

import com.parishod.watomagic.model.preferences.PreferencesManager;

/**
 * Factory para crear instancias de ReplyProvider según las preferencias del usuario.
 * Implementa el patrón Factory para seleccionar la estrategia de respuesta adecuada.
 */
public class ReplyProviderFactory {
    /**
     * Obtiene el provider de respuestas apropiado según las preferencias.
     * Prioridad: BotJS > OpenAI > Static
     * 
     * @param prefs PreferencesManager con las preferencias del usuario
     * @return Instancia de ReplyProvider apropiada
     */
    public static ReplyProvider getProvider(PreferencesManager prefs) {
        // Prioridad 1: BotJS (si está habilitado y tiene URL configurada)
        if (prefs.isBotJsEnabled() && prefs.getBotJsUrl() != null && !prefs.getBotJsUrl().trim().isEmpty()) {
            return new BotJsReplyProvider();
        }
        
        // Prioridad 2: OpenAI (si está habilitado)
        if (prefs.isOpenAIRepliesEnabled()) {
            return new OpenAIReplyProvider();
        }
        
        // Prioridad 3: Static (por defecto)
        return new StaticReplyProvider();
    }
}
