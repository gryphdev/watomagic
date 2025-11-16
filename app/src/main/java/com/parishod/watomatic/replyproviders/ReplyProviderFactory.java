package com.parishod.watomagic.replyproviders;

import com.parishod.watomagic.model.preferences.PreferencesManager;

/**
 * Factory para crear instancias de ReplyProvider según la configuración del usuario
 * Implementa el patrón Strategy para seleccionar el provider apropiado
 */
public class ReplyProviderFactory {
    /**
     * Obtiene el provider de respuestas apropiado según la configuración
     * Prioridad: BotJS > OpenAI > Static
     * 
     * @param prefs PreferencesManager con la configuración del usuario
     * @return Instancia del ReplyProvider apropiado
     */
    public static ReplyProvider getProvider(PreferencesManager prefs) {
        // Prioridad 1: BotJS (si está habilitado y tiene script)
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
