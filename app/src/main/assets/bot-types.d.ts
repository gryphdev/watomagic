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
        timeoutMs?: number;
    }): Promise<string>;

    // Utilidades
    getCurrentTime(): number;
    getAppName(packageName: string): string;
};

/**
 * API de localStorage estándar (compatible con navegadores)
 * Internamente usa Android.storage* para persistencia
 */
interface Storage {
    readonly length: number;
    getItem(key: string): string | null;
    setItem(key: string, value: string): void;
    removeItem(key: string): void;
    clear(): void;
    key(index: number): string | null;
}

declare const localStorage: Storage;

/**
 * Función principal que debe implementar el bot
 */
declare function processNotification(notification: NotificationData): Promise<BotResponse> | BotResponse;
