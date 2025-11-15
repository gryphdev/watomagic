# Guía de uso de Bots JavaScript (BotJS)

> ⚠️ **Estado**: El sistema BotJS aún no está disponible en la build pública. Esta guía describe cómo funcionará la experiencia de usuario una vez que el feature se habilite.

## Objetivo
Permitir que cualquier usuario descargue, pruebe y administre bots JavaScript que automaticen las respuestas a notificaciones de apps como WhatsApp, Telegram o Signal, sin necesidad de tocar el código nativo de Watomagic.

## Antes de empezar
- Requiere Android 8.0+ con permisos de notificación y responder desde notificaciones habilitados.
- Solo se aceptan scripts alojados en **HTTPS**. Las URLs `http://` son rechazadas automáticamente.
- Cada bot debe pesar menos de **100 KB** y exponer la función `processNotification`.
- La ejecución está aislada mediante QuickJS, con un timeout duro de **5 segundos**.

## Flujo rápido
1. **Abrir Ajustes → Bots JavaScript**: se abrirá `BotConfigActivity` con cuatro tarjetas (estado, descarga, info y opciones avanzadas).
2. **Habilitar el switch "Activar BotJS"**: mientras esté apagado, se usará la respuesta automática estándar u OpenAI, según preferencias.
3. **Ingresar la URL del bot**: pegar la dirección HTTPS donde está publicado `bot.js`. La app validará tamaño, patrón y firma (cuando esté habilitada).
4. **Descargar**: el botón “Descargar bot” lanzará la descarga y mostrará el spinner. Ante cualquier error se mostrará un Snackbar con el motivo (por ejemplo, timeout o validación).
5. **Probar**: al finalizar la descarga, la tarjeta “Información del bot” mostrará URL, hash y fecha. El botón “Probar bot” ejecuta el script contra una notificación ficticia y muestra el resultado (`KEEP`, `REPLY`, etc.).
6. **Auto‑update**: desde “Opciones avanzadas” se puede activar actualizaciones cada 6 horas (WorkManager). Si falla una actualización, el bot anterior se mantiene.
7. **Logs y limpieza**: “Ver registros” abre los logs filtrados por la etiqueta `BotJS`, y “Eliminar bot” borra el archivo `active-bot.js` junto con su metadata.

## Qué ocurre tras habilitarlo
1. `ReplyProviderFactory` prioriza `BotJsReplyProvider` cuando `isBotJsEnabled()` es verdadero.
2. Las notificaciones entrantes se serializan a `NotificationData` y se envían al bot mediante `BotJsEngine`.
3. La respuesta del bot se traduce en una acción:
   - `REPLY`: se envía la respuesta personalizada.
   - `DISMISS`: no se responde y se marca como atendida.
   - `KEEP`: se aplica la respuesta estática configurada.
   - `SNOOZE`: la notificación se reprograma (si la app de origen lo permite).
4. Si el bot arroja error, timeout o excede el rate limit, Watomagic usa el mensaje de respaldo configurado por el usuario.

## Buenas prácticas para usuarios
- Mantener los bots en repositorios de confianza y revisar el código fuente antes de habilitarlos.
- Aprovechar el botón de prueba cada vez que se actualice el bot.
- Revisar periódicamente la tarjeta de información para verificar la fecha de última actualización y el hash.
- Desactivar BotJS temporalmente si se detectan respuestas inusuales; el sistema volverá automáticamente al provider OpenAI o al mensaje estático.

## Resolución de problemas
| Problema | Posible causa | Acción recomendada |
|----------|---------------|--------------------|
| `Bot code validation failed` | El archivo supera 100 KB o contiene patrones prohibidos (`eval`, `Function`, etc.) | Revisar el script antes de volver a subirlo |
| `Only HTTPS URLs are allowed` | La URL no usa HTTPS | Migrar el archivo a un hosting seguro |
| `Bot execution exceeded 5000ms` | El bot tarda demasiado (llamadas externas lentas) | Añadir timeouts a las APIs usadas o cachear resultados |
| El bot responde constantemente | Faltan reglas de rate limiting dentro del script | Usar `Android.storage*` para registrar la última respuesta |

## Roadmap visible en la UI
1. **Fase 1–4** (provider + motor) deben completarse antes de exponer el switch en Ajustes.
2. **Fase 5** agrega descargas y actualizaciones silenciosas.
3. **Fase 6** habilita completamente la pantalla BotJS con logs y botones de prueba.
4. **Fase 7** incluye diagnósticos automáticos (alertas si un bot falla de forma repetida).

Mantendremos este documento sincronizado con los avances; el estado más reciente se encuentra en `docs/PLAN_BOTJS_SYSTEM.md`.
