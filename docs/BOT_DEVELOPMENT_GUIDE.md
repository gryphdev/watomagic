# 🛠️ Guía de Desarrollo de Bots para Watomagic

Esta guía te enseñará cómo crear bots JavaScript personalizados para Watomagic.

---

## 📖 Introducción

Los bots de Watomagic son scripts JavaScript que se ejecutan localmente en dispositivos Android usando QuickJS. Cada bot debe implementar una función `processNotification` que recibe datos de la notificación y retorna una acción.

### Características Principales

- ✅ **Ejecución local**: Los bots se ejecutan en tu dispositivo, no en servidores externos
- ✅ **APIs disponibles**: Acceso a storage, HTTP, logging y utilidades
- ✅ **TypeScript support**: Interfaces TypeScript disponibles para autocompletado
- ✅ **Seguro**: Sandbox con validaciones y timeouts automáticos

---

## 🚀 Estructura Básica

Todo bot debe implementar la función `processNotification`:

```javascript
async function processNotification(notification) {
    // Tu lógica aquí
    return {
        action: 'KEEP' // o 'DISMISS', 'REPLY', 'SNOOZE'
    };
}
```

### Tipos de Datos

```typescript
interface NotificationData {
    id: number;
    appPackage: string;      // Ej: 'com.whatsapp'
    title: string;            // Título de la notificación
    body: string;             // Contenido del mensaje
    timestamp: number;        // Timestamp en milisegundos
    isGroup: boolean;         // true si es un grupo
    actions: string[];        // Acciones disponibles
}

interface BotResponse {
    action: 'KEEP' | 'DISMISS' | 'REPLY' | 'SNOOZE';
    replyText?: string;        // Requerido si action = 'REPLY'
    snoozeMinutes?: number;    // Requerido si action = 'SNOOZE'
    reason?: string;           // Opcional: para logging/debugging
}
```

---

## 📚 APIs Disponibles

### Android.log()

Registra mensajes en los logs de la aplicación.

```javascript
Android.log('debug', 'Mensaje de depuración');
Android.log('info', 'Información general');
Android.log('warn', 'Advertencia');
Android.log('error', 'Error crítico');
```

**Niveles disponibles**: `'debug'`, `'info'`, `'warn'`, `'error'`

### Android.storageGet(key)

Obtiene un valor almacenado previamente.

```javascript
const lastReply = Android.storageGet('lastAutoReply');
// Retorna: string | null
```

### Android.storageSet(key, value)

Almacena un valor para uso futuro.

```javascript
Android.storageSet('contador', '5');
Android.storageSet('lastReply', Date.now().toString());
```

**Nota**: Los valores se almacenan como strings. Usa `JSON.stringify()` para objetos.

### Android.storageRemove(key)

Elimina un valor almacenado.

```javascript
Android.storageRemove('contador');
```

### Android.storageKeys()

Obtiene todas las claves almacenadas.

```javascript
const keys = Android.storageKeys();
// Retorna: string[]
```

### Android.httpRequest(options)

Realiza una petición HTTP a una API externa.

```javascript
const response = await Android.httpRequest({
    url: 'https://api.example.com/data',
    method: 'POST',  // 'GET', 'POST', 'PUT', 'DELETE'
    headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer YOUR_API_KEY'
    },
    body: JSON.stringify({ key: 'value' })
});

const data = JSON.parse(response);
```

**Parámetros**:
- `url` (string, requerido): URL HTTPS
- `method` (string, opcional): 'GET', 'POST', 'PUT', 'DELETE' (default: 'GET')
- `headers` (object, opcional): Headers HTTP
- `body` (string, opcional): Cuerpo de la petición

**Retorna**: Promise<string> con la respuesta HTTP

**Importante**: Solo se permiten URLs HTTPS.

### Android.getCurrentTime()

Obtiene el timestamp actual en milisegundos.

```javascript
const now = Android.getCurrentTime();
// Retorna: number (milisegundos desde epoch)
```

### Android.getAppName(packageName)

Obtiene el nombre legible de una app.

```javascript
const appName = Android.getAppName('com.whatsapp');
// Retorna: 'WhatsApp'
```

---

## 💡 Ejemplos Comunes

### 1. Auto-respuesta Simple

```javascript
async function processNotification(notification) {
    if (notification.appPackage === 'com.whatsapp') {
        return {
            action: 'REPLY',
            replyText: 'Estoy ocupado, te respondo luego.'
        };
    }
    return { action: 'KEEP' };
}
```

### 2. Bloquear Apps Específicas

```javascript
async function processNotification(notification) {
    const blockedApps = ['com.spam.app', 'com.annoying.app'];
    
    if (blockedApps.includes(notification.appPackage)) {
        return {
            action: 'DISMISS',
            reason: 'App bloqueada por el usuario'
        };
    }
    
    return { action: 'KEEP' };
}
```

### 3. Rate Limiting (Una respuesta por hora)

```javascript
async function processNotification(notification) {
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
    
    return { action: 'KEEP' };
}
```

### 4. Detección de Spam con Patrones

```javascript
async function processNotification(notification) {
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
    
    return { action: 'KEEP' };
}
```

### 5. Reglas Basadas en Horario

```javascript
async function processNotification(notification) {
    const hour = new Date().getHours();
    
    // Durante horas de sueño (23:00 - 07:00), posponer notificaciones no críticas
    if ((hour >= 23 || hour < 7) && !notification.title.includes('alarma')) {
        return {
            action: 'SNOOZE',
            snoozeMinutes: 480, // Posponer hasta las 8 AM
            reason: 'Horario de sueño'
        };
    }
    
    return { action: 'KEEP' };
}
```

### 6. Usar API Externa para Clasificación

```javascript
async function processNotification(notification) {
    // Solo procesar notificaciones importantes
    if (notification.title.includes('urgente') || 
        notification.title.includes('importante')) {
        
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
            // Fallback: mantener notificación
        }
    }
    
    return { action: 'KEEP' };
}
```

### 7. Integración con OpenAI

```javascript
async function processNotification(notification) {
    if (notification.appPackage === 'com.whatsapp') {
        try {
            const response = await Android.httpRequest({
                url: 'https://api.openai.com/v1/chat/completions',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer YOUR_OPENAI_API_KEY'
                },
                body: JSON.stringify({
                    model: 'gpt-3.5-turbo',
                    messages: [
                        {
                            role: 'system',
                            content: 'Eres un asistente que genera respuestas cortas y amigables para mensajes de WhatsApp.'
                        },
                        {
                            role: 'user',
                            content: notification.body
                        }
                    ],
                    max_tokens: 100
                })
            });
            
            const result = JSON.parse(response);
            const aiReply = result.choices[0].message.content;
            
            return {
                action: 'REPLY',
                replyText: aiReply
            };
        } catch (error) {
            Android.log('error', `Error en OpenAI: ${error.message}`);
            // Fallback: respuesta estática
            return {
                action: 'REPLY',
                replyText: 'Gracias por tu mensaje. Te responderé pronto.'
            };
        }
    }
    
    return { action: 'KEEP' };
}
```

### 8. Rastrear Frecuencia de Notificaciones

```javascript
async function processNotification(notification) {
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
    
    return { action: 'KEEP' };
}
```

### 9. Bot Completo con Múltiples Reglas

```javascript
async function processNotification(notification) {
    Android.log('info', `Procesando notificación de: ${notification.title}`);
    
    // Regla 1: Bloquear apps específicas
    const blockedApps = ['com.spam.app'];
    if (blockedApps.includes(notification.appPackage)) {
        return { action: 'DISMISS', reason: 'App bloqueada' };
    }
    
    // Regla 2: Detectar spam
    const spamPatterns = [/ganaste/i, /regalo gratis/i];
    const fullText = `${notification.title} ${notification.body}`;
    for (const pattern of spamPatterns) {
        if (pattern.test(fullText)) {
            return { action: 'DISMISS', reason: 'Spam detectado' };
        }
    }
    
    // Regla 3: Auto-respuesta con rate limiting (WhatsApp)
    if (notification.appPackage === 'com.whatsapp') {
        const lastReply = Android.storageGet('lastAutoReply');
        const now = Android.getCurrentTime();
        
        if (!lastReply || now - parseInt(lastReply) > 3600000) {
            Android.storageSet('lastAutoReply', now.toString());
            return {
                action: 'REPLY',
                replyText: 'Estoy ocupado ahora. Te respondo pronto!'
            };
        }
    }
    
    // Regla 4: Horario de sueño
    const hour = new Date().getHours();
    if ((hour >= 23 || hour < 7) && !notification.title.includes('alarma')) {
        return {
            action: 'SNOOZE',
            snoozeMinutes: 480,
            reason: 'Horario de sueño'
        };
    }
    
    // Por defecto: mantener notificación
    return { action: 'KEEP' };
}
```

---

## ⚠️ Limitaciones y Restricciones

### Limitaciones Técnicas

- **Timeout**: 5 segundos por ejecución (el bot se cancela automáticamente)
- **Tamaño máximo**: 100KB por bot
- **Rate limiting**: Máximo 100 ejecuciones por minuto
- **Solo HTTPS**: Las llamadas HTTP deben ser HTTPS

### Restricciones de Seguridad

Los siguientes patrones están **bloqueados** y causarán que el bot sea rechazado:

- ❌ `eval()`
- ❌ `Function()`
- ❌ `constructor[]`
- ❌ `__proto__`
- ❌ `import()`

### Qué NO pueden hacer los bots

- ❌ Acceder al sistema de archivos de Android
- ❌ Leer contactos o datos de otras apps
- ❌ Modificar configuraciones del sistema
- ❌ Ejecutar código peligroso

---

## ✅ Mejores Prácticas

### 1. Manejo de Errores

Siempre usa try/catch para APIs externas:

```javascript
async function processNotification(notification) {
    try {
        const response = await Android.httpRequest({
            url: 'https://api.example.com/data',
            method: 'GET'
        });
        // Procesar respuesta
    } catch (error) {
        Android.log('error', `Error: ${error.message}`);
        // Fallback seguro
        return { action: 'KEEP' };
    }
}
```

### 2. Optimización de Performance

- **Caché resultados**: Usa `Android.storageSet()` para evitar llamadas repetidas
- **Timeouts cortos**: Las APIs externas deben responder rápido
- **Lógica eficiente**: Evita bucles largos o procesamiento pesado

### 3. Logging para Debugging

Usa `Android.log()` estratégicamente:

```javascript
Android.log('info', `Procesando: ${notification.appPackage}`);
Android.log('debug', `Storage keys: ${Android.storageKeys().join(', ')}`);
```

### 4. Validación de Datos

Valida los datos antes de usarlos:

```javascript
async function processNotification(notification) {
    if (!notification || !notification.appPackage) {
        Android.log('error', 'Notificación inválida');
        return { action: 'KEEP' };
    }
    // ...
}
```

### 5. Documentación en el Código

Comenta tu código para facilitar el mantenimiento:

```javascript
/**
 * Bot de auto-respuesta con rate limiting
 * Responde máximo una vez por hora por app
 */
async function processNotification(notification) {
    // ...
}
```

---

## 🧪 Testing

### Testing Local

1. Escribe tu bot en un archivo `.js`
2. Sube el archivo a un servidor HTTPS
3. Configura la URL en Watomagic
4. Usa el botón **"Probar Bot"** para ejecutar una notificación de prueba
5. Revisa los logs para ver el resultado

### Testing con Logs

```javascript
async function processNotification(notification) {
    Android.log('info', `Testing bot with: ${notification.title}`);
    
    // Tu lógica aquí
    
    Android.log('info', `Bot decision: ${result.action}`);
    return result;
}
```

---

## 📦 Deployment

### 1. Subir el Bot

Sube tu archivo `bot.js` a un servidor HTTPS accesible públicamente.

**Recomendaciones**:
- ✅ Usa GitHub Gist o GitHub Pages
- ✅ Usa un servidor web estático (Netlify, Vercel, etc.)
- ✅ Asegúrate de que la URL sea HTTPS
- ✅ Verifica que el archivo sea accesible sin autenticación

### 2. Configurar en Watomagic

1. Copia la URL HTTPS de tu bot
2. Pégala en la configuración de bots
3. Descarga el bot
4. Activa el bot

### 3. Auto-actualización

Si habilitas auto-actualización, el bot se actualizará automáticamente cada 6 horas desde la URL configurada.

---

## 📚 Recursos Adicionales

- **[Referencia de API](./BOT_API_REFERENCE.md)** - Documentación completa de todas las APIs
- **[Guía para Usuarios](./BOT_USER_GUIDE.md)** - Cómo usar bots en Watomagic
- **[Plan de Implementación](./PLAN_BOTJS_SYSTEM.md)** - Detalles técnicos del sistema

---

## 🐛 Solución de Problemas

### El bot no se valida

- ✅ Verifica que tenga la función `processNotification`
- ✅ Verifica que no use patrones bloqueados (eval, Function, etc.)
- ✅ Verifica que el tamaño sea menor a 100KB

### El bot falla al ejecutarse

- ✅ Revisa los logs con `Android.log()`
- ✅ Verifica la sintaxis JavaScript
- ✅ Asegúrate de que todas las APIs usadas estén disponibles
- ✅ Prueba con el botón "Probar Bot"

### El bot es lento

- ✅ Optimiza llamadas HTTP (usa caché)
- ✅ Reduce la complejidad de la lógica
- ✅ Verifica que no haya bucles infinitos

---

**¿Necesitas ayuda?** Abre un issue en el repositorio de GitHub.
