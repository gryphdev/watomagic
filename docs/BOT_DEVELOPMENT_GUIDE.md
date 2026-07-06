# Guía de desarrollo de bots BotJS

**Estado:** ✅ Implementado y funcional
**JavaScript Engine:** Mozilla Rhino 1.7.15 (ES5 + ES6 parcial)

## 1. Panorama general
Los bots son scripts JavaScript auto‑contenidos que implementan `processNotification(notification: NotificationData): BotResponse`. Se ejecutan dentro de Mozilla Rhino, con acceso a un objeto global `Android` que expone APIs de storage, red y utilidades controladas.

**IMPORTANTE:** Rhino soporta ES5 completamente y ES6 parcialmente. No soporta `async/await`. Todas las operaciones son síncronas.

## 2. Requisitos
- Node.js 18+ para linting y bundling local.
- Conocimientos básicos de JavaScript ES5 (sin `async/await`).
- Hosting HTTPS para publicar `bot.js` (GitHub Pages, Cloudflare Pages, S3 + CloudFront, etc.).
- Compromiso con el límite de **100 KB** por script.
- **Restricciones de sintaxis ES5**: No usar `async/await`, preferir `var` sobre `const`/`let`, usar concatenación `+` en lugar de template literals.

## 3. Kit de inicio
1. Copiar `app/src/main/assets/bot-types.d.ts` del repositorio.
2. Crear un nuevo proyecto:
   ```bash
   mkdir watomagic-bot && cd watomagic-bot
   npm init -y
   npm install --save-dev typescript esbuild eslint
   ```
3. Copiar `bot-types.d.ts` en `types/` y referenciarlo desde `tsconfig.json`.

## 4. Estructura recomendada
```
watomagic-bot/
├── src/
│   ├── bot.ts              # Punto de entrada con processNotification
│   └── rules/
│       └── spam.ts
├── dist/
│   └── bot.js              # Salida final (≤100 KB)
├── types/
│   └── bot-types.d.ts
└── package.json
```

## 5. Ejemplo mínimo (ES5 compatible con Rhino)
```javascript
/**
 * Bot de respuesta automática simple
 * Compatible con Rhino (ES5)
 */
function processNotification(notification) {
  Android.log('info', '[BOT] Revisando ' + notification.appPackage);

  if (notification.appPackage === 'com.whatsapp') {
    var lastSent = Android.storageGet('lastReplyTs');
    var now = Android.getCurrentTime();

    if (!lastSent || now - Number(lastSent) > 15 * 60 * 1000) {
      Android.storageSet('lastReplyTs', now.toString());
      return { action: 'REPLY', replyText: 'Estoy reunido, respondo luego.' };
    }
  }

  return { action: 'KEEP' };
}
```

## 6. Patrones útiles
- **Rate limiting**: almacenar marca temporal en `Android.storage*`.
- **Lists dinámicas**: usar `storageKeys()` para enumerar claves por app.
- **HTTP síncrono**: `Android.httpRequest({ url, method, headers, body })` bloquea hasta recibir respuesta (máximo 4 segundos). Siempre devuelve texto; parsearlo con `JSON.parse`.
- **Fallback**: en caso de error lanzar `throw new Error('motivo')`; Watomagic retornará al mensaje estático.
- **ES5 syntax**: Usar `var` en lugar de `const`/`let`, concatenación con `+` en lugar de template literals, `function` en lugar de arrow functions.

## 7. Testing local
- Ejecutar con Node.js simulando las APIs:
  ```bash
  node tools/mock-runner.js dist/bot.js fixtures/whatsapp.json
  ```
- **IMPORTANTE**: Asegurar compatibilidad ES5 antes de publicar. Evitar `async/await`, `const`/`let` modernos, template literals, arrow functions.
- Incluir pruebas unitarias simples para reglas críticas.

## 8. Empaquetado
1. Transpilar con esbuild:
   ```bash
   npx esbuild src/bot.ts --bundle --minify --platform=browser --outfile=dist/bot.js
   ```
2. Verificar tamaño:
   ```bash
   wc -c dist/bot.js # debe ser <= 102400
   ```
3. Publicar `dist/bot.js` en un hosting HTTPS y tomar la URL final.

## 9. Checklist de entrega
- [ ] `processNotification` exportado en el scope global.
- [ ] **Compatible con ES5**: Sin `async/await`, sin `const`/`let` modernos, sin template literals, sin arrow functions.
- [ ] Sin uso de `eval`, `Function`, `import()` dinámico ni `XMLHttpRequest`.
- [ ] Manejo de errores con try/catch en llamadas HTTP síncronas.
- [ ] Respeta el timeout de 5 s (evitar bucles intensivos).
- [ ] Se documentó propósito y versión al inicio del archivo:
  ```javascript
  /**
   * Bot: Reglas de guardia nocturna
   * Versión: 0.1.0
   * Autor: tu@correo
   * Engine: Rhino (ES5)
   */
  ```

## 10. Lectura de imágenes (opcional)

Si el usuario activó acceso a adjuntos, `notification.attachments` contiene metadatos. Para placeholders de WhatsApp:

```javascript
if (notification.isMediaPlaceholder && Android.hasWhatsAppMediaAccess()) {
  var img = Android.readLatestWhatsAppImage(notification.timestamp);
  // img es Base64 JPEG o null
}
```

Las respuestas siguen siendo solo texto (`replyText`).

## 11. Recursos
- [Referencia de APIs](./BOT_API_REFERENCE.md)
- [Guía para usuarios](./BOT_USER_GUIDE.md)
- [Arquitectura](./ARCHITECTURE.md)
