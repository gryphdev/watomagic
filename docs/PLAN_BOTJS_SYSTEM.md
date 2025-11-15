# Plan de Implementación: Sistema de Bots JavaScript Descargables para Watomagic

**Fecha**: 2025-11-12
**Objetivo**: Crear una plataforma de plugins JavaScript descargables que permita a los usuarios personalizar la lógica de respuesta automática de notificaciones.

---

## 📊 Estado Actual de Implementación

**Fecha de Evaluación**: 2025-11-15
**Estado General**: ❌ **0% IMPLEMENTADO**

### Resumen Ejecutivo
El análisis del código actual revela que **NINGÚN componente del plan BotJS ha sido implementado todavía**. El proyecto se encuentra en su estado original con la arquitectura monolítica existente de Watomatic.

### Estado por Fase
- ❌ **Fase 1**: Strategy Pattern - NO implementado
- ❌ **Fase 2**: Interfaces TypeScript - NO implementado
- ❌ **Fase 3**: QuickJS Integration - NO implementado
- ❌ **Fase 4**: BotJsReplyProvider - NO implementado
- ❌ **Fase 5**: Download & Update System - NO implementado
- ❌ **Fase 6**: GUI Configuration - NO implementado
- ❌ **Fase 7**: Testing & Security - NO implementado
- ✅ **Fase 8**: Documentación - PARCIALMENTE (plan + guías listas, feature aún no implementado)

### Componentes Clave Faltantes
- **NotificationService.sendReply()**: 149 líneas monolíticas (necesita refactoring a ~20 líneas)
- **ReplyProvider system**: No existe (0/4 providers creados)
- **BotJS engine**: No existe (0/7 clases botjs creadas)
- **QuickJS dependency**: No agregada en build.gradle.kts
- **GUI**: BotConfigActivity no existe
- **Assets**: Directorio `/assets/` no existe

### Ventajas del Estado Actual
✅ WorkManager ya incluido como dependencia
✅ Retrofit/OkHttp ya incluidos (reutilizables para BotRepository)
✅ Arquitectura actual bien definida (facilita refactoring)
✅ OpenAI funcionando correctamente (referencia para providers)

### Documentación publicada (2025-11-15)
- `docs/BOT_USER_GUIDE.md`: guía operativa para habilitar y probar bots descargables.
- `docs/BOT_DEVELOPMENT_GUIDE.md`: paso a paso para crear bots en TypeScript.
- `docs/BOT_API_REFERENCE.md`: contrato definitivo de `NotificationData`, `BotResponse` y las APIs expuestas.
- `docs/ARCHITECTURE.md`: resumen técnico del flujo, módulos y medidas de seguridad.

---

## 🎯 Visión General

### Concepto Principal
Transformar Watomagic en una **plataforma de bots extensible** donde:
- Los usuarios pueden **configurar una URL** desde donde descargar `bot.js`/`bot.ts`
- El bot se ejecuta **localmente** en el dispositivo Android usando QuickJS
- El bot puede **consultar APIs externas** (incluyendo OpenAI, Claude, o cualquier servicio)
- Sistema de **auto-actualización** para mantener los bots actualizados
- **Interfaces TypeScript** bien definidas para la comunicación
- **Preservar compatibilidad** con el proyecto upstream Watomatic

### Arquitectura Propuesta

```
Notificación WhatsApp → NotificationService
    ↓
ReplyProviderFactory (Strategy Pattern)
    ├─→ StaticReplyProvider (mensajes estáticos)
    ├─→ OpenAIReplyProvider (IA de OpenAI)
    └─→ BotJsReplyProvider (bot.js personalizado)
         ↓
         BotJsEngine (QuickJS)
         ├─→ AndroidAPI (storage, http, log, utils)
         └─→ bot.js del usuario
              ↓
              Puede llamar APIs externas
              ↓
              Retorna acción (REPLY, DISMISS, KEEP, SNOOZE)
    ↓
sendActualReply() → Respuesta a WhatsApp
```

---

## ✅ Decisiones Confirmadas

**Fecha de Confirmación**: 2025-11-15

### Motor JavaScript
- **Seleccionado**: QuickJS (`app.cash.quickjs:quickjs-android:0.9.2`)
- **Razones**:
  - Ligero (~2MB vs ~7MB de V8)
  - Soporte ES2020 completo
  - Bien mantenido por Cash App
  - Menor impacto en tamaño de APK

### Orden de Implementación
- **Confirmado**: Empezar con Fase 1 (Strategy Pattern)
- **Razones**:
  - Crítico para mantener compatibilidad con upstream Watomatic
  - Minimiza merge conflicts futuros
  - Mejora inmediata de calidad de código
  - Prerequisito arquitectónico para BotJS

### Alcance del Proyecto
- **Confirmado**: Implementar las 8 fases completas
- **Estimación**: 20-27 horas de desarrollo
- **Entregables**:
  - Sistema BotJS funcional completo
  - GUI de configuración Material 3
  - Sistema de auto-updates
  - Tests con >75% cobertura
  - Documentación completa para desarrolladores

### Seguridad
- ✅ Solo URLs HTTPS (rechazar http://)
- ✅ Validación de tamaño: máx 100KB por bot
- ✅ Blacklist de patrones peligrosos
- ✅ Timeout: 5 segundos por ejecución
- ✅ Rate limiting: 100 ejecuciones/minuto
- ✅ Sandbox: thread separado, sin acceso filesystem
- ⚠️ Firma digital: opcional para fase futura

### Auto-updates
- ✅ WorkManager cada 6 horas
- ✅ Comparación SHA-256 hash
- ✅ Notificación al usuario
- ✅ Opción de deshabilitar en settings
- ✅ Rollback si nueva versión falla validación

### UI/UX
- ✅ Diseño Material 3 con 4 cards principales
- ✅ Feedback visual claro (errores/éxitos)
- ✅ Botón de test bot
- ✅ Integración en settings existentes

### Testing
- ✅ Unit tests para cada provider
- ✅ Integration test end-to-end
- ✅ Tests de seguridad (patrones peligrosos)
- ✅ Mock tests para QuickJS y HTTP
- ✅ Objetivo: >75% cobertura

---

## 📋 Fases de Implementación

## Fase 1: Refactorizar Arquitectura (Strategy Pattern) ⚡ CRÍTICO

**Por qué primero**: Esta refactorización es fundamental para:
1. Mantener compatibilidad con upstream Watomatic
2. Minimizar conflictos en futuros merges
3. Permitir extensibilidad limpia del sistema

### 1.1 Crear interfaz ReplyProvider
**Archivo**: `/app/src/main/java/com/parishod/watomatic/replyproviders/ReplyProvider.java`

```java
public interface ReplyProvider {
    /**
     * Genera una respuesta para una notificación entrante
     * @param context Contexto de Android
     * @param incomingMessage Mensaje recibido en la notificación
     * @param notificationData Datos completos de la notificación
     * @param callback Callback para devolver la respuesta o error
     */
    void generateReply(Context context,
                      String incomingMessage,
                      NotificationData notificationData,
                      ReplyCallback callback);

    interface ReplyCallback {
        void onSuccess(String reply);
        void onFailure(String error);
    }
}
```

### 1.2 Extraer OpenAI a provider separado
**Archivo**: `/app/src/main/java/com/parishod/watomatic/replyproviders/OpenAIReplyProvider.java`

**Acción**:
- Mover las 140+ líneas de lógica OpenAI desde `NotificationService.sendReply()` (líneas 151-277 exactas)
- Mantener exactamente la misma funcionalidad
- Preservar el manejo de errores y reintentos existente
- **Nota**: El método sendReply() completo ocupa líneas 138-286 (149 líneas totales)

**Beneficio**: Aísla la lógica de OpenAI en su propio módulo

### 1.3 Crear StaticReplyProvider
**Archivo**: `/app/src/main/java/com/parishod/watomatic/replyproviders/StaticReplyProvider.java`

Encapsular la lógica de respuestas estáticas (el comportamiento original de Watomatic).

### 1.4 Crear ReplyProviderFactory
**Archivo**: `/app/src/main/java/com/parishod/watomatic/replyproviders/ReplyProviderFactory.java`

```java
public class ReplyProviderFactory {
    public static ReplyProvider getProvider(PreferencesManager prefs) {
        if (prefs.isBotJsEnabled()) {
            return new BotJsReplyProvider();
        } else if (prefs.isOpenAIRepliesEnabled()) {
            return new OpenAIReplyProvider();
        }
        return new StaticReplyProvider();
    }
}
```

### 1.5 Simplificar NotificationService.sendReply()
**Objetivo**: Reducir de 150 líneas a ~20 líneas

```java
private void sendReply(StatusBarNotification sbn) {
    final NotificationWear notificationWear = NotificationUtils.extractWearNotification(sbn);
    if (notificationWear.getRemoteInputs().isEmpty()) return;

    PreferencesManager prefs = PreferencesManager.getPreferencesInstance(this);
    String incomingMessage = extractIncomingMessage(sbn);
    String fallbackReply = CustomRepliesData.getInstance(this).getTextToSendOrElse();

    ReplyProvider provider = ReplyProviderFactory.getProvider(prefs);
    provider.generateReply(this, incomingMessage, notificationData,
        new ReplyProvider.ReplyCallback() {
            @Override
            public void onSuccess(String reply) {
                sendActualReply(sbn, notificationWear, reply);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Reply generation failed: " + error);
                sendActualReply(sbn, notificationWear, fallbackReply);
            }
        });
}
```

**Resultado**: Merge conflicts mínimos con upstream en futuros updates.

---

## Fase 2: Definir Interfaces TypeScript 📝

### 2.1 Crear definiciones de tipos
**Archivo**: `/app/src/main/assets/bot-types.d.ts`

```typescript
/**
 * Datos de la notificación entrante
 */
interface NotificationData {
    id: number;
    appPackage: string;
    title: string;
    body: string;
    timestamp: number;
    isGroup: boolean;
    actions: string[];
}

/**
 * Respuesta esperada del bot
 */
interface BotResponse {
    action: 'KEEP' | 'DISMISS' | 'REPLY' | 'SNOOZE';
    replyText?: string;        // Requerido si action = 'REPLY'
    snoozeMinutes?: number;    // Requerido si action = 'SNOOZE'
    reason?: string;           // Opcional: para logging/debugging
}

/**
 * APIs de Android disponibles para el bot
 */
declare const Android: {
    // Logging
    log(level: 'debug' | 'info' | 'warn' | 'error', message: string): void;

    // Storage (persistencia local)
    storageGet(key: string): string | null;
    storageSet(key: string, value: string): void;
    storageRemove(key: string): void;
    storageKeys(): string[];

    // HTTP Requests
    httpRequest(options: {
        url: string;
        method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
        headers?: Record<string, string>;
        body?: string;
    }): Promise<string>;

    // Utilidades
    getCurrentTime(): number;
    getAppName(packageName: string): string;
};

/**
 * Función principal que debe implementar el bot
 */
declare function processNotification(notification: NotificationData): Promise<BotResponse> | BotResponse;
```

### 2.2 Ejemplo de bot de referencia
**Archivo**: `/app/src/main/assets/example-bot.js`

```javascript
/**
 * Bot de ejemplo para Watomagic
 * Este bot demuestra todas las capacidades disponibles
 */

async function processNotification(notification) {
    Android.log('info', `Procesando notificación de: ${notification.title}`);

    // Ejemplo 1: Descartar notificaciones de apps específicas
    if (notification.appPackage === 'com.annoying.app') {
        return {
            action: 'DISMISS',
            reason: 'App bloqueada por el usuario'
        };
    }

    // Ejemplo 2: Auto-respuesta con rate limiting
    if (notification.appPackage === 'com.whatsapp') {
        const lastReply = Android.storageGet('lastAutoReply');
        const now = Android.getCurrentTime();

        // No auto-responder más de una vez por hora
        if (!lastReply || now - parseInt(lastReply) > 3600000) {
            Android.storageSet('lastAutoReply', now.toString());

            return {
                action: 'REPLY',
                replyText: 'Estoy ocupado ahora. Te respondo pronto!'
            };
        }
    }

    // Ejemplo 3: Usar API externa para clasificación inteligente
    if (notification.title.includes('urgente') || notification.title.includes('importante')) {
        try {
            const response = await Android.httpRequest({
                url: 'https://api.example.com/classify',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer YOUR_API_KEY'
                },
                body: JSON.stringify({
                    title: notification.title,
                    body: notification.body,
                    app: notification.appPackage
                })
            });

            const result = JSON.parse(response);

            if (result.priority < 5) {
                return {
                    action: 'DISMISS',
                    reason: 'Prioridad baja según IA'
                };
            }
        } catch (error) {
            Android.log('error', `Error en API: ${error.message}`);
        }
    }

    // Ejemplo 4: Reglas basadas en horario
    const hour = new Date().getHours();

    // Durante horas de sueño (23:00 - 07:00), posponer notificaciones no críticas
    if ((hour >= 23 || hour < 7) && !notification.title.includes('alarma')) {
        return {
            action: 'SNOOZE',
            snoozeMinutes: 480, // Posponer hasta las 8 AM
            reason: 'Horario de sueño'
        };
    }

    // Ejemplo 5: Detectar spam con patrones
    const spamPatterns = [
        /ganaste/i,
        /haz clic aquí/i,
        /regalo gratis/i,
        /oferta limitada/i
    ];

    const fullText = `${notification.title} ${notification.body}`;
    for (const pattern of spamPatterns) {
        if (pattern.test(fullText)) {
            return {
                action: 'DISMISS',
                reason: 'Spam detectado'
            };
        }
    }

    // Ejemplo 6: Rastrear frecuencia de notificaciones
    const appNotifKey = `notif_count_${notification.appPackage}`;
    const count = parseInt(Android.storageGet(appNotifKey) || '0') + 1;
    Android.storageSet(appNotifKey, count.toString());

    if (count > 10) {
        const appName = Android.getAppName(notification.appPackage);
        Android.log('warn', `${appName} envió ${count} notificaciones`);

        return {
            action: 'DISMISS',
            reason: 'Demasiadas notificaciones de esta app'
        };
    }

    // Por defecto: mantener notificación
    return {
        action: 'KEEP'
    };
}
```

---

## Fase 3: Integrar Motor JavaScript (QuickJS) 🚀

### 3.1 Agregar dependencia QuickJS
**Archivo**: `/app/build.gradle.kts`

```kotlin
dependencies {
    // ... dependencias existentes ...

    // QuickJS JavaScript Engine
    implementation("app.cash.quickjs:quickjs-android:0.9.2")
}
```

**Impacto en APK**: ~2MB adicionales

### 3.2 Crear BotJsEngine wrapper
**Archivo**: `/app/src/main/java/com/parishod/watomatic/botjs/BotJsEngine.java`

```java
public class BotJsEngine {
    private static final int EXECUTION_TIMEOUT_MS = 5000;
    private final Context context;
    private QuickJs quickJs;

    public BotJsEngine(Context context) {
        this.context = context;
    }

    public void initialize() {
        quickJs = QuickJs.create();
        injectAndroidAPIs();
    }

    public String executeBot(String jsCode, String notificationDataJson)
            throws ExecutionException {
        // Ejecutar con timeout
        // Retornar respuesta JSON
    }

    private void injectAndroidAPIs() {
        // Inyectar objeto Android con todas las APIs
    }

    public void cleanup() {
        if (quickJs != null) {
            quickJs.close();
        }
    }
}
```

### 3.3 Implementar AndroidAPI para bots
**Archivo**: `/app/src/main/java/com/parishod/watomatic/botjs/BotAndroidAPI.java`

```java
public class BotAndroidAPI {
    private final Context context;
    private final SharedPreferences botStorage;
    private final OkHttpClient httpClient;

    // Logging
    public void log(String level, String message) {
        switch (level) {
            case "error": Log.e("BotJS", message); break;
            case "warn": Log.w("BotJS", message); break;
            case "info": Log.i("BotJS", message); break;
            default: Log.d("BotJS", message);
        }
    }

    // Storage - Persistencia local
    public String storageGet(String key) {
        return botStorage.getString(key, null);
    }

    public void storageSet(String key, String value) {
        botStorage.edit().putString(key, value).apply();
    }

    public void storageRemove(String key) {
        botStorage.edit().remove(key).apply();
    }

    public String[] storageKeys() {
        return botStorage.getAll().keySet().toArray(new String[0]);
    }

    // HTTP Requests
    public String httpRequest(String optionsJson) throws IOException {
        // Parsear options, ejecutar request con OkHttp
        // Retornar respuesta como string
    }

    // Utilidades
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public String getAppName(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }
}
```

---

## Fase 4: Implementar BotJsReplyProvider 🤖

### 4.1 Crear el provider
**Archivo**: `/app/src/main/java/com/parishod/watomatic/replyproviders/BotJsReplyProvider.java`

```java
public class BotJsReplyProvider implements ReplyProvider {
    private static final String TAG = "BotJsReplyProvider";

    @Override
    public void generateReply(Context context,
                             String incomingMessage,
                             NotificationData notificationData,
                             ReplyCallback callback) {
        // Ejecutar en thread background
        new Thread(() -> {
            try {
                // Cargar bot.js
                String jsCode = loadBotCode(context);

                // Validar código
                if (!BotValidator.validate(jsCode)) {
                    callback.onFailure("Bot code validation failed");
                    return;
                }

                // Ejecutar bot
                BotJsEngine engine = new BotJsEngine(context);
                engine.initialize();

                String notificationJson = convertToJson(notificationData);
                String responseJson = engine.executeBot(jsCode, notificationJson);

                // Parsear respuesta
                BotResponse response = parseResponse(responseJson);

                // Manejar acción
                switch (response.action) {
                    case "REPLY":
                        callback.onSuccess(response.replyText);
                        break;
                    case "DISMISS":
                        // Señalar que no se debe responder
                        callback.onFailure("DISMISS");
                        break;
                    case "KEEP":
                        // Usar respuesta estática
                        callback.onFailure("KEEP");
                        break;
                    case "SNOOZE":
                        // Implementar snooze
                        callback.onFailure("SNOOZE");
                        break;
                }

                engine.cleanup();

            } catch (Exception e) {
                Log.e(TAG, "Bot execution failed", e);
                callback.onFailure(e.getMessage());
            }
        }).start();
    }
}
```

### 4.2 Sistema de caché y validación
**Archivo**: `/app/src/main/java/com/parishod/watomatic/botjs/BotValidator.java`

```java
public class BotValidator {
    private static final int MAX_BOT_SIZE_BYTES = 102400; // 100KB

    private static final String[] BLACKLISTED_PATTERNS = {
        "eval\\s*\\(",
        "Function\\s*\\(",
        "constructor\\s*\\[",
        "__proto__",
        "import\\s*\\("
    };

    public static boolean validate(String jsCode) {
        // Verificar tamaño
        if (jsCode.length() > MAX_BOT_SIZE_BYTES) {
            Log.w("BotValidator", "Bot too large");
            return false;
        }

        // Verificar patrones peligrosos
        for (String pattern : BLACKLISTED_PATTERNS) {
            if (jsCode.matches(".*" + pattern + ".*")) {
                Log.w("BotValidator", "Dangerous pattern detected: " + pattern);
                return false;
            }
        }

        // Verificar que define processNotification
        if (!jsCode.contains("processNotification")) {
            Log.w("BotValidator", "Missing processNotification function");
            return false;
        }

        return true;
    }
}
```

### 4.3 Integrar en Factory
**Modificar**: `ReplyProviderFactory.java`

```java
public static ReplyProvider getProvider(PreferencesManager prefs) {
    // Prioridad: BotJs > OpenAI > Static
    if (prefs.isBotJsEnabled() && prefs.getBotJsScriptPath() != null) {
        return new BotJsReplyProvider();
    } else if (prefs.isOpenAIRepliesEnabled()) {
        return new OpenAIReplyProvider();
    }
    return new StaticReplyProvider();
}
```

---

## Fase 5: Download & Update System 📥

### 5.1 Crear BotRepository
**Archivo**: `/app/src/main/java/com/parishod/watomatic/botjs/BotRepository.java`

```java
public class BotRepository {
    private final Context context;
    private final OkHttpClient httpClient;
    private final File botsDir;

    public BotRepository(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
        this.botsDir = new File(context.getFilesDir(), "bots");
        botsDir.mkdirs();
    }

    /**
     * Descarga un bot desde una URL
     */
    public Result<BotInfo> downloadBot(String url) {
        try {
            // Validar HTTPS
            if (!url.startsWith("https://")) {
                return Result.error("Only HTTPS URLs are allowed");
            }

            // Descargar código
            Request request = new Request.Builder()
                .url(url)
                .build();

            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                return Result.error("Download failed: " + response.code());
            }

            String jsCode = response.body().string();

            // Validar código
            if (!BotValidator.validate(jsCode)) {
                return Result.error("Bot validation failed");
            }

            // Guardar en almacenamiento interno
            File botFile = new File(botsDir, "active-bot.js");
            FileWriter writer = new FileWriter(botFile);
            writer.write(jsCode);
            writer.close();

            // Guardar metadata
            BotInfo botInfo = new BotInfo(url, System.currentTimeMillis());
            saveBotMetadata(botInfo);

            return Result.success(botInfo);

        } catch (IOException e) {
            Log.e("BotRepository", "Download failed", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * Verifica si hay actualizaciones disponibles
     */
    public boolean checkForUpdates() {
        // Comparar hash del bot remoto vs local
        // Retornar true si hay nueva versión
        return false;
    }

    /**
     * Obtiene información del bot instalado
     */
    public BotInfo getInstalledBotInfo() {
        SharedPreferences prefs = context.getSharedPreferences("bot_metadata", Context.MODE_PRIVATE);
        String url = prefs.getString("url", null);
        long timestamp = prefs.getLong("timestamp", 0);

        if (url == null) return null;

        return new BotInfo(url, timestamp);
    }

    /**
     * Elimina el bot instalado
     */
    public void deleteBot() {
        File botFile = new File(botsDir, "active-bot.js");
        botFile.delete();

        context.getSharedPreferences("bot_metadata", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply();
    }

    private void saveBotMetadata(BotInfo info) {
        context.getSharedPreferences("bot_metadata", Context.MODE_PRIVATE)
            .edit()
            .putString("url", info.url)
            .putLong("timestamp", info.timestamp)
            .apply();
    }
}
```

### 5.2 Auto-update en background
**Archivo**: `/app/src/main/java/com/parishod/watomatic/workers/BotUpdateWorker.java`

```java
public class BotUpdateWorker extends Worker {
    public BotUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        PreferencesManager prefs = PreferencesManager.getPreferencesInstance(getApplicationContext());

        if (!prefs.isBotJsEnabled() || !prefs.isBotAutoUpdateEnabled()) {
            return Result.success();
        }

        BotRepository repository = new BotRepository(getApplicationContext());

        if (repository.checkForUpdates()) {
            String botUrl = prefs.getBotJsUrl();
            Result<BotInfo> result = repository.downloadBot(botUrl);

            if (result.isSuccess()) {
                // Notificar al usuario
                showUpdateNotification();
                return Result.success();
            }
        }

        return Result.success();
    }

    private void showUpdateNotification() {
        // Crear notificación informando del update
    }
}
```

**Programar en MainActivity**:
```java
// En MainActivity.onCreate()
PeriodicWorkRequest botUpdateWork = new PeriodicWorkRequest.Builder(
    BotUpdateWorker.class,
    6, TimeUnit.HOURS
).build();

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "BotUpdateWork",
    ExistingPeriodicWorkPolicy.KEEP,
    botUpdateWork
);
```

### 5.3 Verificación de seguridad
**Implementar en BotRepository**:
- Validar que la URL sea HTTPS
- Opcional: Verificar firma digital del bot
- Sanitización de código (blacklist de patrones)
- Rate limiting de descargas (máx 1 por hora)

---

## Fase 6: GUI - Configuración de Bots 🎨

### 6.1 Nueva BotConfigActivity
**Archivo**: `/app/src/main/java/com/parishod/watomatic/activity/botconfig/BotConfigActivity.kt`

```kotlin
class BotConfigActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBotConfigBinding
    private lateinit var botRepository: BotRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBotConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupBotConfig()
        loadBotInfo()
    }

    private fun setupBotConfig() {
        binding.enableBotSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.getPreferencesInstance(this)
                .setBotJsEnabled(isChecked)
            updateUIVisibility()
        }

        binding.downloadBotButton.setOnClickListener {
            downloadBot()
        }

        binding.testBotButton.setOnClickListener {
            testBot()
        }
    }

    private fun downloadBot() {
        val url = binding.botUrlInput.text.toString()

        if (!url.startsWith("https://")) {
            showError("Solo se permiten URLs HTTPS")
            return
        }

        binding.downloadProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                botRepository.downloadBot(url)
            }

            binding.downloadProgress.visibility = View.GONE

            if (result.isSuccess) {
                showSuccess("Bot descargado exitosamente")
                PreferencesManager.getPreferencesInstance(this@BotConfigActivity)
                    .setBotJsUrl(url)
                loadBotInfo()
            } else {
                showError("Error: ${result.error}")
            }
        }
    }
}
```

### 6.2 Agregar a Settings
**Modificar**: `/app/src/main/res/xml/fragment_settings.xml`

```xml
<!-- Después de OpenAI settings -->
<Preference
    android:key="bot_config"
    android:title="@string/bot_configuration"
    android:summary="@string/bot_configuration_summary"
    android:icon="@drawable/ic_code">
    <intent
        android:action="android.intent.action.VIEW"
        android:targetClass="com.parishod.watomatic.activity.botconfig.BotConfigActivity"
        android:targetPackage="com.parishod.watomatic" />
</Preference>
```

### 6.3 Layouts
**Archivo**: `/app/src/main/res/layout/activity_bot_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            app:title="@string/bot_configuration" />
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- Bot Status Card -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:text="@string/bot_status"
                        android:textAppearance="?attr/textAppearanceHeadline6"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content" />

                    <com.google.android.material.switchmaterial.SwitchMaterial
                        android:id="@+id/enableBotSwitch"
                        android:text="@string/enable_bot_js"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp" />
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- Download URL Card -->
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/downloadUrlCard"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:text="@string/bot_download_url"
                        android:textAppearance="?attr/textAppearanceHeadline6"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content" />

                    <com.google.android.material.textfield.TextInputLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp"
                        android:hint="@string/bot_url_hint">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/botUrlInput"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:inputType="textUri" />
                    </com.google.android.material.textfield.TextInputLayout>

                    <Button
                        android:id="@+id/downloadBotButton"
                        android:text="@string/download_bot"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp" />

                    <ProgressBar
                        android:id="@+id/downloadProgress"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:visibility="gone" />
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- Bot Info Card -->
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/botInfoCard"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                android:visibility="gone">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:text="@string/bot_info"
                        android:textAppearance="?attr/textAppearanceHeadline6"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content" />

                    <TextView
                        android:id="@+id/botUrlText"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp" />

                    <TextView
                        android:id="@+id/botLastUpdateText"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="4dp" />

                    <Button
                        android:id="@+id/testBotButton"
                        android:text="@string/test_bot"
                        style="@style/Widget.Material3.Button.OutlinedButton"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp" />
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- Advanced Settings Card -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:text="@string/advanced_settings"
                        android:textAppearance="?attr/textAppearanceHeadline6"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content" />

                    <com.google.android.material.switchmaterial.SwitchMaterial
                        android:id="@+id/autoUpdateSwitch"
                        android:text="@string/auto_update_bot"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp" />

                    <Button
                        android:id="@+id/viewLogsButton"
                        android:text="@string/view_bot_logs"
                        style="@style/Widget.Material3.Button.TextButton"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content" />

                    <Button
                        android:id="@+id/deleteBotButton"
                        android:text="@string/delete_bot"
                        style="@style/Widget.Material3.Button.TextButton"
                        app:iconTint="?attr/colorError"
                        android:textColor="?attr/colorError"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content" />
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

---

## Fase 7: Testing & Seguridad 🔒

### 7.1 Validación y sandboxing

**Rate Limiter**:
```java
public class RateLimiter {
    private final int maxExecutions;
    private final long windowMs;
    private final Queue<Long> executionTimes = new LinkedList<>();

    public boolean tryAcquire() {
        long now = System.currentTimeMillis();

        // Limpiar ejecuciones antiguas
        while (!executionTimes.isEmpty() &&
               executionTimes.peek() < now - windowMs) {
            executionTimes.poll();
        }

        if (executionTimes.size() >= maxExecutions) {
            return false;
        }

        executionTimes.offer(now);
        return true;
    }
}
```

**Timeout Executor**:
```java
public class TimeoutExecutor {
    public static <T> T executeWithTimeout(Callable<T> task, long timeoutMs)
            throws TimeoutException, ExecutionException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(task);

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Bot execution exceeded " + timeoutMs + "ms");
        } finally {
            executor.shutdownNow();
        }
    }
}
```

### 7.2 Error handling robusto

**BotExecutionException**:
```java
public class BotExecutionException extends Exception {
    private final String jsError;
    private final String jsStackTrace;

    public BotExecutionException(String message, String jsError, String jsStackTrace) {
        super(message);
        this.jsError = jsError;
        this.jsStackTrace = jsStackTrace;
    }

    public String getDetailedMessage() {
        return String.format(
            "Bot Error: %s\nJS Error: %s\nStack Trace: %s",
            getMessage(), jsError, jsStackTrace
        );
    }
}
```

### 7.3 Tests unitarios

**Test para ReplyProviderFactory**:
```java
@Test
public void testProviderSelection_BotJsEnabled() {
    PreferencesManager prefs = mock(PreferencesManager.class);
    when(prefs.isBotJsEnabled()).thenReturn(true);
    when(prefs.getBotJsScriptPath()).thenReturn("/path/to/bot.js");

    ReplyProvider provider = ReplyProviderFactory.getProvider(prefs);

    assertTrue(provider instanceof BotJsReplyProvider);
}

@Test
public void testProviderSelection_OpenAIEnabled() {
    PreferencesManager prefs = mock(PreferencesManager.class);
    when(prefs.isBotJsEnabled()).thenReturn(false);
    when(prefs.isOpenAIRepliesEnabled()).thenReturn(true);

    ReplyProvider provider = ReplyProviderFactory.getProvider(prefs);

    assertTrue(provider instanceof OpenAIReplyProvider);
}

@Test
public void testProviderSelection_DefaultStatic() {
    PreferencesManager prefs = mock(PreferencesManager.class);
    when(prefs.isBotJsEnabled()).thenReturn(false);
    when(prefs.isOpenAIRepliesEnabled()).thenReturn(false);

    ReplyProvider provider = ReplyProviderFactory.getProvider(prefs);

    assertTrue(provider instanceof StaticReplyProvider);
}
```

**Test para BotValidator**:
```java
@Test
public void testValidation_ValidBot() {
    String validBot = "async function processNotification(notification) { return { action: 'KEEP' }; }";
    assertTrue(BotValidator.validate(validBot));
}

@Test
public void testValidation_TooLarge() {
    String largeBot = new String(new char[200000]).replace('\0', 'x');
    assertFalse(BotValidator.validate(largeBot));
}

@Test
public void testValidation_DangerousPattern() {
    String dangerousBot = "eval('malicious code')";
    assertFalse(BotValidator.validate(dangerousBot));
}
```

---

## Fase 8: Documentación 📚

### 8.1 Documentación para usuarios
**Archivo**: `/docs/BOT_DEVELOPMENT_GUIDE.md`

```markdown
# Guía de Desarrollo de Bots para Watomagic

## Introducción
Los bots de Watomagic son scripts JavaScript que se ejecutan localmente en tu dispositivo para procesar notificaciones entrantes y decidir cómo responder.

## Estructura Básica
Todo bot debe implementar la función `processNotification`:

```javascript
async function processNotification(notification) {
    // Tu lógica aquí
    return {
        action: 'KEEP' // o 'DISMISS', 'REPLY', 'SNOOZE'
    };
}
```

## APIs Disponibles

### Android.log()
Registra mensajes en los logs de la aplicación.
```javascript
Android.log('info', 'Mensaje informativo');
Android.log('error', 'Algo salió mal');
```

### Android.storage*()
Persistencia de datos entre ejecuciones.
```javascript
Android.storageSet('contador', '5');
const valor = Android.storageGet('contador');
Android.storageRemove('contador');
```

### Android.httpRequest()
Realizar llamadas HTTP a APIs externas.
```javascript
const response = await Android.httpRequest({
    url: 'https://api.example.com/data',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ key: 'value' })
});
const data = JSON.parse(response);
```

## Ejemplos Comunes

### Auto-respuesta Simple
```javascript
async function processNotification(notification) {
    if (notification.appPackage === 'com.whatsapp') {
        return {
            action: 'REPLY',
            replyText: 'Estoy ocupado, te respondo luego.'
        };
    }
    return { action: 'KEEP' };
}
```

### Bloquear Apps
```javascript
async function processNotification(notification) {
    const blockedApps = ['com.annoying.app', 'com.spam.app'];

    if (blockedApps.includes(notification.appPackage)) {
        return {
            action: 'DISMISS',
            reason: 'App bloqueada'
        };
    }

    return { action: 'KEEP' };
}
```

### Usar IA Externa
```javascript
async function processNotification(notification) {
    const response = await Android.httpRequest({
        url: 'https://api.openai.com/v1/chat/completions',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer TU_API_KEY'
        },
        body: JSON.stringify({
            model: 'gpt-3.5-turbo',
            messages: [
                { role: 'system', content: 'Eres un asistente que decide cómo responder notificaciones.' },
                { role: 'user', content: notification.body }
            ]
        })
    });

    const result = JSON.parse(response);
    const aiReply = result.choices[0].message.content;

    return {
        action: 'REPLY',
        replyText: aiReply
    };
}
```

## Mejores Prácticas

1. **Maneja errores**: Usa try/catch para evitar que tu bot falle
2. **Sé eficiente**: El bot tiene 5 segundos de timeout
3. **Usa storage sabiamente**: Para datos persistentes entre notificaciones
4. **Logging**: Usa Android.log() para debuggear
5. **Rate limiting**: Controla la frecuencia de respuestas con storage

## Limitaciones

- Timeout de ejecución: 5 segundos
- Tamaño máximo del bot: 100KB
- No se permite: eval(), Function(), acceso al sistema de archivos
- Las llamadas HTTP deben ser HTTPS

## Deployment

Sube tu bot.js a un servidor HTTPS y configura la URL en la app.
```

### 8.2 API Reference
**Archivo**: `/docs/BOT_API_REFERENCE.md`

Documentación completa de todas las interfaces TypeScript y métodos disponibles.

---

## 📊 Resumen de Archivos

### Archivos Nuevos a Crear (32 archivos):

```
/app/src/main/java/com/parishod/watomatic/
├── replyproviders/
│   ├── ReplyProvider.java ⭐ (interfaz base)
│   ├── ReplyProviderFactory.java ⭐ (factory pattern)
│   ├── StaticReplyProvider.java ⭐ (respuestas estáticas)
│   ├── OpenAIReplyProvider.java ⭐ (extraído de NotificationService)
│   └── BotJsReplyProvider.java 🆕 (nuevo provider de bots)
├── botjs/
│   ├── BotJsEngine.java 🆕 (wrapper de QuickJS)
│   ├── BotAndroidAPI.java 🆕 (APIs para el bot)
│   ├── BotRepository.java 🆕 (download/update system)
│   ├── BotValidator.java 🆕 (validación de código)
│   ├── BotExecutionException.java 🆕
│   ├── RateLimiter.java 🆕
│   └── TimeoutExecutor.java 🆕
├── activity/botconfig/
│   └── BotConfigActivity.kt 🆕 (UI de configuración)
└── workers/
    └── BotUpdateWorker.java 🆕 (auto-updates)

/app/src/main/res/
├── layout/
│   └── activity_bot_config.xml 🆕
├── values/
│   └── strings.xml (agregar strings de bot)
└── xml/
    └── fragment_settings.xml (modificar)

/app/src/main/assets/
├── bot-types.d.ts 🆕 (interfaces TypeScript)
└── example-bot.js 🆕 (bot de referencia)

/docs/
├── PLAN_BOTJS_SYSTEM.md (este archivo)
├── BOT_DEVELOPMENT_GUIDE.md 🆕
├── BOT_API_REFERENCE.md 🆕
└── ARCHITECTURE.md 🆕
```

### Archivos a Modificar (4 archivos):

```
/app/src/main/java/com/parishod/watomatic/
├── service/NotificationService.java
│   └── sendReply() método: 150→20 líneas ⭐ CRÍTICO
├── model/preferences/PreferencesManager.java
│   └── Agregar: isBotJsEnabled(), getBotJsUrl(), etc.
└── activity/main/MainActivity.java
    └── Programar BotUpdateWorker

/app/build.gradle.kts
└── Agregar dependencia QuickJS
```

---

## ⏱️ Estimación de Esfuerzo

| Fase | Descripción | Tiempo Estimado |
|------|-------------|----------------|
| **Fase 1** | Refactoring Strategy Pattern | 3-4 horas |
| **Fase 2** | Interfaces TypeScript | 1 hora |
| **Fase 3** | Integración QuickJS | 4-5 horas |
| **Fase 4** | BotJsReplyProvider | 3-4 horas |
| **Fase 5** | Download & Update System | 2-3 horas |
| **Fase 6** | GUI Configuration | 3-4 horas |
| **Fase 7** | Testing & Security | 2-3 horas |
| **Fase 8** | Documentación | 2-3 horas |
| **Total** | | **20-27 horas** |

---

## 🎯 Hitos de Verificación

### Milestone 1: Strategy Pattern (Fin Fase 1) - ❌ NO INICIADO
**Progreso**: 0/12 tareas completadas

#### Creación de Providers
- [ ] ReplyProvider.java - Interfaz base creada
- [ ] OpenAIReplyProvider.java - Lógica OpenAI extraída (líneas 151-277)
- [ ] StaticReplyProvider.java - Respuestas estáticas encapsuladas
- [ ] ReplyProviderFactory.java - Factory pattern implementado

#### Refactoring NotificationService
- [ ] NotificationService.sendReply() simplificado (149→20 líneas)
- [ ] Método sendActualReply() preservado y funcionando
- [ ] Callbacks correctamente implementados

#### Testing Fase 1
- [ ] ReplyProviderFactoryTest.java - Tests de selección de providers
- [ ] OpenAIReplyProviderTest.java - Tests con mocks de Retrofit
- [ ] StaticReplyProviderTest.java - Tests de respuestas estáticas

#### Verificación Final
- [ ] ✅ OpenAI sigue funcionando exactamente igual que antes
- [ ] ✅ Respuestas estáticas funcionan correctamente
- [ ] ✅ Todos los tests pasando

---

### Milestone 2: TypeScript Interfaces (Fin Fase 2) - ❌ NO INICIADO
**Progreso**: 0/3 tareas completadas

- [ ] Directorio `/app/src/main/assets/` creado
- [ ] bot-types.d.ts - Interfaces TypeScript definidas
- [ ] example-bot.js - Bot de referencia con 6 ejemplos funcionando

---

### Milestone 3: QuickJS Integration (Fin Fase 3) - ❌ NO INICIADO
**Progreso**: 0/10 tareas completadas

#### Dependencias
- [ ] build.gradle.kts - QuickJS dependency agregada
- [ ] Build exitoso con nueva dependencia

#### Core Engine
- [ ] BotJsEngine.java - Wrapper de QuickJS creado
- [ ] BotAndroidAPI.java - APIs de Android implementadas
- [ ] TimeoutExecutor.java - Sistema de timeout creado

#### Android APIs
- [ ] Android.log() - Logging funcional
- [ ] Android.storage*() - Storage con SharedPreferences
- [ ] Android.httpRequest() - HTTP con OkHttpClient
- [ ] Android.getCurrentTime() - Utilidades funcionando

#### Testing Fase 3
- [ ] BotJsEngineTest.java - Tests de ejecución básica
- [ ] ✅ Puede ejecutar JavaScript simple con timeout

---

### Milestone 4: Bot System Functional (Fin Fase 4) - ❌ NO INICIADO
**Progreso**: 0/11 tareas completadas

#### Core Provider
- [ ] BotJsReplyProvider.java - Provider implementado
- [ ] Carga bot.js desde almacenamiento interno
- [ ] Ejecuta bot con BotJsEngine
- [ ] Parsea BotResponse correctamente
- [ ] Maneja 4 acciones: REPLY, DISMISS, KEEP, SNOOZE

#### Validación y Seguridad
- [ ] BotValidator.java - Validación de código
- [ ] BotExecutionException.java - Manejo de errores
- [ ] RateLimiter.java - Rate limiting 100/min
- [ ] Factory actualizado con prioridad BotJS > OpenAI > Static

#### Testing Fase 4
- [ ] BotValidatorTest.java - Tests de validación
- [ ] BotJsReplyProviderTest.java - Test end-to-end
- [ ] ✅ Bot puede procesar notificación de prueba exitosamente

---

### Milestone 5: Download & Auto-update (Fin Fase 5) - ❌ NO INICIADO
**Progreso**: 0/12 tareas completadas

#### Download System
- [ ] BotRepository.java - Sistema de descarga creado
- [ ] downloadBot() - Descarga y valida desde HTTPS
- [ ] checkForUpdates() - Compara hash SHA-256
- [ ] getInstalledBotInfo() - Metadata del bot
- [ ] deleteBot() - Eliminación de bot
- [ ] Rate limiting de descargas (1/hora)

#### Auto-update Worker
- [ ] BotUpdateWorker.java - Worker creado
- [ ] WorkManager programado en MainActivity (cada 6h)
- [ ] Notificación de update funcionando

#### PreferencesManager
- [ ] isBotJsEnabled() / setBotJsEnabled()
- [ ] getBotJsUrl() / setBotJsUrl()
- [ ] isBotAutoUpdateEnabled() / setBotAutoUpdateEnabled()

---

### Milestone 6: GUI Complete (Fin Fase 6) - ❌ NO INICIADO
**Progreso**: 0/13 tareas completadas

#### Activity
- [ ] BotConfigActivity.kt - Activity creada
- [ ] activity_bot_config.xml - Layout con 4 cards
- [ ] Bot Status Card - Switch enable/disable
- [ ] Download URL Card - Input + botón + progress
- [ ] Bot Info Card - Muestra metadata + test
- [ ] Advanced Settings Card - Auto-update, logs, delete

#### Funcionalidad
- [ ] Descarga de bot desde URL funcionando
- [ ] Validación HTTPS en UI
- [ ] Progress feedback durante descarga
- [ ] Test bot con notificación dummy
- [ ] Snackbar para errores/éxitos

#### Integración
- [ ] fragment_settings.xml - Entry agregado
- [ ] strings.xml - Strings agregados
- [ ] ✅ Activity se abre desde settings correctamente

---

### Milestone 7: Testing & Security (Fin Fase 7) - ❌ NO INICIADO
**Progreso**: 0/8 tareas completadas

#### Tests de Seguridad
- [ ] Test rechazo de patrones peligrosos (eval, Function, etc.)
- [ ] Test timeout se activa a los 5s
- [ ] Test rate limiting funciona
- [ ] Test solo HTTPS permitido

#### Integration Tests
- [ ] BotSystemIntegrationTest.java - Test end-to-end completo
- [ ] Mock de NotificationService funcionando
- [ ] Test con example-bot.js

#### Métricas
- [ ] ✅ Cobertura de tests >75% alcanzada

---

### Milestone 8: Production Ready (Fin Fase 8) - ❌ NO INICIADO
**Progreso**: 4/6 tareas completadas

#### Documentación
- [x] BOT_DEVELOPMENT_GUIDE.md - Guía completa para devs
- [x] BOT_API_REFERENCE.md - API reference detallada
- [x] ARCHITECTURE.md - Diagramas y decisiones
- [x] BOT_USER_GUIDE.md - Documentación operativa para usuarios finales

#### Verificación Final
- [ ] ✅ Documentación completa y clara
- [ ] ✅ Ejemplos de bots funcionan
- [ ] ✅ Sistema completo listo para producción

---

### 📈 Progreso Total del Proyecto

**Fases Completadas**: 0/8 (0%)

| Fase | Nombre | Estado | Progreso |
|------|--------|--------|----------|
| 1 | Strategy Pattern | ❌ NO INICIADO | 0/12 (0%) |
| 2 | TypeScript Interfaces | ❌ NO INICIADO | 0/3 (0%) |
| 3 | QuickJS Integration | ❌ NO INICIADO | 0/10 (0%) |
| 4 | BotJS Provider | ❌ NO INICIADO | 0/11 (0%) |
| 5 | Download System | ❌ NO INICIADO | 0/12 (0%) |
| 6 | GUI | ❌ NO INICIADO | 0/13 (0%) |
| 7 | Testing & Security | ❌ NO INICIADO | 0/8 (0%) |
| 8 | Documentation | ❌ NO INICIADO | 0/6 (0%) |
| **TOTAL** | **Sistema BotJS** | ❌ **NO INICIADO** | **0/75 (0%)** |

---

## 🔐 Consideraciones de Seguridad

### Validaciones Implementadas
1. ✅ Solo URLs HTTPS
2. ✅ Validación de tamaño (max 100KB)
3. ✅ Blacklist de patrones peligrosos
4. ✅ Timeout de ejecución (5s)
5. ✅ Rate limiting (100 exec/min)
6. ✅ Sandbox en thread separado
7. ⚠️ Opcional: Firma digital de bots

### Superficie de Ataque Minimizada
- No acceso a filesystem Android
- No acceso a contactos
- No acceso a otros apps
- Solo HTTP/HTTPS outbound
- Storage aislado por app

---

## 🔄 Compatibilidad con Upstream

### Ventajas del Enfoque Strategy Pattern
1. **Mínimos merge conflicts**: Solo ~20 líneas en NotificationService
2. **Extensiones aisladas**: Toda la lógica de bot.js en paquete separado
3. **Puede contribuir al upstream**: El refactoring mejora el código base original
4. **Fácil mantenimiento**: Actualizaciones de upstream se aplican limpiamente

### Plan de Merge con Upstream
```bash
# Configurar upstream
git remote add upstream https://github.com/adeekshith/watomatic.git
git fetch upstream

# Sincronizar regularmente
git checkout main
git merge upstream/main

# Resolver conflictos (deberían ser mínimos)
# - PreferencesManager: agregar keys de bot.js
# - Factory: agregar case de bot.js
# - NotificationService: debería mergear limpio
```

---

## 📈 Métricas de Éxito

### Técnicas
- ✅ Código compila sin errores
- ✅ Todos los tests pasan (>80% coverage)
- ✅ APK size increase < 3MB
- ✅ Bot execution < 5s (99th percentile)
- ✅ Zero crashes por 7 días

### Funcionales
- ✅ Usuario puede descargar bot por URL
- ✅ Bot ejecuta y responde correctamente
- ✅ Auto-update funciona
- ✅ OpenAI sigue funcionando
- ✅ Respuestas estáticas siguen funcionando

### UX
- ✅ UI intuitiva y clara
- ✅ Mensajes de error informativos
- ✅ Documentación comprensible
- ✅ Ejemplo funcional incluido

---

## 🚀 Próximos Pasos Post-MVP

### Mejoras Futuras (Post-Implementación)
1. **Bot Marketplace**: Repositorio de bots compartidos por la comunidad
2. **Bot Editor In-App**: Editor de código en la aplicación
3. **Más Engines**: Soporte para Python (Chaquopy), Lua, etc.
4. **TypeScript Support**: Compilar .ts a .js en el servidor
5. **Debugger**: Herramienta visual para debuggear bots
6. **Bot Analytics**: Estadísticas de ejecución
7. **Cloud Sync**: Sincronizar bots entre dispositivos
8. **Permisos Granulares**: Control fino de qué APIs puede usar cada bot

---

## 📝 Notas Finales

### Decisiones Arquitectónicas Clave
1. **QuickJS vs V8/Rhino**: QuickJS elegido por tamaño (~2MB) y ES2020 support
2. **Strategy Pattern**: Permite extensibilidad limpia
3. **HTTPS obligatorio**: Seguridad first
4. **Timeout de 5s**: Balance entre complejidad y UX
5. **Almacenamiento interno**: Cumple con políticas de Google Play

### Riesgos Mitigados
- ❌ **APK demasiado grande**: QuickJS solo 2MB
- ❌ **Seguridad comprometida**: Validación + sandboxing
- ❌ **Merge conflicts**: Strategy pattern minimiza cambios
- ❌ **Bots maliciosos**: Blacklist + validación + HTTPS

### Éxito Garantizado Si
1. ✅ Se completa el refactoring Strategy Pattern primero
2. ✅ Se mantiene compatibilidad con OpenAI
3. ✅ Se documentan bien las interfaces
4. ✅ Se testea exhaustivamente
5. ✅ Se sincroniza regularmente con upstream

---

## 📝 Historial de Cambios

### Versión 2.0 - 2025-11-15
- ✅ Agregado estado actual de implementación (0% completado)
- ✅ Agregadas decisiones confirmadas (motor, alcance, seguridad, etc.)
- ✅ Actualizados detalles técnicos (líneas de código correctas)
- ✅ Agregado checklist detallado de progreso (75 tareas totales)
- ✅ Agregada tabla de progreso por fase
- ✅ Confirmado uso de QuickJS como motor JavaScript
- ✅ Confirmado implementación de 8 fases completas

### Versión 1.0 - 2025-11-12
- Plan inicial del sistema BotJS
- Arquitectura Strategy Pattern propuesta
- 8 fases de implementación definidas
- Estimación de 20-27 horas

---

**Autor**: Plan generado con Claude Code
**Versión**: 2.0
**Última actualización**: 2025-11-15
**Estado del Proyecto**: ❌ NO INICIADO (0/75 tareas completadas)
