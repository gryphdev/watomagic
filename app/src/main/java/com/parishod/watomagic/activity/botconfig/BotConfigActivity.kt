package com.parishod.watomagic.activity.botconfig

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.parishod.watomagic.R
import com.parishod.watomagic.activity.BaseActivity
import com.parishod.watomagic.botjs.BotRepository
import com.parishod.watomagic.botjs.BotLogCapture
import com.parishod.watomagic.botjs.BotJsEngine
import com.parishod.watomagic.model.preferences.PreferencesManager
import com.parishod.watomagic.replyproviders.model.NotificationData
import com.parishod.watomagic.workers.BotUpdateWorker
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileReader
import android.service.notification.StatusBarNotification
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class BotConfigActivity : BaseActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var botRepository: BotRepository
    
    private lateinit var enableBotSwitch: SwitchMaterial
    private lateinit var botUrlInput: TextInputEditText
    private lateinit var downloadBotButton: Button
    private lateinit var downloadProgress: ProgressBar
    private lateinit var botInfoCard: View
    private lateinit var botUrlText: TextView
    private lateinit var botHashText: TextView
    private lateinit var botLastUpdateText: TextView
    private lateinit var testBotButton: Button
    private lateinit var autoUpdateSwitch: SwitchMaterial
    private lateinit var deleteBotButton: Button
    private lateinit var debugModeSwitch: SwitchMaterial
    private lateinit var viewLogsButton: Button
    private lateinit var testWebhookButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bot_config)

        preferencesManager = PreferencesManager.getPreferencesInstance(this)
        botRepository = BotRepository(this)

        setupToolbar()
        setupViews()
        loadBotInfo()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViews() {
        enableBotSwitch = findViewById(R.id.enableBotSwitch)
        botUrlInput = findViewById(R.id.botUrlInput)
        downloadBotButton = findViewById(R.id.downloadBotButton)
        downloadProgress = findViewById(R.id.downloadProgress)
        botInfoCard = findViewById(R.id.botInfoCard)
        botUrlText = findViewById(R.id.botUrlText)
        botHashText = findViewById(R.id.botHashText)
        botLastUpdateText = findViewById(R.id.botLastUpdateText)
        testBotButton = findViewById(R.id.testBotButton)
        autoUpdateSwitch = findViewById(R.id.autoUpdateSwitch)
        deleteBotButton = findViewById(R.id.deleteBotButton)

        enableBotSwitch.isChecked = preferencesManager.isBotJsEnabled()
        enableBotSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBotJsEnabled(isChecked)
            updateUIVisibility()
        }

        autoUpdateSwitch.isChecked = preferencesManager.isBotJsAutoUpdateEnabled()
        autoUpdateSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBotJsAutoUpdate(isChecked)
            scheduleBotUpdateWorker(isChecked)
        }

        downloadBotButton.setOnClickListener { downloadBot() }
        testBotButton.setOnClickListener { testBot() }
        deleteBotButton.setOnClickListener { deleteBot() }

        // Debug UI
        debugModeSwitch = findViewById(R.id.debugModeSwitch)
        viewLogsButton = findViewById(R.id.viewLogsButton)
        testWebhookButton = findViewById(R.id.testWebhookButton)

        debugModeSwitch.isChecked = preferencesManager.isBotJsDebugModeEnabled()
        debugModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBotJsDebugMode(isChecked)
            BotLogCapture.setEnabled(isChecked)
            updateUIVisibility()
        }

        viewLogsButton.setOnClickListener { openLogViewer() }
        testWebhookButton.setOnClickListener { testWebhook() }

        // Inicializar BotLogCapture según preferencias
        BotLogCapture.setEnabled(preferencesManager.isBotJsDebugModeEnabled())

        // Cargar URL si existe
        preferencesManager.getBotJsUrl()?.let {
            botUrlInput.setText(it)
        }
    }

    private fun downloadBot() {
        val url = botUrlInput.text?.toString()?.trim() ?: ""

        if (url.isEmpty()) {
            showError("Por favor ingresa una URL")
            return
        }

        if (!url.startsWith("https://")) {
            showError("Solo se permiten URLs HTTPS")
            return
        }

        downloadProgress.visibility = View.VISIBLE
        downloadBotButton.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                botRepository.downloadBot(url)
            }

            downloadProgress.visibility = View.GONE
            downloadBotButton.isEnabled = true

            if (result.isSuccess) {
                showSuccess("Bot descargado exitosamente")
                preferencesManager.setBotJsUrl(url)
                loadBotInfo()
            } else {
                showError("Error: ${result.getError()}")
            }
        }
    }

    private fun testBot() {
        val botInfo = botRepository.getInstalledBotInfo()
        if (botInfo == null) {
            showError("No hay bot instalado")
            return
        }

        // Habilitar debug temporalmente
        val wasDebugEnabled = BotLogCapture.isEnabled()
        BotLogCapture.setEnabled(true)
        BotLogCapture.clear()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    // Crear notificación de prueba
                    val testNotification = createTestNotification()

                    // Cargar código del bot
                    val jsCode = loadBotCode()

                    // Ejecutar bot
                    val botJsEngine = BotJsEngine(this@BotConfigActivity)
                    botJsEngine.initialize()
                    try {
                        val response = botJsEngine.executeBot(jsCode, testNotification)
                        Result.success(response)
                    } finally {
                        botJsEngine.cleanup()
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            // Restaurar modo debug
            if (!wasDebugEnabled) {
                BotLogCapture.setEnabled(false)
            }

            if (result.isSuccess) {
                showSuccess("Test exitoso. Ver logs para detalles.")
                openLogViewer()
            } else {
                showError("Test falló: ${result.exceptionOrNull()?.message}")
                openLogViewer()
            }
        }
    }

    private fun createTestNotification(): NotificationData {
        // Crear notificación dummy para testing
        // Nota: StatusBarNotification requiere API real, creamos un mock simple
        return object : NotificationData {
            override fun getTitle(): String = "Test WhatsApp Message"
            override fun getBody(): String = "Hola, este es un mensaje de prueba"
            override fun getAppPackage(): String = "com.whatsapp"
            override fun getTimestamp(): Long = System.currentTimeMillis()
            override fun isGroup(): Boolean = false
            override fun getStatusBarNotification(): StatusBarNotification {
                throw UnsupportedOperationException("Test notification - no real SBN")
            }
        }
    }

    private fun loadBotCode(): String {
        val botsDir = File(filesDir, "bots")
        val botFile = File(botsDir, "active-bot.js")
        return FileReader(botFile).use { it.readText() }
    }

    private fun testWebhook() {
        val url = botUrlInput.text?.toString()?.trim() ?: ""
        if (url.isEmpty()) {
            showError("Ingresa una URL primero")
            return
        }

        downloadProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val body = response.body?.string()?.take(200) ?: ""
                        Result.success("HTTP ${response.code}: $body")
                    } else {
                        Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            downloadProgress.visibility = View.GONE

            if (result.isSuccess) {
                showSuccess("Webhook OK: ${result.getOrNull()}")
            } else {
                showError("Webhook Error: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private fun openLogViewer() {
        val logs = BotLogCapture.getLogs()
        if (logs.isEmpty()) {
            showError("No hay logs disponibles")
            return
        }

        val logText = logs.joinToString("\n") { log ->
            val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                .format(Date(log.timestamp))
            "[$time] [${log.level.uppercase()}] ${log.message}"
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_log_viewer, null)
        val logTextView = dialogView.findViewById<TextView>(R.id.logTextView)
        logTextView.text = logText

        AlertDialog.Builder(this)
            .setTitle("Logs de Ejecución (${logs.size})")
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Limpiar") { _, _ ->
                BotLogCapture.clear()
                showSuccess("Logs limpiados")
            }
            .show()
    }

    private fun deleteBot() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Bot")
            .setMessage("¿Estás seguro de que quieres eliminar el bot instalado?")
            .setPositiveButton("Eliminar") { _, _ ->
                botRepository.deleteBot()
                preferencesManager.setBotJsUrl(null)
                preferencesManager.setBotJsEnabled(false)
                preferencesManager.setBotJsAutoUpdate(false)
                scheduleBotUpdateWorker(false)
                showSuccess("Bot eliminado")
                loadBotInfo()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun loadBotInfo() {
        val botInfo = botRepository.getInstalledBotInfo()
        
        if (botInfo != null) {
            botInfoCard.visibility = View.VISIBLE
            botUrlText.text = "URL: ${botInfo.url}"
            botHashText.text = "Hash: ${botInfo.hash.take(16)}..."
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(botInfo.timestamp))
            botLastUpdateText.text = "Última actualización: $dateStr"
        } else {
            botInfoCard.visibility = View.GONE
        }
        
        updateUIVisibility()
    }

    private fun updateUIVisibility() {
        val isEnabled = enableBotSwitch.isChecked
        val hasBot = botRepository.getInstalledBotInfo() != null
        
        botInfoCard.visibility = if (hasBot) View.VISIBLE else View.GONE
        testBotButton.isEnabled = isEnabled && hasBot
        deleteBotButton.isEnabled = hasBot
        autoUpdateSwitch.isEnabled = hasBot
    }

    private fun scheduleBotUpdateWorker(enabled: Boolean) {
        val workManager = WorkManager.getInstance(this)
        
        // Cancelar trabajo existente
        workManager.cancelUniqueWork(BotUpdateWorker.WORK_NAME)
        
        if (enabled) {
            // Programar actualización periódica cada 6 horas
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.UNMETERED) // WiFi
                .setRequiresBatteryNotLow(true)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<BotUpdateWorker>(
                6, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()
            
            workManager.enqueueUniquePeriodicWork(
                BotUpdateWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }
}

