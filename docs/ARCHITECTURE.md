# Arquitectura del sistema BotJS

**Última actualización:** 2025-01-21
**JavaScript Engine:** Mozilla Rhino 1.7.15 (ES5 + ES6 parcial)
**Estado:** ✅ **IMPLEMENTACIÓN COMPLETA** – Migrado de QuickJS a Rhino con interoperabilidad Java↔JS funcional.

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
   │ StaticReplyProvider                           │
   │ OpenAIReplyProvider                           │
   └ BotJsReplyProvider ──► BotJsEngine ──► Rhino  │
                                   │               │
                                   ▼               │
                              bot.js del usuario ◄─┘
```

- Cada provider implementa `ReplyProvider.generateReply`.
- `BotJsReplyProvider` serializa la notificación, invoca el motor Rhino y traduce la salida (`BotResponse`).
- Si BotJS falla, el sistema vuelve al mensaje estático para mantener compatibilidad.
- **Rhino** proporciona interoperabilidad Java↔JavaScript completa mediante `Context.javaToJS()`.

---

## 2. Módulos principales

| Módulo | Estado | Rol | Archivos | Líneas |
|--------|--------|-----|----------|---------|
| `replyproviders.*` | ✅ **Completo** | Interfaz, factory, Static, OpenAI y BotJsReplyProvider implementados | `BotJsReplyProvider.java` | 147 |
| `botjs/BotJsEngine` | ✅ **Migrado a Rhino** | Rhino wrapper con timeouts, ejecución segura e interoperabilidad Java↔JS | `BotJsEngine.java` | 189 |
| `botjs/BotAndroidAPI` | ✅ **Completo** | APIs de logging, storage y HTTP (solo HTTPS) expuestas a bots mediante Rhino | `BotAndroidAPI.java` | - |
| `botjs/BotRepository` | ✅ **Completo** | Download, validación SHA-256 opcional, almacenamiento de `active-bot.js` | `BotRepository.java` | 268 |
| `botjs/BotValidator` | ✅ **Completo** | Validación de tamaño, patrones peligrosos y estructura | `BotValidator.java` | - |
| `workers/BotUpdateWorker` | ✅ **Completo** | WorkManager con auto-updates cada 6 horas | `BotUpdateWorker.java` | 96 |
| `activity/botconfig/*` | ✅ **Completo** | GUI Material 3 completa con enable/disable, URL input, bot info, testing | `BotConfigActivity.kt` | 219 |
| `model/preferences/*` | ✅ **Extendido** | +8 métodos BotJS para persistencia de configuración | `PreferencesManager.java` | +53 |

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
2. `BotRepository.downloadBot(url, sha256)` descarga, valida y guarda el archivo junto con metadata (URL, hash, timestamp).
3. Si se proporciona `sha256`, valida el hash antes de instalar.
4. `BotUpdateWorker` corre cada 6 h:
   - Consulta `BotRepository.checkForUpdates()`.
   - Si hay nueva versión (hash diferente), vuelve a descargar y notifica al usuario.
   - Implementa rollback si la validación falla.

---

## 4. Seguridad y aislamiento
- Rhino se ejecuta en un thread dedicado con `TimeoutExecutor`.
- Storage aislado en `SharedPreferences bot_storage`.
- HTTP restringido a HTTPS y reforzado con OkHttp (operaciones síncronas).
- Rate limiting: 100 ejecuciones/minuto por bot para evitar loops.
- Validación estática previa a cada ejecución:
  - Tamaño ≤ 100 KB.
  - Patrones prohibidos: `eval(`, `Function(`, `__proto__`, `constructor[`, `import(`.
  - `processNotification` debe existir.
- **ES5 Compatibility**: Rhino soporta ES5 completamente, ES6 parcialmente (no `async/await`).

---

## 5. Compatibilidad con upstream
- Cambios en `NotificationService` se reducen a ~20 líneas; resto vive en módulos nuevos.
- `PreferencesManager` alberga flags y URLs adicionales, evitando tocar lógica crítica.
- La arquitectura permite mantener sincronización con el repo original de Watomatic sin conflictos mayores.

---

## 6. Dependencias externas
- **Mozilla Rhino (1.7.15)**: motor JS embebido con interoperabilidad Java↔JS completa (~1.5 MB).
- **OkHttp / Retrofit**: ya presentes en el proyecto, reutilizados para descargas y APIs de bots.
- **WorkManager**: ya disponible; se aprovecha para auto‑updates.

---

## 7. Estado de implementación por fases

- ✅ **Fase 1 – Strategy Pattern**: Factory, providers (Static, OpenAI, BotJS) implementados y funcionando.
- ✅ **Fase 2 – Assets TypeScript**: `bot-types.d.ts` y `example-bot.js` listos en `app/src/main/assets/`.
- ✅ **Fases 3–4 – Rhino + Providers**: `BotJsEngine` migrado a Rhino, `BotAndroidAPI` expuesto mediante `Context.javaToJS()`, `BotJsReplyProvider` integrado.
- ✅ **Fases 5–6 – Bot lifecycle completo**: `BotRepository` (con SHA-256), `BotUpdateWorker`, `BotConfigActivity` y `PreferencesManager` extendido.
- 🟡 **Fase 7 – Testing & Seguridad**: Scaffolding listo, pendiente suites unitarias completas (objetivo >75% cobertura).
- ✅ **Fase 8 – Cierre**: Documentación actualizada, compilación exitosa verificada.

---

## 8. Commits principales

| Commit | Fecha | Descripción | Cambios |
|--------|-------|-------------|---------|
| `[pending]` | 2025-01-21 | Migrar de QuickJS 0.9.2 a Rhino 1.7.15 con interoperabilidad Java↔JS | `BotJsEngine.java` reescrito |
| `745fd66` | 2025-11-19 | Implementar BotJS configuration activity y componentes relacionados | +1005 líneas |
| `6fd8495` | 2025-11-19 | Agregar imports para Context y NotificationData en múltiples clases | 12 archivos |
| `fff410c` | 2025-11-19 | Agregar script check_imports.sh para verificación de imports | +198 líneas |

---

## 9. Herramientas de verificación

### Script de validación de imports
```bash
./scripts/check_imports.sh
```

Verifica automáticamente:
- Imports de `Context` en Activities, Fragments, Services
- Imports de `NotificationData` en ReplyProviders
- Detección de archivos por tipo (Activity, Fragment, Worker, etc.)
- Categorización y reporte de errores/warnings

### Verificación de compilación
Ver documentación completa en `docs/COMPILATION_SUCCESS_GUIDE.md`

---

## 10. Próximas mejoras opcionales

- 🔲 Testing completo: Suites unitarias e integrales para >75% cobertura
- 🔲 Bot marketplace: Lista curada de bots verificados
- 🔲 GUI avanzada: Visor de logs, métricas de performance
- 🔲 Validación mejorada: Verificación de firmas, sandboxing adicional
- 🔲 Documentación de usuario: Guía para usuarios no técnicos
