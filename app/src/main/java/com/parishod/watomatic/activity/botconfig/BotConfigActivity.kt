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
import com.parishod.watomagic.model.preferences.PreferencesManager
import com.parishod.watomagic.replyproviders.BotJsReplyProvider
import com.parishod.watomagic.replyproviders.NotificationData
import com.parishod.watomagic.workers.BotUpdateWorker
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

        // Cargar URL si existe
        preferencesManager.getBotJsUrl()?.let {
            botUrlInput.setText(it)
        }
    }

    private fun downloadBot() {
        val url = botUrlInput.text?.toString()?.trim() ?: ""

        if (url.isEmpty()) {
            showError(getString(R.string.enter_url))
            return
        }

        if (!url.startsWith("https://")) {
            showError(getString(R.string.https_only))
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
                showSuccess(getString(R.string.bot_downloaded_successfully))
                preferencesManager.setBotJsUrl(url)
                loadBotInfo()
                // Schedule update worker if auto-update is enabled
                if (preferencesManager.isBotJsAutoUpdateEnabled()) {
                    scheduleBotUpdateWorker(true)
                }
            } else {
                val errorMsg = result.getError() ?: getString(R.string.download_failed, "Unknown error")
                showError(getString(R.string.download_failed, errorMsg))
            }
        }
    }

    private fun testBot() {
        val botInfo = botRepository.getInstalledBotInfo()
        if (botInfo == null) {
            showError(getString(R.string.no_bot_installed))
            return
        }

        testBotButton.isEnabled = false
        downloadProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Create a dummy notification for testing
                val testNotification = NotificationData(
                    id = 999,
                    appPackage = "com.whatsapp",
                    title = "Test Contact",
                    body = "This is a test message for bot validation",
                    timestamp = System.currentTimeMillis(),
                    isGroup = false,
                    actions = Collections.emptyList()
                )

                val result = withContext(Dispatchers.IO) {
                    try {
                        val provider = BotJsReplyProvider()
                        var testResult: String? = null
                        var testError: String? = null

                        provider.generateReply(
                            this@BotConfigActivity,
                            testNotification.body,
                            testNotification,
                            object : com.parishod.watomagic.replyproviders.ReplyProvider.ReplyCallback {
                                override fun onSuccess(reply: String) {
                                    testResult = reply
                                }

                                override fun onFailure(error: String) {
                                    testError = error
                                }
                            }
                        )

                        // Wait a bit for async execution
                        Thread.sleep(2000)

                        if (testResult != null) {
                            "SUCCESS: $testResult"
                        } else if (testError != null) {
                            "ACTION: $testError"
                        } else {
                            "TIMEOUT: No response received"
                        }
                    } catch (e: Exception) {
                        "ERROR: ${e.message}"
                    }
                }

                downloadProgress.visibility = View.GONE
                testBotButton.isEnabled = true

                if (result.startsWith("SUCCESS:")) {
                    val reply = result.substringAfter("SUCCESS: ")
                    showSuccess(getString(R.string.bot_test_success, "REPLY") + "\n" + 
                               getString(R.string.bot_test_reply, reply))
                } else if (result.startsWith("ACTION:")) {
                    val action = result.substringAfter("ACTION: ")
                    showSuccess(getString(R.string.bot_test_success, action))
                } else {
                    showError(getString(R.string.bot_test_failed, result))
                }
            } catch (e: Exception) {
                downloadProgress.visibility = View.GONE
                testBotButton.isEnabled = true
                showError(getString(R.string.bot_test_failed, e.message ?: "Unknown error"))
            }
        }
    }

    private fun deleteBot() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_bot))
            .setMessage(getString(R.string.delete_bot_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                botRepository.deleteBot()
                preferencesManager.setBotJsUrl(null)
                preferencesManager.setBotJsEnabled(false)
                preferencesManager.setBotJsAutoUpdate(false)
                showSuccess(getString(R.string.bot_deleted_successfully))
                loadBotInfo()
                // Cancel scheduled worker
                WorkManager.getInstance(this).cancelAllWorkByTag("BotUpdateWorker")
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun loadBotInfo() {
        val botInfo = botRepository.getInstalledBotInfo()
        
        if (botInfo != null) {
            botInfoCard.visibility = View.VISIBLE
            botUrlText.text = getString(R.string.bot_info_url, botInfo.url)
            
            if (botInfo.hash.isNotEmpty()) {
                botHashText.visibility = View.VISIBLE
                botHashText.text = getString(R.string.bot_info_hash, botInfo.hash.substring(0, minOf(16, botInfo.hash.length)) + "...")
            } else {
                botHashText.visibility = View.GONE
            }
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(botInfo.timestamp))
            botLastUpdateText.text = getString(R.string.bot_info_date, dateStr)
        } else {
            botInfoCard.visibility = View.GONE
        }
        
        updateUIVisibility()
    }

    private fun updateUIVisibility() {
        val isEnabled = enableBotSwitch.isChecked
        val hasBot = botRepository.getInstalledBotInfo() != null
        
        // Show/hide elements based on state
        botInfoCard.visibility = if (hasBot) View.VISIBLE else View.GONE
        testBotButton.isEnabled = isEnabled && hasBot
        deleteBotButton.isEnabled = hasBot
        autoUpdateSwitch.isEnabled = hasBot
    }

    private fun scheduleBotUpdateWorker(enabled: Boolean) {
        val workManager = WorkManager.getInstance(this)
        
        if (enabled && preferencesManager.isBotJsEnabled() && 
            !preferencesManager.getBotJsUrl().isNullOrEmpty()) {
            // Schedule periodic work every 6 hours
            val request = PeriodicWorkRequestBuilder<BotUpdateWorker>(6, TimeUnit.HOURS)
                .addTag("BotUpdateWorker")
                .build()
            workManager.enqueue(request)
        } else {
            // Cancel scheduled work
            workManager.cancelAllWorkByTag("BotUpdateWorker")
        }
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }
}
