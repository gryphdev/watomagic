/**
 * Bot de ejemplo para Watomagic
 * Este bot demuestra todas las capacidades disponibles
 */

async function processNotification(notification) {
    Android.log('info', `Procesando notificación de: ${notification.title}`);

    // Ejemplo 1: Descartar notificaciones de apps específicas
    if (notification.appPackage === 'com.annoying.app') {
        return {
            action: 'DISMISS',
            reason: 'App bloqueada por el usuario'
        };
    }

    // Ejemplo 2: Auto-respuesta con rate limiting
    if (notification.appPackage === 'com.whatsapp') {
        const lastReply = Android.storageGet('lastAutoReply');
        const now = Android.getCurrentTime();

        // No auto-responder más de una vez por hora
        if (!lastReply || now - parseInt(lastReply) > 3600000) {
            Android.storageSet('lastAutoReply', now.toString());

            return {
                action: 'REPLY',
                replyText: 'Estoy ocupado ahora. Te respondo pronto!'
            };
        }
    }

    // Ejemplo 3: Usar API externa para clasificación inteligente
    if (notification.title.includes('urgente') || notification.title.includes('importante')) {
        try {
            const response = await Android.httpRequest({
                url: 'https://api.example.com/classify',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer YOUR_API_KEY'
                },
                body: JSON.stringify({
                    title: notification.title,
                    body: notification.body,
                    app: notification.appPackage
                })
            });

            const result = JSON.parse(response);

            if (result.priority < 5) {
                return {
                    action: 'DISMISS',
                    reason: 'Prioridad baja según IA'
                };
            }
        } catch (error) {
            Android.log('error', `Error en API: ${error.message}`);
        }
    }

    // Ejemplo 4: Reglas basadas en horario
    const hour = new Date().getHours();

    // Durante horas de sueño (23:00 - 07:00), posponer notificaciones no críticas
    if ((hour >= 23 || hour < 7) && !notification.title.includes('alarma')) {
        return {
            action: 'SNOOZE',
            snoozeMinutes: 480, // Posponer hasta las 8 AM
            reason: 'Horario de sueño'
        };
    }

    // Ejemplo 5: Detectar spam con patrones
    const spamPatterns = [
        /ganaste/i,
        /haz clic aquí/i,
        /regalo gratis/i,
        /oferta limitada/i
    ];

    const fullText = `${notification.title} ${notification.body}`;
    for (const pattern of spamPatterns) {
        if (pattern.test(fullText)) {
            return {
                action: 'DISMISS',
                reason: 'Spam detectado'
            };
        }
    }

    // Ejemplo 6: Rastrear frecuencia de notificaciones
    const appNotifKey = `notif_count_${notification.appPackage}`;
    const count = parseInt(Android.storageGet(appNotifKey) || '0') + 1;
    Android.storageSet(appNotifKey, count.toString());

    if (count > 10) {
        const appName = Android.getAppName(notification.appPackage);
        Android.log('warn', `${appName} envió ${count} notificaciones`);

        return {
            action: 'DISMISS',
            reason: 'Demasiadas notificaciones de esta app'
        };
    }

    // Por defecto: mantener notificación
    return {
        action: 'KEEP'
    };
}
