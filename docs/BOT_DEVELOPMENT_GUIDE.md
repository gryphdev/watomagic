# Guía de desarrollo de bots BotJS

> ⚠️ **Estado del proyecto**: El runtime BotJS todavía no está integrado en Watomagic. Este documento permite adelantar el diseño de bots para que, una vez se habilite el feature, puedan importarse sin cambios.

## 1. Panorama general
Los bots son scripts JavaScript/TypeScript auto‑contenidos que implementan `processNotification(notification: NotificationData): Promise<BotResponse>`. Se ejecutan dentro de QuickJS, con acceso a un objeto global `Android` que expone APIs de storage, red y utilidades controladas.

## 2. Requisitos
- Node.js 18+ para linting y bundling local.
- Conocimientos básicos de TypeScript y asincronía (async/await).
- Hosting HTTPS para publicar `bot.js` (GitHub Pages, Cloudflare Pages, S3 + CloudFront, etc.).
- Compromiso con el límite de **100 KB** por script.

## 3. Kit de inicio
1. Descargar la definición de tipos `bot-types.d.ts` (se publicará en `/app/src/main/assets/`).
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

## 5. Ejemplo mínimo
```typescript
/// <reference path="../types/bot-types.d.ts" />

export async function processNotification(notification: NotificationData): Promise<BotResponse> {
  Android.log('info', `Revisando ${notification.appPackage}`);

  if (notification.appPackage === 'com.whatsapp') {
    const lastSent = Android.storageGet('lastReplyTs');
    const now = Android.getCurrentTime();

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
- **HTTP**: `await Android.httpRequest({ url, method, headers, body })` siempre devuelve texto; parsearlo con `JSON.parse`.
- **Fallback**: en caso de error lanzar `throw new Error('motivo')`; Watomagic retornará al mensaje estático.

## 7. Testing local
- Ejecutar con [quickjs-emscripten](https://github.com/justjake/quickjs-emscripten) o Node.js simulando las APIs:
  ```bash
  node tools/mock-runner.js dist/bot.js fixtures/whatsapp.json
  ```
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
- [ ] Sin uso de `eval`, `Function`, `import()` dinámico ni `XMLHttpRequest`.
- [ ] Manejo de errores con try/catch en llamadas HTTP.
- [ ] Respeta el timeout de 5 s (evitar bucles intensivos).
- [ ] Se documentó propósito y versión al inicio del archivo:
  ```javascript
  /**
   * Bot: Reglas de guardia nocturna
   * Versión: 0.1.0
   * Autor: tu@correo
   */
  ```

## 10. Próximos recursos
- [Plan maestro BotJS](./PLAN_BOTJS_SYSTEM.md)
- [Referencia completa de APIs](./BOT_API_REFERENCE.md)
- [Guía para usuarios finales](./BOT_USER_GUIDE.md)
