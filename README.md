# 🪄 Watomagic - Respuesta automática inteligente para mensajería

Watomagic envía respuestas automáticas a tus contactos en apps de mensajería. Útil para migrar de WhatsApp, contestador automático en vacaciones, o automatizar respuestas con IA.

### 📸 [Capturas de pantalla](./media/screenshots/)

---

## ✨ Características

- ✅ **Respuestas automáticas** en WhatsApp, Signal, Telegram y más
- 🤖 **BotJS**: Ejecuta bots JavaScript personalizados para lógica avanzada
- 🧠 **IA integrada**: Respuestas inteligentes con OpenAI
- ✏️ **Mensajes personalizados**: Edita tus respuestas estáticas
- 👥 **Soporte para grupos**: Configurable
- 🔄 **Auto-actualización**: Los bots se actualizan automáticamente
- 🔒 **Privacidad**: Todo se ejecuta localmente en tu dispositivo

---

## 🚀 Inicio rápido

### 1. Instalación
- Descarga la app desde [releases](../../releases) o compila desde código
- Concede permisos de acceso a notificaciones
- Habilita el servicio en la app

### 2. Configuración básica
1. Abre Watomagic
2. Escribe tu mensaje de respuesta automática
3. Selecciona las apps donde quieres activar respuestas
4. Activa el switch de "Auto reply ON"

### 3. Respuestas con IA (OpenAI)
1. Ve a **Settings → General Settings**
2. Activa "Enable AI Auto-Replies"
3. Ingresa tu API Key de OpenAI
4. Selecciona el modelo (gpt-3.5-turbo, gpt-4, etc.)

### 4. Bots JavaScript personalizados
1. Ve a **Settings → Bot Configuration**
2. Activa "Enable BotJS"
3. Ingresa la URL HTTPS de tu bot (ej: `https://example.com/bot.js`)
4. Presiona "Download Bot"
5. El bot se ejecutará automáticamente para cada notificación

---

## 🤖 Sistema BotJS

Watomagic permite ejecutar bots JavaScript personalizados que procesan notificaciones y deciden cómo responder.

### Características de BotJS

- **Ejecución local**: Los bots se ejecutan en tu dispositivo usando QuickJS
- **APIs disponibles**: Storage, HTTP requests, logging, utilidades
- **Auto-actualización**: Los bots se actualizan automáticamente cada 6 horas
- **Seguridad**: Validación de código, solo HTTPS, rate limiting

### Ejemplo de bot

```javascript
async function processNotification(notification) {
    // Bloquear apps específicas
    if (notification.appPackage === 'com.annoying.app') {
        return { action: 'DISMISS' };
    }

    // Auto-respuesta con rate limiting
    if (notification.appPackage === 'com.whatsapp') {
        const lastReply = Android.storageGet('lastAutoReply');
        const now = Android.getCurrentTime();
        
        if (!lastReply || now - parseInt(lastReply) > 3600000) {
            Android.storageSet('lastAutoReply', now.toString());
            return {
                action: 'REPLY',
                replyText: 'Estoy ocupado. Te respondo pronto!'
            };
        }
    }

    // Por defecto: mantener notificación
    return { action: 'KEEP' };
}
```

### APIs disponibles

- `Android.log(level, message)` - Logging
- `Android.storageGet(key)` / `Android.storageSet(key, value)` - Persistencia
- `Android.httpRequest(options)` - Llamadas HTTP
- `Android.getCurrentTime()` - Tiempo actual
- `Android.getAppName(packageName)` - Nombre de app

### Documentación completa

Ver [docs/BOT_DEVELOPMENT_GUIDE.md](./docs/BOT_DEVELOPMENT_GUIDE.md) para la guía completa de desarrollo de bots.

---

## 📋 Apps soportadas

- WhatsApp
- Signal
- Telegram
- Facebook Messenger
- Y más...

---

## 🔧 Solución de problemas

### La respuesta automática no funciona

1. Verifica que el servicio esté habilitado en Watomagic
2. Asegúrate de tener permisos de acceso a notificaciones
3. Desactiva el bloqueo biométrico específico de la app para Watomagic
4. Verifica que las notificaciones estén habilitadas en la app de mensajería

### El bot no se descarga

- Verifica que la URL sea HTTPS (no HTTP)
- Asegúrate de que el servidor esté accesible
- Revisa los logs en "View Bot Logs"

### OpenAI no responde

- Verifica que tu API Key sea válida
- Revisa tu cuota de OpenAI
- Verifica la conexión a internet

---

## 🛠️ Desarrollo

### Compilar desde código

```bash
git clone https://github.com/tu-usuario/watomagic.git
cd watomagic
./gradlew assembleDebug
```

### Estructura del proyecto

```
app/src/main/java/com/parishod/watomagic/
├── replyproviders/      # Sistema de providers (Strategy Pattern)
│   ├── ReplyProvider.java
│   ├── OpenAIReplyProvider.java
│   ├── StaticReplyProvider.java
│   └── BotJsReplyProvider.java
├── botjs/               # Sistema BotJS
│   ├── BotJsEngine.java
│   ├── BotAndroidAPI.java
│   ├── BotRepository.java
│   └── BotValidator.java
└── service/
    └── NotificationService.java
```

### Requisitos

- Android SDK 24+ (Android 7.0)
- Java 17
- Gradle 8.0+

---

## 📝 Licencia

Ver [LICENSE](./LICENSE)

---

## ⚠️ Importante

Esta app no está asociada con WhatsApp, Facebook, Signal ni ninguna otra empresa. Es un proyecto de código abierto independiente.

---

## 🤝 Contribuir

Las contribuciones son bienvenidas. Por favor:
1. Fork el proyecto
2. Crea una rama para tu feature
3. Commit tus cambios
4. Push a la rama
5. Abre un Pull Request

---

## 📚 Documentación adicional

- [Guía de desarrollo de bots](./docs/BOT_DEVELOPMENT_GUIDE.md)
- [Referencia de API de bots](./docs/BOT_API_REFERENCE.md)
- [Arquitectura del sistema](./docs/ARCHITECTURE.md)

---

**¿Necesitas ayuda?** Abre un [issue](../../issues) o consulta la documentación.
