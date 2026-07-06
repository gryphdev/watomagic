# BotJS API Reference

**Versión del documento:** 0.4 (2025-01-21)
**JavaScript Engine:** Mozilla Rhino 1.7.15 (ES5 + ES6 parcial)
**Estado:** ✅ Implementado y funcional
**Cambios en v0.4:** Corrección de exposición de métodos Android usando `FunctionObject` y `ScriptableObject` personalizado
**Cambios en v0.3:** Añadido objeto global `localStorage` compatible con API estándar

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
| `attachments` | `AttachmentInfo[]` | Imágenes extraídas de la notificación (requiere activar acceso en BotConfig). |

### AttachmentInfo
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `string` | Identificador único del adjunto en la sesión del bot. |
| `mimeType` | `string` | Tipo MIME (ej. `image/jpeg`). |
| `size` | `number` | Tamaño en bytes (máx. 5 MB). |
| `hasFile` | `boolean` | `true` si el archivo está disponible en sandbox. |
| `thumbnailBase64` | `string?` | Miniatura JPEG en Base64 para preview. |

### BotResponse
| Campo | Tipo | Obligatorio | Descripción |
|-------|------|-------------|-------------|
| `action` | `'KEEP' \| 'DISMISS' \| 'REPLY' \| 'SNOOZE'` | Sí | Acción solicitada. |
| `replyText` | `string` | Solo para `REPLY` | Texto que se enviará como respuesta. |
| `snoozeMinutes` | `number` | Solo para `SNOOZE` | Minutos que la notificación debe posponerse. |
| `reason` | `string` | No | Texto para logs y diagnósticos. |
| `attachments` | `AttachmentToSend[]` | No | Imágenes a enviar con `REPLY` (requiere activar envío en BotConfig). |

### AttachmentToSend
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `path` | `string` | Ruta relativa a `bot_attachments/` o absoluta dentro del sandbox de la app. |
| `mimeType` | `string` | Tipo MIME del archivo (ej. `image/jpeg`). |

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

---

## 3. Objeto global `localStorage`

**Disponible desde:** v0.2 (2025-01-21)

Watomagic proporciona un objeto `localStorage` global compatible con la API estándar del navegador. Internamente usa `Android.storage*` para persistencia.

### `localStorage.getItem(key)`
- Retorna `string | null`.
- Equivalente a `Android.storageGet(key)`.

### `localStorage.setItem(key, value)`
- Guarda `value` (string) de forma persistente.
- Equivalente a `Android.storageSet(key, value)`.

### `localStorage.removeItem(key)`
- Elimina la clave indicada.
- Equivalente a `Android.storageRemove(key)`.

### `localStorage.clear()`
- Elimina todas las claves almacenadas.

### `localStorage.key(index)`
- Retorna `string | null` con la clave en el índice dado (0-based).

### `localStorage.length`
- Propiedad de solo lectura que retorna el número de claves almacenadas.
- Se calcula dinámicamente en cada acceso.

**Ejemplo de uso:**
```javascript
// Sintaxis estándar de localStorage
localStorage.setItem('lastReply', Date.now().toString());
var lastReply = localStorage.getItem('lastReply');

// También puedes usar Android.storage* directamente
Android.storageSet('lastReply', Date.now().toString());
var lastReply = Android.storageGet('lastReply');
```

---

## 4. Objeto global `Android` (continuación)

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
- Retorna el nombre legible (por ejemplo, "WhatsApp").

### `Android.getAttachmentPath(id)`
- Retorna la ruta absoluta del adjunto en sandbox, o `null`.

### `Android.readAttachmentAsBase64(id)`
- Lee el adjunto completo como Base64 (máx. 5 MB), o `null`.

### `Android.getAttachmentThumbnail(id)`
- Retorna el thumbnail Base64 del adjunto, o `null`.

---

## 4.1 Adjuntos (imágenes)

### Fuentes soportadas (recepción)
Las imágenes se extraen **desde la notificación**, no desde carpetas de WhatsApp (`/Android/media/com.whatsapp/`). Fuentes, en orden de prioridad:

1. `MessagingStyle.Message.getDataUri()` — apps de mensajería modernas.
2. `Notification.EXTRA_PICTURE` / `EXTRA_BIG_PICTURE` — bitmap embebido en extras.

Los archivos se copian a `getExternalFilesDir()/bot_attachments/` (sandbox de Watomagic). Requiere activar **Acceso a adjuntos** en BotConfig.

### Envío de imágenes (limitaciones)
Para responder con imagen, el bot retorna `attachments` en `BotResponse`:

```javascript
return {
  action: 'REPLY',
  replyText: 'Aquí va la imagen',
  attachments: [{ path: 'mi-imagen.jpg', mimeType: 'image/jpeg' }]
};
```

El envío usa `RemoteInput.addDataResultToIntent()` — la API oficial para datos binarios en inline reply. **Solo funciona si la app destino declaró `setAllowDataType("image/*")` en su RemoteInput.** WhatsApp puede no soportarlo; en ese caso Watomagic envía el texto y registra un warning en log.

No se requieren permisos de almacenamiento externo: todo opera en el sandbox de la app + FileProvider.

---

## 5. Restricciones del sandbox
- Tiempo máximo de CPU: **5000 ms**. Se aborta con `TimeoutException`.
- Memoria máxima por ejecución: 4 MB (límite interno de Rhino).
- No hay acceso a `require`, `import`, `XMLHttpRequest`, `fetch`, `navigator`.
- **`localStorage` está disponible** (implementación personalizada que usa `Android.storage*`).
- `Math.random()` está permitido pero usa la implementación de Rhino (no criptográfica).
- Todos los módulos se ejecutan en un único thread y se destruyen tras cada notificación.

---

## 6. Errores estándar
| Código | Cuándo ocurre | Acción recomendada |
|--------|---------------|--------------------|
| `BOT_VALIDATION_FAILED` | Script demasiado grande o contiene patrones prohibidos. | Reducir tamaño y eliminar `eval`, `Function`, `__proto__`, `import()`. |
| `BOT_TIMEOUT` | El bot no respondió en 5 s. | Optimizar reglas o cachear peticiones externas. |
| `BOT_RATE_LIMIT` | Se superó el límite de 100 ejecuciones/min. | Añadir lógica de batching o sleeps. |
| `HTTP_ONLY_HTTPS` | URL no segura al descargar. | Usar hosting HTTPS. |
| `HTTP_REQUEST_BLOCKED` | Intento de usar HTTP (no HTTPS) dentro del bot. | Migrar a HTTPS. |

---

## 7. Versionado y compatibilidad
- **Versión 0.4** (actual): corrección de exposición de métodos Android usando `FunctionObject` explícito.
- **Versión 0.3**: añadido `localStorage` global.
- **Versión 0.2**: primera iteración con `Android.storage*`.
- **Versión 0.1**: primera iteración con `Android.storage*`.
- Los bots deberían declarar en un comentario superior qué versión de la API esperan:
  ```javascript
  // BotJS API: 0.4
  ```
- Cuando exista la versión 0.5+, Watomagic expondrá `Android.getApiVersion()` para permitir degradar funcionalidad.

---

## 8. Recursos relacionados
- [Guía de desarrollo](./BOT_DEVELOPMENT_GUIDE.md)
- [Guía de usuario](./BOT_USER_GUIDE.md)
- [Plan maestro y roadmap](./PLAN_BOTJS_SYSTEM.md)
