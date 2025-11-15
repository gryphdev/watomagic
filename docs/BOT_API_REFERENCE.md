# 📚 Referencia de API: Bots JavaScript para Watomagic

Documentación completa de todas las APIs disponibles para bots JavaScript.

---

## 📋 Tabla de Contenidos

- [Interfaces TypeScript](#interfaces-typescript)
- [APIs de Android](#apis-de-android)
- [Ejemplos de Uso](#ejemplos-de-uso)

---

## 🔷 Interfaces TypeScript

### NotificationData

Datos de la notificación entrante que recibe el bot.

```typescript
interface NotificationData {
    id: number;              // ID único de la notificación
    appPackage: string;      // Package name de la app (ej: 'com.whatsapp')
    title: string;          // Título de la notificación
    body: string;           // Contenido del mensaje
    timestamp: number;      // Timestamp en milisegundos (Unix epoch)
    isGroup: boolean;       // true si la notificación proviene de un grupo
    actions: string[];      // Lista de acciones disponibles en la notificación
}
```

**Ejemplo**:
```javascript
{
    id: 12345,
    appPackage: 'com.whatsapp',
    title: 'Juan Pérez',
    body: 'Hola, ¿cómo estás?',
    timestamp: 1700000000000,
    isGroup: false,
    actions: ['REPLY', 'CALL']
}
```

---

### BotResponse

Respuesta que debe retornar la función `processNotification`.

```typescript
interface BotResponse {
    action: 'KEEP' | 'DISMISS' | 'REPLY' | 'SNOOZE';
    replyText?: string;        // Requerido si action = 'REPLY'
    snoozeMinutes?: number;    // Requerido si action = 'SNOOZE'
    reason?: string;           // Opcional: para logging/debugging
}
```

#### Acciones Disponibles

| Acción | Descripción | Campos Requeridos |
|--------|--------------|-------------------|
| `KEEP` | Mantener la notificación sin responder | Ninguno |
| `DISMISS` | Descartar la notificación sin responder | Ninguno |
| `REPLY` | Responder automáticamente | `replyText` |
| `SNOOZE` | Posponer la notificación | `snoozeMinutes` |

**Ejemplos**:
```javascript
// Mantener notificación
{ action: 'KEEP' }

// Descartar notificación
{ action: 'DISMISS', reason: 'Spam detectado' }

// Responder automáticamente
{ action: 'REPLY', replyText: 'Gracias por tu mensaje!' }

// Posponer 30 minutos
{ action: 'SNOOZE', snoozeMinutes: 30, reason: 'Horario de sueño' }
```

---

## 🤖 APIs de Android

Todas las APIs están disponibles a través del objeto global `Android`.

### Android.log(level, message)

Registra un mensaje en los logs de la aplicación.

**Parámetros**:
- `level` (string, requerido): Nivel de log. Valores: `'debug'`, `'info'`, `'warn'`, `'error'`
- `message` (string, requerido): Mensaje a registrar

**Retorna**: `void`

**Ejemplo**:
```javascript
Android.log('info', 'Procesando notificación de WhatsApp');
Android.log('error', 'Error al conectar con API');
Android.log('debug', `Valor almacenado: ${Android.storageGet('key')}`);
```

**Notas**:
- Los logs son visibles en la sección "Ver Logs del Bot" en la app
- Usa `'debug'` para información detallada de depuración
- Usa `'error'` para errores críticos que requieren atención

---

### Android.storageGet(key)

Obtiene un valor previamente almacenado.

**Parámetros**:
- `key` (string, requerido): Clave del valor a obtener

**Retorna**: `string | null` - El valor almacenado o `null` si no existe

**Ejemplo**:
```javascript
const lastReply = Android.storageGet('lastAutoReply');
if (lastReply) {
    Android.log('info', `Última respuesta: ${lastReply}`);
} else {
    Android.log('info', 'No hay última respuesta registrada');
}
```

**Notas**:
- Los valores se almacenan como strings
- Para objetos, usa `JSON.stringify()` al guardar y `JSON.parse()` al leer
- Los datos persisten entre ejecuciones del bot

---

### Android.storageSet(key, value)

Almacena un valor para uso futuro.

**Parámetros**:
- `key` (string, requerido): Clave para identificar el valor
- `value` (string, requerido): Valor a almacenar (debe ser string)

**Retorna**: `void`

**Ejemplo**:
```javascript
// Almacenar timestamp
Android.storageSet('lastAutoReply', Date.now().toString());

// Almacenar objeto (convertir a JSON)
const data = { count: 5, lastUpdate: Date.now() };
Android.storageSet('stats', JSON.stringify(data));

// Almacenar string simple
Android.storageSet('userPreference', 'enabled');
```

**Notas**:
- Los valores deben ser strings
- Para objetos, usa `JSON.stringify()`
- No hay límite de tamaño, pero se recomienda mantener valores pequeños
- Los datos persisten entre ejecuciones del bot

---

### Android.storageRemove(key)

Elimina un valor almacenado.

**Parámetros**:
- `key` (string, requerido): Clave del valor a eliminar

**Retorna**: `void`

**Ejemplo**:
```javascript
Android.storageRemove('lastAutoReply');
Android.storageRemove('temporaryData');
```

**Notas**:
- No genera error si la clave no existe
- Útil para limpiar datos temporales

---

### Android.storageKeys()

Obtiene todas las claves almacenadas.

**Parámetros**: Ninguno

**Retorna**: `string[]` - Array de claves almacenadas

**Ejemplo**:
```javascript
const keys = Android.storageKeys();
Android.log('info', `Claves almacenadas: ${keys.join(', ')}`);

// Limpiar todas las claves
keys.forEach(key => Android.storageRemove(key));
```

**Notas**:
- Retorna un array vacío si no hay claves almacenadas
- Útil para debugging y limpieza de datos

---

### Android.httpRequest(options)

Realiza una petición HTTP a una API externa.

**Parámetros**:
- `options` (object, requerido): Opciones de la petición
  - `url` (string, requerido): URL HTTPS de la API
  - `method` (string, opcional): Método HTTP. Valores: `'GET'`, `'POST'`, `'PUT'`, `'DELETE'`. Default: `'GET'`
  - `headers` (object, opcional): Headers HTTP como objeto clave-valor
  - `body` (string, opcional): Cuerpo de la petición (para POST/PUT)

**Retorna**: `Promise<string>` - Respuesta HTTP como string

**Ejemplo GET**:
```javascript
try {
    const response = await Android.httpRequest({
        url: 'https://api.example.com/data',
        method: 'GET',
        headers: {
            'Authorization': 'Bearer YOUR_API_KEY'
        }
    });
    
    const data = JSON.parse(response);
    Android.log('info', `Datos recibidos: ${data}`);
} catch (error) {
    Android.log('error', `Error en petición: ${error.message}`);
}
```

**Ejemplo POST**:
```javascript
try {
    const response = await Android.httpRequest({
        url: 'https://api.openai.com/v1/chat/completions',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer YOUR_API_KEY'
        },
        body: JSON.stringify({
            model: 'gpt-3.5-turbo',
            messages: [
                { role: 'user', content: 'Hola' }
            ]
        })
    });
    
    const result = JSON.parse(response);
    // Procesar resultado
} catch (error) {
    Android.log('error', `Error: ${error.message}`);
}
```

**Restricciones**:
- ❌ Solo se permiten URLs HTTPS (no HTTP)
- ⚠️ Timeout implícito: Si la petición tarda más de 5 segundos, puede fallar
- ⚠️ El bot completo tiene un timeout de 5 segundos

**Notas**:
- Siempre usa `try/catch` para manejar errores
- Para APIs que retornan JSON, usa `JSON.parse()` para parsear la respuesta
- Los headers son opcionales pero recomendados para autenticación

---

### Android.getCurrentTime()

Obtiene el timestamp actual en milisegundos.

**Parámetros**: Ninguno

**Retorna**: `number` - Timestamp en milisegundos desde Unix epoch (1 de enero de 1970)

**Ejemplo**:
```javascript
const now = Android.getCurrentTime();
const lastReply = Android.storageGet('lastAutoReply');

if (lastReply) {
    const timeDiff = now - parseInt(lastReply);
    const hoursDiff = timeDiff / (1000 * 60 * 60);
    
    Android.log('info', `Última respuesta hace ${hoursDiff.toFixed(2)} horas`);
}
```

**Notas**:
- Útil para rate limiting y comparaciones de tiempo
- Retorna milisegundos, no segundos
- Compatible con `Date.now()` en JavaScript estándar

---

### Android.getAppName(packageName)

Obtiene el nombre legible de una aplicación Android.

**Parámetros**:
- `packageName` (string, requerido): Package name de la app (ej: `'com.whatsapp'`)

**Retorna**: `string` - Nombre legible de la app o el package name si no se encuentra

**Ejemplo**:
```javascript
const appName = Android.getAppName('com.whatsapp');
Android.log('info', `Notificación de: ${appName}`); // "Notificación de: WhatsApp"

const unknownApp = Android.getAppName('com.unknown.app');
Android.log('info', unknownApp); // "com.unknown.app"
```

**Notas**:
- Retorna el nombre que el usuario ve en el launcher
- Si la app no está instalada o no se encuentra, retorna el package name
- Útil para logging y mensajes de usuario

---

## 📝 Función Principal

### processNotification(notification)

Función principal que debe implementar todo bot. Se ejecuta cada vez que llega una notificación.

**Parámetros**:
- `notification` (NotificationData, requerido): Datos de la notificación entrante

**Retorna**: `Promise<BotResponse> | BotResponse` - Acción a realizar

**Ejemplo Síncrono**:
```javascript
function processNotification(notification) {
    if (notification.appPackage === 'com.whatsapp') {
        return {
            action: 'REPLY',
            replyText: 'Gracias por tu mensaje!'
        };
    }
    return { action: 'KEEP' };
}
```

**Ejemplo Asíncrono**:
```javascript
async function processNotification(notification) {
    try {
        const response = await Android.httpRequest({
            url: 'https://api.example.com/classify',
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: notification.body })
        });
        
        const result = JSON.parse(response);
        
        if (result.shouldReply) {
            return {
                action: 'REPLY',
                replyText: result.reply
            };
        }
    } catch (error) {
        Android.log('error', `Error: ${error.message}`);
    }
    
    return { action: 'KEEP' };
}
```

**Notas**:
- Puede ser síncrona o asíncrona
- Debe retornar siempre un `BotResponse` válido
- Si lanza una excepción, Watomagic usará el método de respuesta fallback

---

## 🔍 Ejemplos de Uso Completo

### Ejemplo 1: Bot Simple con Rate Limiting

```javascript
async function processNotification(notification) {
    const RATE_LIMIT_MS = 3600000; // 1 hora
    
    if (notification.appPackage === 'com.whatsapp') {
        const lastReply = Android.storageGet('lastAutoReply');
        const now = Android.getCurrentTime();
        
        if (!lastReply || now - parseInt(lastReply) > RATE_LIMIT_MS) {
            Android.storageSet('lastAutoReply', now.toString());
            
            return {
                action: 'REPLY',
                replyText: 'Estoy ocupado ahora. Te respondo pronto!'
            };
        }
        
        Android.log('debug', 'Rate limit activo, no respondiendo');
    }
    
    return { action: 'KEEP' };
}
```

### Ejemplo 2: Bot con API Externa

```javascript
async function processNotification(notification) {
    try {
        Android.log('info', `Consultando API para: ${notification.title}`);
        
        const response = await Android.httpRequest({
            url: 'https://api.example.com/process',
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
        
        if (result.action === 'reply') {
            return {
                action: 'REPLY',
                replyText: result.message
            };
        } else if (result.action === 'dismiss') {
            return {
                action: 'DISMISS',
                reason: result.reason
            };
        }
    } catch (error) {
        Android.log('error', `Error en API: ${error.message}`);
    }
    
    return { action: 'KEEP' };
}
```

### Ejemplo 3: Bot con Múltiples Reglas

```javascript
async function processNotification(notification) {
    // Regla 1: Bloquear apps
    const blockedApps = ['com.spam.app'];
    if (blockedApps.includes(notification.appPackage)) {
        return { action: 'DISMISS', reason: 'App bloqueada' };
    }
    
    // Regla 2: Detectar spam
    const spamPatterns = [/ganaste/i, /regalo/i];
    const fullText = `${notification.title} ${notification.body}`;
    for (const pattern of spamPatterns) {
        if (pattern.test(fullText)) {
            return { action: 'DISMISS', reason: 'Spam' };
        }
    }
    
    // Regla 3: Horario de sueño
    const hour = new Date().getHours();
    if (hour >= 23 || hour < 7) {
        return {
            action: 'SNOOZE',
            snoozeMinutes: 480,
            reason: 'Horario de sueño'
        };
    }
    
    // Regla 4: Auto-respuesta con rate limiting
    if (notification.appPackage === 'com.whatsapp') {
        const lastReply = Android.storageGet('lastAutoReply');
        const now = Android.getCurrentTime();
        
        if (!lastReply || now - parseInt(lastReply) > 3600000)) {
            Android.storageSet('lastAutoReply', now.toString());
            return {
                action: 'REPLY',
                replyText: 'Gracias por tu mensaje!'
            };
        }
    }
    
    return { action: 'KEEP' };
}
```

---

## ⚠️ Limitaciones y Restricciones

### Limitaciones Técnicas

| Limitación | Valor | Descripción |
|------------|-------|-------------|
| Timeout de ejecución | 5 segundos | El bot se cancela automáticamente |
| Tamaño máximo del bot | 100KB | Tamaño del archivo JavaScript |
| Rate limiting | 100 ejecuciones/minuto | Máximo de ejecuciones por minuto |
| Protocolo HTTP | Solo HTTPS | No se permiten URLs HTTP |

### Patrones Bloqueados

Los siguientes patrones causarán que el bot sea rechazado:

- `eval(...)`
- `Function(...)`
- `constructor[...]`
- `__proto__`
- `import(...)`

### Restricciones de Seguridad

Los bots **NO pueden**:
- ❌ Acceder al sistema de archivos
- ❌ Leer contactos o datos de otras apps
- ❌ Modificar configuraciones del sistema
- ❌ Ejecutar código peligroso

---

## 📚 Recursos Adicionales

- **[Guía para Desarrolladores](./BOT_DEVELOPMENT_GUIDE.md)** - Tutorial completo para crear bots
- **[Guía para Usuarios](./BOT_USER_GUIDE.md)** - Cómo usar bots en Watomagic
- **[Plan de Implementación](./PLAN_BOTJS_SYSTEM.md)** - Detalles técnicos del sistema

---

**Última actualización**: 2025-11-15
