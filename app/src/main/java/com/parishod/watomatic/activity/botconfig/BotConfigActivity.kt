package com.parishod.watomagic.activity.botconfig

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.parishod.watomagic.R
import com.parishod.watomagic.activity.BaseActivity
import com.parishod.watomagic.botjs.BotRepository
import com.parishod.watomagic.model.preferences.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BotConfigActivity : BaseActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var botRepository: BotRepository
    
    private lateinit var enableBotSwitch: SwitchMaterial
    private lateinit var botUrlInput: TextInputEditText
    private lateinit var downloadBotButton: Button
    private lateinit var downloadProgress: ProgressBar
    private lateinit var botInfoCard: View
    private lateinit var botUrlText: TextView
    private lateinit var botLastUpdateText: TextView
    private lateinit var testBotButton: Button
    private lateinit var autoUpdateSwitch: SwitchMaterial
    private lateinit var deleteBotButton: Button

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
        }

        downloadBotButton.setOnClickListener { downloadBot() }
        testBotButton.setOnClickListener { testBot() }
        deleteBotButton.setOnClickListener { deleteBot() }

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
        showSuccess("Función de prueba en desarrollo")
        // TODO: Implementar test con notificación dummy
    }

    private fun deleteBot() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Bot")
            .setMessage("¿Estás seguro de que quieres eliminar el bot instalado?")
            .setPositiveButton("Eliminar") { _, _ ->
                botRepository.deleteBot()
                preferencesManager.setBotJsUrl(null)
                preferencesManager.setBotJsEnabled(false)
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
        // TODO: Mostrar/ocultar elementos según el estado
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }
}
