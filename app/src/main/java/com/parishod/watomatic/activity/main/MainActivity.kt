package com.parishod.watomagic.activity.main

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.parishod.watomagic.R
import com.parishod.watomagic.activity.BaseActivity
import com.parishod.watomagic.flavor.FlavorNavigator
import com.parishod.watomagic.model.preferences.PreferencesManager
import com.parishod.watomagic.viewmodel.SwipeToKillAppDetectViewModel

import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.parishod.watomagic.service.NlsHealthCheckWorker

class MainActivity : BaseActivity() {
    private lateinit var viewModel: SwipeToKillAppDetectViewModel
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesManager = PreferencesManager.getPreferencesInstance(this)

        // Flavor-aware login navigation: Only GooglePlay flavor has LoginActivity
        if (FlavorNavigator.navigateToLoginIfNeeded(this, preferencesManager)) {
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        setTitle(R.string.app_name)

        viewModel = ViewModelProvider(this)[SwipeToKillAppDetectViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_frame_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Schedule health check
        NlsHealthCheckWorker.schedule(this)
    }
}