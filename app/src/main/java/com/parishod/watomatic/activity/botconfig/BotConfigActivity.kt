package com.parishod.watomagic.activity.botconfig

import android.os.Bundle
import android.text.format.DateFormat
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.parishod.watomagic.R
import com.parishod.watomagic.activity.BaseActivity
import com.parishod.watomagic.botjs.BotJsEngine
import com.parishod.watomagic.botjs.BotRepository
import com.parishod.watomagic.model.NotificationData
import com.parishod.watomagic.model.preferences.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BotConfigActivity : BaseActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var botRepository: BotRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bot_config)

        preferencesManager = PreferencesManager.getPreferencesInstance(this)
        botRepository = BotRepository(this)

        setupToolbar()
        setupBotConfig()
        loadBotInfo()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupBotConfig() {
        val enableBotSwitch = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.enableBotSwitch)
        val downloadBotButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.downloadBotButton)
        val botUrlInput = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.botUrlInput)
        val autoUpdateSwitch = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.autoUpdateSwitch)
        val testBotButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.testBotButton)
        val deleteBotButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.deleteBotButton)

        // Load current settings
        enableBotSwitch.isChecked = preferencesManager.isBotJsEnabled()
        autoUpdateSwitch.isChecked = preferencesManager.isBotAutoUpdateEnabled()
        botUrlInput.setText(preferencesManager.getBotJsUrl() ?: "")

        // Enable/Disable BotJS
        enableBotSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBotJsEnabled(isChecked)
            updateUIVisibility()
        }

        // Download bot
        downloadBotButton.setOnClickListener {
            downloadBot()
        }

        // Auto-update toggle
        autoUpdateSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBotAutoUpdateEnabled(isChecked)
        }

        // Test bot
        testBotButton.setOnClickListener {
            testBot()
        }

        // Delete bot
        deleteBotButton.setOnClickListener {
            showDeleteConfirmation()
        }

        updateUIVisibility()
    }

    private fun downloadBot() {
        val botUrlInput = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.botUrlInput)
        val url = botUrlInput.text.toString().trim()

        if (url.isEmpty()) {
            showError(getString(R.string.error_invalid_url))
            return
        }

        if (!url.startsWith("https://")) {
            showError("Only HTTPS URLs are allowed")
            return
        }

        val downloadProgress = findViewById<View>(R.id.downloadProgress)
        downloadProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                botRepository.downloadBot(url)
            }

            downloadProgress.visibility = View.GONE

            if (result.isSuccess) {
                preferencesManager.setBotJsUrl(url)
                showSuccess(getString(R.string.bot_download_success))
                loadBotInfo()
            } else {
                showError(getString(R.string.bot_download_error, result.getError()))
            }
        }
    }

    private fun testBot() {
        lifecycleScope.launch {
            val progressBar = findViewById<View>(R.id.downloadProgress)
            progressBar.visibility = View.VISIBLE

            try {
                val result = withContext(Dispatchers.IO) {
                    // Create a test notification
                    val testNotification = NotificationData(
                        1,
                        "com.whatsapp",
                        "Test User",
                        "This is a test message",
                        System.currentTimeMillis(),
                        false,
                        emptyArray()
                    )

                    // Load and execute bot
                    val botFile = java.io.File(filesDir, "bots/active-bot.js")
                    if (!botFile.exists()) {
                        throw Exception("Bot not installed")
                    }

                    val jsCode = botFile.readText()
                    val engine = BotJsEngine(this@BotConfigActivity)
                    engine.initialize()

                    val notificationJson = String.format(
                        """{"id":%d,"appPackage":"%s","title":"%s","body":"%s","timestamp":%d,"isGroup":%s,"actions":[]}""",
                        testNotification.id,
                        testNotification.appPackage,
                        testNotification.title,
                        testNotification.body,
                        testNotification.timestamp,
                        testNotification.isGroup
                    )

                    val response = engine.executeBot(jsCode, notificationJson)
                    engine.cleanup()

                    response
                }

                progressBar.visibility = View.GONE
                showSuccess(getString(R.string.bot_test_success))
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                showError(getString(R.string.bot_test_error, e.message))
            }
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_bot))
            .setMessage("Are you sure you want to delete the installed bot?")
            .setPositiveButton(android.R.string.ok) { _, _ ->
                botRepository.deleteBot()
                preferencesManager.setBotJsUrl(null)
                showSuccess(getString(R.string.bot_deleted))
                loadBotInfo()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun loadBotInfo() {
        val botInfo = botRepository.getInstalledBotInfo()
        val botInfoCard = findViewById<View>(R.id.botInfoCard)
        val botUrlText = findViewById<android.widget.TextView>(R.id.botUrlText)
        val botLastUpdateText = findViewById<android.widget.TextView>(R.id.botLastUpdateText)

        if (botInfo != null) {
            botInfoCard.visibility = View.VISIBLE
            botUrlText.text = "URL: ${botInfo.url}"
            val dateFormat = DateFormat.getDateFormat(this)
            val timeFormat = DateFormat.getTimeFormat(this)
            val dateTime = java.util.Date(botInfo.timestamp)
            botLastUpdateText.text = getString(R.string.bot_last_updated, 
                "${dateFormat.format(dateTime)} ${timeFormat.format(dateTime)}")
        } else {
            botInfoCard.visibility = View.GONE
        }
    }

    private fun updateUIVisibility() {
        val isEnabled = preferencesManager.isBotJsEnabled()
        val downloadUrlCard = findViewById<View>(R.id.downloadUrlCard)
        downloadUrlCard.visibility = if (isEnabled) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
