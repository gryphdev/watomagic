package com.parishod.watomagic.replyproviders;

import android.content.Context;

/**
 * Interfaz base para todos los proveedores de respuestas automáticas.
 * Implementa el patrón Strategy para permitir diferentes estrategias de generación de respuestas.
 */
public interface ReplyProvider {
    /**
     * Genera una respuesta para una notificación entrante
     * @param context Contexto de Android
     * @param incomingMessage Mensaje recibido en la notificación
     * @param notificationData Datos completos de la notificación
     * @param callback Callback para devolver la respuesta o error
     */
    void generateReply(Context context,
                      String incomingMessage,
                      NotificationData notificationData,
                      ReplyCallback callback);

    /**
     * Callback para manejar el resultado de la generación de respuesta
     */
    interface ReplyCallback {
        /**
         * Se llama cuando la respuesta se genera exitosamente
         * @param reply Texto de la respuesta generada
         */
        void onSuccess(String reply);

        /**
         * Se llama cuando falla la generación de respuesta
         * @param error Mensaje de error o código de acción especial (DISMISS, KEEP, SNOOZE)
         */
        void onFailure(String error);
    }
}
