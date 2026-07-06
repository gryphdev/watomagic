# Arquitectura del sistema BotJS

**Última actualización:** 2026-07-06  
**Motor:** Mozilla Rhino 1.7.15 (ES5 + ES6 parcial)

## 1. Visión general

```
Notificación entrante
        │
        ▼
NotificationService.sendReply()
        │
        ▼
ReplyProviderFactory
   ├─ StaticReplyProvider
   ├─ OpenAIReplyProvider
   └─ BotJsReplyProvider ──► BotJsEngine (Rhino) ──► bot.js
```

- Cada provider implementa `ReplyProvider.generateReply`.
- `BotJsReplyProvider` serializa `NotificationData`, ejecuta el bot y traduce `BotResponse`.
- Si BotJS falla, se usa la respuesta estática configurada.

## 2. Módulos principales

| Módulo | Rol |
|--------|-----|
| `replyproviders.*` | Strategy pattern: Static, OpenAI, BotJS |
| `botjs/BotJsEngine` | Runtime Rhino con timeout y sandbox |
| `botjs/BotAndroidAPI` | APIs expuestas al bot (log, storage, HTTP, adjuntos) |
| `botjs/BotRepository` | Descarga HTTPS, SHA-256, `active-bot.js` |
| `botjs/AttachmentExtractor` | Extrae imágenes de notificaciones entrantes |
| `botjs/WhatsAppMediaResolver` | Lectura vía SAF cuando hay placeholder |
| `workers/BotUpdateWorker` | Auto-update cada 6 h (WorkManager) |
| `activity/botconfig/*` | GUI Material 3 de configuración |

## 3. Flujos clave

### Respuesta a notificación

1. `NotificationService` recibe `StatusBarNotification`.
2. Se construye `NotificationData` (incluye adjuntos si está habilitado).
3. `ReplyProviderFactory` elige provider según preferencias.
4. `BotJsReplyProvider` valida y ejecuta `active-bot.js` (timeout 5 s).
5. `sendActualReply()` envía texto o aplica fallback.

### Descarga y auto-update

1. URL HTTPS en `BotConfigActivity` → `BotRepository.downloadBot()`.
2. Validación SHA-256 opcional y almacenamiento con metadata.
3. `BotUpdateWorker` verifica cada 6 h y actualiza si el hash cambia.

## 4. Seguridad

- Timeout: 5 s por ejecución.
- Tamaño máximo del script: 100 KB.
- Rate limit: 100 ejecuciones/min, 3 min entre descargas de bots distintos.
- HTTP del bot: solo HTTPS.
- Sandbox: sin filesystem arbitrario, patrones peligrosos bloqueados.
- Adjuntos: solo lectura (notificación + SAF); respuestas solo texto.

## 5. Compatibilidad upstream

Cambios mínimos en Watomatic (`NotificationService`, `PreferencesManager`, entrada en Settings). Todo lo demás vive en `com.parishod.watomagic`.

## 6. Dependencias

- **Rhino 1.7.15** (~1.5 MB)
- **OkHttp / Retrofit** — descargas y HTTP del bot
- **WorkManager** — auto-updates

## 7. CI/CD

Builds firmados vía GitHub Actions (`.github/workflows/android-release.yml`). Ver [GITHUB_ACTIONS_MIGRATION.md](./GITHUB_ACTIONS_MIGRATION.md).
