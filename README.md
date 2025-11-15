# 🪄 Watomagic

Respuesta automática y privada para cualquier app de mensajería en Android. Configurá un mensaje, elegí las apps y dejá que Watomagic conteste por vos mientras viajás, trabajás o migrás a otra plataforma.

---

## 🎯 Qué ofrece
- **Auto-reply universal**: WhatsApp, Telegram, Signal y cualquier app con acciones de respuesta en la notificación.
- **Mensajes personalizados** y plantillas múltiples (contactos específicos, grupos, horarios).
- **Compatibilidad con grupos** sin ruido adicional.
- **Integración OpenAI** opcional para respuestas generadas con IA.
- **Privacidad primero**: todo se procesa en el dispositivo; sin rastreos ni servidores.
- **Código abierto** bajo licencia libre.

📸 [Galería de capturas](./media/screenshots/)

---

## 🚀 Uso rápido
1. **Instalá** la última APK desde [Releases](https://github.com/Parishod/watomagic/releases) o Google Play (si está disponible en tu región).
2. **Concedé permisos**: acceso a notificaciones y ejecución en background.
3. **Configura el mensaje** en *Settings → Auto-reply text*.
4. **Elegí apps y contactos** que recibirán la respuesta.
5. **Activa el servicio** desde el interruptor principal.

> 💡 Tip: añadí un recordatorio en la barra de estado para confirmar que el servicio sigue activo.

---

## ⚙️ Configuraciones clave
- **Respuestas por contacto**: whitelist/blacklist para nombres específicos.
- **Retraso automático**: evita responder en bucle a la misma persona.
- **OpenAI Replies (beta)**: configura tu API Key y modelo (gpt-3.5/4). Si falla, Watomagic usa tu mensaje estático como fallback.
- **Notificación propia**: recibe un resumen cada vez que se envía una respuesta.

Consulta la documentación completa en `docs/` para detalles avanzados y el nuevo plan BotJS.

---

## 🧑‍💻 Guía exprés para desarrolladores
```bash
# requisitos
Java 17, Android SDK 34+, Android NDK (opcional para tests nativos)

# instalar dependencias
./gradlew tasks

# compilar flavour principal
./gradlew :app:assembleDefaultDebug

# ejecutar lint y tests unitarios
./gradlew :app:lint :app:testDefaultDebugUnitTest
```

Si Gradle no encuentra el SDK, crea `local.properties` con:
```
sdk.dir=/ruta/al/Android/Sdk
```

---

## 🛣️ Roadmap BotJS (en progreso)
El plan para convertir a Watomagic en una plataforma de bots descargables está documentado en `docs/PLAN_BOTJS_SYSTEM.md`. Resumen de fases:
1. **Strategy Pattern** (en curso): Providers para respuestas estáticas, OpenAI y futuros bots.
2. **Definiciones TypeScript** y assets de ejemplo.
3. **Motor QuickJS** embebido (`app.cash.quickjs`).
4. **BotJsReplyProvider** con sandbox, rate limiting y validaciones.
5. **Descarga/auto-update** con WorkManager.
6. **UI de configuración** Material 3.
7. **Testing & seguridad** (coverage >75%).
8. **Documentación final** para creadores de bots.

Seguimos priorizando compatibilidad con Watomatic upstream para facilitar futuros merges.

---

## 🆘 Soporte rápido
- Comprueba que las notificaciones estén activas y sin bloqueo biométrico por app.
- Evita modos de ahorro extremo que finalicen el servicio.
- Revisa `Settings → Enabled Apps` para confirmar que la app objetivo está marcada.
- En caso de fallos con OpenAI, verifica tu cuota y modelo configurado.

¿Encontraste un bug? Abrí un issue con logs relevantes o utiliza el canal de soporte indicado en la sección de *Contribuciones* del repositorio.

---

## ⚠️ Nota legal
Watomagic no pertenece ni está afiliado a WhatsApp, Meta, Signal, Telegram ni a ninguna otra empresa de mensajería. Úsala respetando los términos de servicio de cada plataforma.
