# Arquitectura del sistema BotJS

**Última actualización:** 2025-11-15  
**Estado:** Arquitectura aprobada, implementación pendiente (Fases 1–7 sin iniciar).

---

## 1. Visión general

```
Notificación entrante
        │
        ▼
NotificationService.sendReply()
        │
        ▼
ReplyProviderFactory ──────────────────────────────┐
   │ StaticReplyProvider                            │
   │ OpenAIReplyProvider                            │
   └ BotJsReplyProvider ──► BotJsEngine ──► QuickJS │
                                   │               │
                                   ▼               │
                              bot.js del usuario ◄─┘
```

- Cada provider implementa `ReplyProvider.generateReply`.
- `BotJsReplyProvider` serializa la notificación, invoca el motor QuickJS y traduce la salida (`BotResponse`).
- Si BotJS falla, el sistema vuelve al mensaje estático para mantener compatibilidad.

---

## 2. Módulos principales

| Módulo | Estado | Rol |
|--------|--------|-----|
| `replyproviders.*` | Diseño aprobado | Contiene interfaz, factory y providers (Static, OpenAI, BotJS). |
| `botjs/BotJsEngine` | Diseño aprobado | Encapsula QuickJS, inyecta `Android` y maneja timeouts. |
| `botjs/BotAndroidAPI` | Diseño aprobado | Implementa logging, storage y HTTP con OkHttp. |
| `botjs/BotRepository` | Diseño aprobado | Descargar, validar y almacenar `active-bot.js`. |
| `botjs/BotValidator` | Diseño aprobado | Reglas estáticas (tamaño, patrones, firma futura). |
| `workers/BotUpdateWorker` | Diseño aprobado | WorkManager periódico para auto-updates. |
| `activity/botconfig/*` | Diseño aprobado | Pantalla Material 3 para configurar bots. |

---

## 3. Flujos clave

### 3.1 Respuesta a notificación
1. `NotificationService` recibe `StatusBarNotification`.
2. Se construye `NotificationData`.
3. `ReplyProviderFactory` elige provider según preferencias.
4. `BotJsReplyProvider`:
   - Carga `active-bot.js` desde `files/bots/`.
   - Valida con `BotValidator`.
   - Ejecuta `BotJsEngine.executeBot()` con timeout de 5 s.
   - Convierte la respuesta en acciones (send reply, dismiss, etc.).
5. `sendActualReply()` se encarga de enviar la respuesta o delegar al fallback.

### 3.2 Descarga y auto-update
1. Usuario ingresa URL HTTPS en `BotConfigActivity`.
2. `BotRepository.downloadBot()` descarga, valida y guarda el archivo junto con metadata (URL, hash, timestamp).
3. `BotUpdateWorker` corre cada 6 h:
   - Consulta `BotRepository.checkForUpdates()`.
   - Si hay nueva versión, vuelve a descargar y notifica al usuario.
   - Implementa rollback si la validación falla.

---

## 4. Seguridad y aislamiento
- QuickJS se ejecuta en un thread dedicado con `TimeoutExecutor`.
- Storage aislado en `SharedPreferences bot_storage`.
- HTTP restringido a HTTPS y reforzado con OkHttp.
- Rate limiting: 100 ejecuciones/minuto por bot para evitar loops.
- Validación estática previa a cada ejecución:
  - Tamaño ≤ 100 KB.
  - Patrones prohibidos: `eval(`, `Function(`, `__proto__`, `constructor[`, `import(`.
  - `processNotification` debe existir.

---

## 5. Compatibilidad con upstream
- Cambios en `NotificationService` se reducen a ~20 líneas; resto vive en módulos nuevos.
- `PreferencesManager` alberga flags y URLs adicionales, evitando tocar lógica crítica.
- La arquitectura permite mantener sincronización con el repo original de Watomatic sin conflictos mayores.

---

## 6. Dependencias externas
- **QuickJS Android (0.9.2)**: motor JS embebido.
- **OkHttp / Retrofit**: ya presentes en el proyecto, reutilizados para descargas y APIs de bots.
- **WorkManager**: ya disponible; se aprovecha para auto‑updates.

---

## 7. Próximos entregables
- ✅ Documentación base (este archivo + guías en `docs/`).
- ☐ Implementar Strategy Pattern (Fase 1).
- ☐ Integrar QuickJS y providers (Fases 2–4).
- ☐ Desarrollar UI + Worker + pruebas (Fases 5–7).
- ☐ Actualizar métricas y guías tras cada milestone.
