# Guía de uso de Bots JavaScript (BotJS)

**Estado:** ✅ Disponible en la build actual

## Objetivo

Descargar y administrar bots JavaScript que automaticen respuestas a notificaciones de WhatsApp, Telegram, Signal y otras apps, sin modificar el código nativo de Watomagic.

## Antes de empezar

- Android 8.0+ con permisos de notificación y respuesta desde notificaciones.
- Scripts alojados solo en **HTTPS** (máx. **100 KB**).
- Timeout de ejecución: **5 segundos**.
- Motor: Mozilla Rhino (ES5, sin `async/await`).

## Configuración

1. **Ajustes → Bot JavaScript** (`BotConfigActivity`).
2. Activar **Habilitar Bot JavaScript**.
3. Pegar la URL HTTPS de `bot.js` y pulsar **Descargar Bot**.
4. Revisar la tarjeta **Información del Bot** (URL, hash, fecha).
5. **Probar Bot** con una notificación ficticia.

### Opciones avanzadas

| Opción | Función |
|--------|---------|
| Auto-actualización | WorkManager verifica cada 6 h |
| Acceso a imágenes en notificaciones | El bot puede leer adjuntos entrantes |
| Carpeta WhatsApp (SAF) | Lee imágenes cuando la notif. solo muestra placeholder |
| Variables de entorno | `CLAVE='valor'` por línea, accesibles con `Android.getenv` |
| Modo Debug / Ver Logs | Diagnóstico vía etiqueta `BotJS` |

**Nota:** Las respuestas del bot son solo texto. No se pueden enviar imágenes como reply.

## Comportamiento

Con BotJS activo, `BotJsReplyProvider` procesa cada notificación:

- `REPLY` → envía texto personalizado
- `DISMISS` → descarta sin responder
- `KEEP` → usa respuesta estática/OpenAI
- `SNOOZE` → pospone (si la app lo permite)

Si el bot falla (error, timeout, rate limit), Watomagic usa el mensaje de respaldo.

## Resolución de problemas

| Problema | Causa probable | Acción |
|----------|----------------|--------|
| `Bot code validation failed` | >100 KB o patrones prohibidos | Revisar el script |
| `Only HTTPS URLs are allowed` | URL insegura | Usar hosting HTTPS |
| `Bot execution exceeded 5000ms` | API externa lenta | Cachear o simplificar |
| Respuestas repetidas | Sin rate limiting en el bot | Usar `Android.storage*` |

## Más información

- [API Reference](./BOT_API_REFERENCE.md)
- [Guía de desarrollo](./BOT_DEVELOPMENT_GUIDE.md)
- [Arquitectura](./ARCHITECTURE.md)
