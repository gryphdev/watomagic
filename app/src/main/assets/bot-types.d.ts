/**
 * Información de un archivo adjunto (imagen)
 */
interface AttachmentInfo {
    id: string;
    mimeType: string;
    size: number;
    hasFile: boolean;
    thumbnailBase64?: string;
}

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
    attachments: AttachmentInfo[];
}

/**
 * Información de un archivo adjunto para enviar
 */
interface AttachmentToSend {
    path: string;              // Ruta relativa a bot_attachments/ o absoluta dentro del directorio de la app
    mimeType: string;          // Tipo MIME (ej: "image/jpeg", "image/png")
}

/**
 * Respuesta esperada del bot
 */
interface BotResponse {
    action: 'KEEP' | 'DISMISS' | 'REPLY' | 'SNOOZE';
    replyText?: string;        // Requerido si action = 'REPLY'
    snoozeMinutes?: number;    // Requerido si action = 'SNOOZE'
    reason?: string;           // Opcional: para logging/debugging
    attachments?: AttachmentToSend[];  // Opcional: archivos adjuntos para enviar (solo si action = 'REPLY')
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

    // Attachment access
    getAttachmentPath(id: string): string | null;
    readAttachmentAsBase64(id: string): string | null;
    getAttachmentThumbnail(id: string): string | null;
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
