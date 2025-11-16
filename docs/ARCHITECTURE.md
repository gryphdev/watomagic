# Arquitectura del sistema BotJS

**Última actualización:** 2025-11-15  
**Estado:** Arquitectura en marcha – Fase 1 en progreso, Fase 2 completada, runtime QuickJS (Fase 3) en desarrollo.

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
| `replyproviders.*` | 🟡 En progreso | Interfaz, factory, Static y OpenAI providers ya refactorizados; pendiente BotJsReplyProvider/otros. |
| `botjs/BotJsEngine` | 🟡 Implementado (scaffolding) | QuickJS wrapper listo con timeouts; aguardando integración con provider y bindings finales. |
| `botjs/BotAndroidAPI` | 🟡 Implementado (scaffolding) | Exposición controlada de logging, storage y HTTP (solo HTTPS) para los bots. |
| `botjs/BotRepository` | Diseño aprobado | Descargar, validar y almacenar `active-bot.js`. |
| `botjs/BotValidator` | 🟡 Implementado | Reglas de tamaño/patrones ya codificadas. |
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
- 🟡 Fase 1 – Strategy Pattern: añadir pruebas unitarias de factory/providers y crear `BotJsReplyProvider` que consuma el runtime QuickJS.
- ✅ Fase 2 – Assets TypeScript: `bot-types.d.ts` y `example-bot.js` listos en `app/src/main/assets/`.
- 🟡 Fases 3–4 – QuickJS + Providers: conectar `BotJsEngine/BotAndroidAPI` con bindings reales, exponer `Android` al sandbox e integrar el nuevo provider.
- ☐ Fases 5–6 – Bot lifecycle completo: `BotRepository`, `BotUpdateWorker`, `BotConfigActivity` y ajustes en `PreferencesManager`/UI.
- ☐ Fase 7 – Testing & Seguridad: suites unitarias/integrales, validaciones adicionales y métricas >75 % cobertura.
- ☐ Fase 8 – Cierre: actualizar documentación, métricas y checklist final para la habilitación de BotJS en producción.
