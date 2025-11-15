# 🤖 Guía de Usuario: Bots JavaScript en Watomagic

Esta guía te ayudará a configurar y usar bots JavaScript personalizados en Watomagic.

---

## 📖 ¿Qué son los Bots JavaScript?

Los bots JavaScript son scripts que se ejecutan localmente en tu dispositivo Android para procesar notificaciones entrantes y decidir automáticamente cómo responder. Pueden:

- ✅ Responder automáticamente con mensajes personalizados
- ✅ Descartar notificaciones no deseadas
- ✅ Posponer notificaciones para más tarde
- ✅ Consultar servicios externos (APIs de IA, clasificadores, etc.)
- ✅ Aplicar reglas complejas basadas en horarios, apps o contenido

---

## 🚀 Configuración Inicial

### Paso 1: Acceder a la Configuración de Bots

1. Abre Watomagic
2. Ve a **Configuración** (⚙️)
3. Busca **"Configuración de Bots"** o **"Bot Configuration"**
4. Toca para abrir la pantalla de configuración

### Paso 2: Habilitar Bots JavaScript

1. En la pantalla de configuración, activa el switch **"Habilitar Bot JavaScript"**
2. Esto activará el sistema de bots (pero aún necesitas descargar un bot)

### Paso 3: Descargar un Bot

Tienes dos opciones:

#### Opción A: Usar un Bot Existente

1. Obtén la URL HTTPS de un bot (de un repositorio, desarrollador, etc.)
2. Pega la URL en el campo **"URL del Bot"**
3. Toca **"Descargar Bot"**
4. Espera a que se descargue y valide (aparecerá un mensaje de éxito/error)

#### Opción B: Crear tu Propio Bot

Consulta la [Guía para Desarrolladores](./BOT_DEVELOPMENT_GUIDE.md) para aprender a crear tu propio bot.

---

## ⚙️ Configuración Avanzada

### Auto-actualización

- **Habilitar auto-actualización**: El bot se actualizará automáticamente cada 6 horas desde la URL configurada
- **Deshabilitar**: Solo se actualizará cuando lo hagas manualmente

### Probar el Bot

1. Toca el botón **"Probar Bot"** en la pantalla de configuración
2. Se ejecutará una notificación de prueba
3. Revisa los logs para ver cómo respondió el bot

### Ver Logs

Los logs del bot te ayudan a entender qué está haciendo:
- Toca **"Ver Logs del Bot"** para ver el historial de ejecuciones
- Los logs muestran errores, decisiones y mensajes de depuración

### Eliminar Bot

Si quieres eliminar el bot instalado:
1. Toca **"Eliminar Bot"** (botón rojo)
2. Confirma la eliminación
3. El bot se eliminará y volverás a usar respuestas estáticas o OpenAI

---

## 🔄 Prioridad de Respuestas

Watomagic usa el siguiente orden de prioridad para decidir cómo responder:

1. **Bot JavaScript** (si está habilitado y descargado)
2. **OpenAI/IA** (si está configurado)
3. **Respuesta estática** (mensaje personalizado)

Esto significa que si tienes un bot JavaScript activo, siempre se usará primero.

---

## ⚠️ Solución de Problemas

### El bot no se descarga

**Problema**: Error al descargar el bot desde la URL

**Soluciones**:
- ✅ Verifica que la URL sea HTTPS (no HTTP)
- ✅ Asegúrate de que el servidor esté accesible
- ✅ Verifica que el archivo sea menor a 100KB
- ✅ Revisa que el bot tenga la función `processNotification`

### El bot no responde

**Problema**: El bot está instalado pero no genera respuestas

**Soluciones**:
- ✅ Verifica que el bot esté habilitado en la configuración
- ✅ Revisa los logs del bot para ver errores
- ✅ Asegúrate de que el bot retorne una acción válida (`REPLY`, `DISMISS`, `KEEP`, `SNOOZE`)
- ✅ Si usas `REPLY`, verifica que incluya `replyText`

### El bot tarda mucho en responder

**Problema**: Las respuestas automáticas son lentas

**Soluciones**:
- ✅ Los bots tienen un timeout de 5 segundos
- ✅ Si tu bot consulta APIs externas, optimiza las llamadas
- ✅ Revisa los logs para ver dónde se está demorando
- ✅ Considera usar caché con `Android.storageSet()` para evitar llamadas repetidas

### El bot genera errores

**Problema**: El bot falla al ejecutarse

**Soluciones**:
- ✅ Revisa los logs del bot para ver el error específico
- ✅ Verifica la sintaxis JavaScript del bot
- ✅ Asegúrate de que todas las APIs usadas estén disponibles
- ✅ Prueba el bot con el botón "Probar Bot" para ver errores en tiempo real

---

## 🔒 Seguridad

### Validaciones Automáticas

Watomagic valida automáticamente los bots para proteger tu dispositivo:

- ✅ **Solo URLs HTTPS**: No se permiten conexiones HTTP no seguras
- ✅ **Tamaño máximo**: Los bots no pueden exceder 100KB
- ✅ **Patrones peligrosos**: Se bloquean funciones como `eval()`, `Function()`, etc.
- ✅ **Timeout**: Los bots se cancelan automáticamente después de 5 segundos
- ✅ **Rate limiting**: Máximo 100 ejecuciones por minuto

### Qué NO pueden hacer los bots

Los bots están en un sandbox seguro y **NO pueden**:
- ❌ Acceder al sistema de archivos de Android
- ❌ Leer contactos o datos de otras apps
- ❌ Modificar configuraciones del sistema
- ❌ Ejecutar código peligroso (eval, Function, etc.)

### Recomendaciones

- ✅ Solo descarga bots de fuentes confiables
- ✅ Revisa el código del bot antes de usarlo (si tienes acceso)
- ✅ Usa URLs HTTPS de servidores seguros
- ✅ Mantén el auto-update habilitado para recibir correcciones de seguridad

---

## 📝 Ejemplos de Uso

### Bot Simple de Auto-respuesta

Un bot que responde automáticamente a WhatsApp:

```javascript
async function processNotification(notification) {
    if (notification.appPackage === 'com.whatsapp') {
        return {
            action: 'REPLY',
            replyText: 'Estoy ocupado ahora. Te respondo pronto!'
        };
    }
    return { action: 'KEEP' };
}
```

### Bot con Rate Limiting

Un bot que solo responde una vez por hora:

```javascript
async function processNotification(notification) {
    const lastReply = Android.storageGet('lastAutoReply');
    const now = Android.getCurrentTime();
    
    if (!lastReply || now - parseInt(lastReply) > 3600000) {
        Android.storageSet('lastAutoReply', now.toString());
        return {
            action: 'REPLY',
            replyText: 'Gracias por tu mensaje. Te responderé pronto.'
        };
    }
    
    return { action: 'KEEP' };
}
```

### Bot que Bloquea Apps

Un bot que descarta notificaciones de apps específicas:

```javascript
async function processNotification(notification) {
    const blockedApps = ['com.spam.app', 'com.annoying.app'];
    
    if (blockedApps.includes(notification.appPackage)) {
        return {
            action: 'DISMISS',
            reason: 'App bloqueada'
        };
    }
    
    return { action: 'KEEP' };
}
```

Para más ejemplos y documentación completa, consulta la [Guía para Desarrolladores](./BOT_DEVELOPMENT_GUIDE.md).

---

## ❓ Preguntas Frecuentes

### ¿Puedo usar múltiples bots a la vez?

No, solo puedes tener un bot activo a la vez. Si necesitas funcionalidad de múltiples bots, combínalos en un solo script.

### ¿Los bots funcionan sin conexión a internet?

Depende del bot. Si el bot solo usa lógica local (sin llamadas HTTP), funcionará sin internet. Si consulta APIs externas, necesitará conexión.

### ¿Puedo editar el bot después de descargarlo?

No directamente desde la app. Debes editar el archivo en el servidor y luego actualizar (manual o automático).

### ¿Qué pasa si el bot tiene un error?

Si el bot falla, Watomagic usará el siguiente método de respuesta disponible (OpenAI o estático) como fallback.

### ¿Los bots consumen mucha batería?

No significativamente. Los bots se ejecutan solo cuando llega una notificación y tienen un timeout de 5 segundos.

---

## 📚 Recursos Adicionales

- **[Guía para Desarrolladores](./BOT_DEVELOPMENT_GUIDE.md)** - Aprende a crear tus propios bots
- **[Referencia de API](./BOT_API_REFERENCE.md)** - Documentación completa de todas las APIs
- **[Plan de Implementación](./PLAN_BOTJS_SYSTEM.md)** - Detalles técnicos del sistema

---

**¿Necesitas ayuda?** Abre un issue en el repositorio de GitHub o consulta la documentación técnica.
