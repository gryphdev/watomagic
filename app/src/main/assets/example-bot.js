/**
 * Bot de ejemplo para Watomagic
 * Este bot demuestra varias capacidades disponibles
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

    // Ejemplo 2: Auto-respuesta con rate limiting de 1 hora
    // Puedes usar localStorage (API estándar) o Android.storage* (ambos funcionan igual)
    if (notification.appPackage === 'com.whatsapp') {
        // Opción A: Usando localStorage (sintaxis estándar del navegador)
        const lastReply = localStorage.getItem('lastAutoReply');
        const now = Android.getCurrentTime();

        if (!lastReply || now - parseInt(lastReply, 10) > 3600000) {
            localStorage.setItem('lastAutoReply', now.toString());

            return {
                action: 'REPLY',
                replyText: 'Estoy ocupado ahora. Te respondo pronto!'
            };
        }
        
        // Opción B: También puedes usar Android.storage* directamente
        // const lastReply = Android.storageGet('lastAutoReply');
        // Android.storageSet('lastAutoReply', now.toString());
    }

    // Ejemplo 3: Usar API externa para clasificación inteligente
    if (notification.title && notification.title.toLowerCase().includes('urgente')) {
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
    if ((hour >= 23 || hour < 7) && (!notification.title || !notification.title.includes('alarma'))) {
        return {
            action: 'SNOOZE',
            snoozeMinutes: 480,
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

    const fullText = `${notification.title ?? ''} ${notification.body ?? ''}`;
    for (const pattern of spamPatterns) {
        if (pattern.test(fullText)) {
            return {
                action: 'DISMISS',
                reason: 'Spam detectado'
            };
        }
    }

    // Ejemplo 6: Rastrear frecuencia de notificaciones por app
    const appNotifKey = 'notif_count_' + notification.appPackage;
    const count = parseInt(localStorage.getItem(appNotifKey) || '0', 10) + 1;
    localStorage.setItem(appNotifKey, count.toString());

    if (count > 10) {
        const appName = Android.getAppName(notification.appPackage);
        Android.log('warn', `${appName} envió ${count} notificaciones`);

        return {
            action: 'DISMISS',
            reason: 'Demasiadas notificaciones de esta app'
        };
    }

    // Ejemplo 7: Procesar imágenes recibidas
    if (notification.attachments && notification.attachments.length > 0) {
        Android.log('info', `Notificación contiene ${notification.attachments.length} imagen(es)`);
        
        const firstAttachment = notification.attachments[0];
        
        // Obtener thumbnail si está disponible
        if (firstAttachment.thumbnailBase64) {
            Android.log('info', 'Thumbnail disponible para preview');
        }
        
        // Leer imagen completa como base64 (máx 5MB)
        if (firstAttachment.hasFile) {
            try {
                const imageBase64 = Android.readAttachmentAsBase64(firstAttachment.id);
                if (imageBase64) {
                    Android.log('info', `Imagen leída: ${imageBase64.length} caracteres base64`);
                    
                    // Aquí podrías enviar la imagen a una API externa para análisis
                    // const analysis = await Android.httpRequest({
                    //     url: 'https://api.example.com/analyze-image',
                    //     method: 'POST',
                    //     headers: { 'Content-Type': 'application/json' },
                    //     body: JSON.stringify({ image: imageBase64 })
                    // });
                    
                    return {
                        action: 'REPLY',
                        replyText: 'Gracias por la imagen! La he recibido correctamente.'
                    };
                }
            } catch (error) {
                Android.log('error', `Error leyendo imagen: ${error.message}`);
            }
        }
    }

    // Por defecto: mantener notificación
    return {
        action: 'KEEP'
    };
}
