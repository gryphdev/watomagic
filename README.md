# 🪄 Watomagic - Respuesta automática para apps de mensajería

Watomagic envía una respuesta automática a todos los que te contacten en apps de mensajería. Es especialmente útil si estás planeando migrar de estas apps, pero también podés usarlo como un contestador automático cuando estás de vacaciones.

### 📸 [Capturas de pantalla](./media/screenshots/)

| [<img src="/media/screenshots/1.png" alt="Captura 1">][scr-page-link] | [<img src="/media/screenshots/2.png" alt="Captura 2">][scr-page-link] | [<img src="/media/screenshots/3.png" alt="Captura 3">][scr-page-link] |
|:---:|:---:|:---:|

[**❯ Ver más capturas**](./media/screenshots/)

---

## ✨ Características

- ✅ **Respuesta automática** en todas las apps de mensajería soportadas
- ✏️ **Personalizá tu mensaje** de respuesta automática
- 👥 **Funciona en grupos** también
- 🔒 **Respeto total por tu privacidad**
  - Sin análisis ni rastreo de datos
- 🆓 **Gratis y código abierto**

## 🧩 Plataforma BotJS (en desarrollo)

Estamos trabajando en un sistema de bots JavaScript descargables (`BotJS`) que permitirá personalizar la lógica de respuesta de forma ilimitada. Aún no está disponible en las builds públicas, pero ya podés revisar la arquitectura y preparar tus scripts.

- Descarga segura de `bot.js` alojados en HTTPS con validación de tamaño y patrones.
- Motor QuickJS sandbox con APIs controladas (`Android.log`, `Android.httpRequest`, storage, etc.).
- Pantalla dedicada (`BotConfigActivity`) para habilitar/deshabilitar bots, probarlos y configurar auto‑updates.
- WorkManager verificando nuevas versiones cada 6 horas con rollback automático ante fallos.

Documentación inicial:
- [Plan maestro y roadmap](./docs/PLAN_BOTJS_SYSTEM.md)
- [Guía de uso para personas usuarias](./docs/BOT_USER_GUIDE.md)
- [Guía de desarrollo de bots](./docs/BOT_DEVELOPMENT_GUIDE.md)
- [API Reference + arquitectura](./docs/BOT_API_REFERENCE.md) · [Arquitectura detallada](./docs/ARCHITECTURE.md)

---

## 💡 ¿Para qué sirve?

Los cambios recientes en la política de privacidad de WhatsApp generaron una migración masiva hacia apps más respetuosas de la privacidad como Signal y otras. Pero la mayoría de nosotros encuentra difícil eliminar WhatsApp porque todo el mundo lo usa.

**Watomagic facilita tu migración** dejando que tus contactos sepan automáticamente que te mudaste a otra app. Simplemente configurá un mensaje de respuesta automática como *"Ya no uso WhatsApp. Por favor contactame por Signal…"* y dejá que la app haga el trabajo por vos.

> ⚠️ **Importante:** Esta app no está asociada con ninguna empresa, incluyendo WhatsApp, Facebook o Signal.

---

## 🔧 Solución de problemas

### La respuesta automática no funciona aunque Watomagic esté habilitado

Watomagic depende de las notificaciones para funcionar. La mayoría de los usuarios ya tiene las notificaciones habilitadas, así que debería funcionar de entrada. Si no funciona, asegurate de que:

- ✅ Las notificaciones estén habilitadas
- ✅ El bloqueo biométrico específico de la app esté deshabilitado para Watomagic

---

## ❓ Preguntas frecuentes

### ¿Por qué no usar una cuenta de WhatsApp Business para respuestas automáticas?

No podés usar una cuenta business sin aceptar la nueva política de privacidad que todos están tratando de evitar.

### ¿Estará disponible para iOS en el futuro?

Esta app depende de la función de respuestas rápidas desde notificaciones específica de Android. Esto probablemente no sea posible en iOS.

---

## 📚 Documentación y recursos

- [Capturas y branding](./media/screenshots/)
- [docs/PLAN_BOTJS_SYSTEM.md](./docs/PLAN_BOTJS_SYSTEM.md) — estado del proyecto BotJS
- [docs/BOT_USER_GUIDE.md](./docs/BOT_USER_GUIDE.md) — guía operativa para la nueva funcionalidad
- [docs/BOT_DEVELOPMENT_GUIDE.md](./docs/BOT_DEVELOPMENT_GUIDE.md) — cómo crear tus propios scripts
- [docs/BOT_API_REFERENCE.md](./docs/BOT_API_REFERENCE.md) y [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) — contrato técnico

---

[scr-page-link]: ./media/screenshots/
