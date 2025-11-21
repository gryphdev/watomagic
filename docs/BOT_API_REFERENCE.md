# BotJS API Reference

**Versión del documento:** 0.2 (2025-01-21)
**JavaScript Engine:** Mozilla Rhino 1.7.15 (ES5 + ES6 parcial)
**Estado:** ✅ Implementado y funcional

---

## 1. Tipos principales

### NotificationData
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `number` | Identificador incremental de la notificación. |
| `appPackage` | `string` | Nombre de paquete Android (`com.whatsapp`, etc.). |
| `title` | `string` | Título mostrado en la notificación. |
| `body` | `string` | Texto completo del mensaje. |
| `timestamp` | `number` | Epoch en milisegundos. |
| `isGroup` | `boolean` | `true` si la notificación pertenece a un chat grupal. |
| `actions` | `string[]` | Lista de Quick Reply actions disponibles. |

### BotResponse
| Campo | Tipo | Obligatorio | Descripción |
|-------|------|-------------|-------------|
| `action` | `'KEEP' \| 'DISMISS' \| 'REPLY' \| 'SNOOZE'` | Sí | Acción solicitada. |
| `replyText` | `string` | Solo para `REPLY` | Texto que se enviará como respuesta. |
| `snoozeMinutes` | `number` | Solo para `SNOOZE` | Minutos que la notificación debe posponerse. |
| `reason` | `string` | No | Texto para logs y diagnósticos. |

### Resultados y errores
- Si `processNotification` lanza una excepción o retorna un valor inválido, Watomagic registra `BotExecutionException` y usa el mensaje de respaldo.
- Toda respuesta debe ser serializable a JSON. Valores `undefined` se descartan automáticamente.

---

## 2. Objeto global `Android`

### `Android.log(level, message)`
- `level`: `'debug' | 'info' | 'warn' | 'error'`
- Escribe en Logcat con la etiqueta `BotJS`.

### `Android.storageGet(key)`
- Retorna `string | null`.
- Espacio aislado `bot_storage` (SharedPreferences). Máximo 200 claves.

### `Android.storageSet(key, value)`
- Guarda `value` (string) usando `apply()`. Se sobrescribe si existe.

### `Android.storageRemove(key)`
- Elimina la clave indicada.

### `Android.storageKeys()`
- Devuelve `string[]` con todas las claves almacenadas.

### `Android.httpRequest(options)`
```ts
type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE';

interface HttpRequestOptions {
  url: string;                   // HTTPS obligatorio
  method?: HttpMethod;           // Default: 'GET'
  headers?: Record<string,string>;
  body?: string;                 // Se envía tal cual, no se serializa automáticamente
  timeoutMs?: number;            // Opcional (<= 5000). Default: 4000
}
```
- Retorna `string` con el cuerpo de la respuesta (operación síncrona, bloquea hasta recibir respuesta o timeout).
- Errores comunes: `ERR_HTTP_TIMEOUT`, `ERR_HTTP_NON_200`.

### `Android.getCurrentTime()`
- Retorna `number` (epoch ms). Ideal para rate limiting.

### `Android.getAppName(packageName)`
- Retorna el nombre legible (por ejemplo, “WhatsApp”).

---

## 3. Restricciones del sandbox
- Tiempo máximo de CPU: **5000 ms**. Se aborta con `TimeoutException`.
- Memoria máxima por ejecución: 4 MB (límite interno de Rhino).
- No hay acceso a `require`, `import`, `XMLHttpRequest`, `fetch`, `localStorage`, `navigator`.
- `Math.random()` está permitido pero usa la implementación de Rhino (no criptográfica).
- Todos los módulos se ejecutan en un único thread y se destruyen tras cada notificación.

---

## 4. Errores estándar
| Código | Cuándo ocurre | Acción recomendada |
|--------|---------------|--------------------|
| `BOT_VALIDATION_FAILED` | Script demasiado grande o contiene patrones prohibidos. | Reducir tamaño y eliminar `eval`, `Function`, `__proto__`, `import()`. |
| `BOT_TIMEOUT` | El bot no respondió en 5 s. | Optimizar reglas o cachear peticiones externas. |
| `BOT_RATE_LIMIT` | Se superó el límite de 100 ejecuciones/min. | Añadir lógica de batching o sleeps. |
| `HTTP_ONLY_HTTPS` | URL no segura al descargar. | Usar hosting HTTPS. |
| `HTTP_REQUEST_BLOCKED` | Intento de usar HTTP (no HTTPS) dentro del bot. | Migrar a HTTPS. |

---

## 5. Versionado y compatibilidad
- **Versión 0.1** (actual): primera iteración. Considera la interfaz estable, salvo ampliaciones en `Android`.
- Los bots deberían declarar en un comentario superior qué versión de la API esperan:
  ```javascript
  // BotJS API: 0.1
  ```
- Cuando exista la versión 0.2+, Watomagic expondrá `Android.getApiVersion()` para permitir degradar funcionalidad.

---

## 6. Recursos relacionados
- [Guía de desarrollo](./BOT_DEVELOPMENT_GUIDE.md)
- [Guía de usuario](./BOT_USER_GUIDE.md)
- [Plan maestro y roadmap](./PLAN_BOTJS_SYSTEM.md)
