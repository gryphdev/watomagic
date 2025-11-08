package com.parishod.watomagic.activity.contactselector

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.parishod.watomagic.R
import com.parishod.watomagic.activity.BaseActivity
import com.parishod.watomagic.databinding.ActivityContactSelectorBinding
import com.parishod.watomagic.fragment.ContactSelectorFragment
import com.parishod.watomagic.model.utils.ContactsHelper
import com.parishod.watomagic.viewmodel.SwipeToKillAppDetectViewModel

import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class ContactSelectorActivity : BaseActivity() {
    private lateinit var contactSelectorFragment: ContactSelectorFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val binding = ActivityContactSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.contact_selector)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        contactSelectorFragment = supportFragmentManager.findFragmentById(R.id.contact_selector_layout)
                as ContactSelectorFragment

        ViewModelProvider(this).get(SwipeToKillAppDetectViewModel::class.java)

        ViewCompat.setOnApplyWindowInsetsListener(binding.contactSelectorRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ContactsHelper.CONTACT_PERMISSION_REQUEST_CODE && this::contactSelectorFragment.isInitialized) {
            contactSelectorFragment.loadContactList()
        }
    }
}