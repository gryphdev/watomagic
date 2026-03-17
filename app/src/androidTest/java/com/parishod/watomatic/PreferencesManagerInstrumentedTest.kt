package com.parishod.watomagic

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.parishod.watomagic.model.preferences.PreferencesManager as AppPreferencesManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesManagerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var prefs: AppPreferencesManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        AppPreferencesManager.resetInstance()
        prefs = AppPreferencesManager.getPreferencesInstance(context)
    }

    @After
    fun tearDown() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        AppPreferencesManager.resetInstance()
    }

    @Test
    fun preferencesManagerIsNotNull() {
        assertNotNull(prefs)
    }

    @Test
    fun serviceEnabledDefaultsToFalseOnFreshInstance() {
        assertFalse(prefs.isServiceEnabled)
    }

    @Test
    fun setAndGetServiceEnabled() {
        prefs.setServicePref(true)
        assertTrue(prefs.isServiceEnabled)
        prefs.setServicePref(false)
        assertFalse(prefs.isServiceEnabled)
    }

    @Test
    fun saveAndRetrieveOpenAIApiKey() {
        val testApiKey = "sk-test-instrumented-key"
        prefs.saveOpenAIApiKey(testApiKey)
        val retrieved = prefs.getOpenAIApiKey()
        if (retrieved != null) {
            assertEquals(testApiKey, retrieved)
        }
        prefs.deleteOpenAIApiKey()
    }

    @Test
    fun deleteOpenAIApiKeyRemovesIt() {
        prefs.saveOpenAIApiKey("sk-key-to-delete")
        prefs.deleteOpenAIApiKey()
        assertNull(prefs.getOpenAIApiKey())
    }

    @Test
    fun setAndGetUserEmail() {
        prefs.setUserEmail("test@example.com")
        assertEquals("test@example.com", prefs.userEmail)
    }

    @Test
    fun guestModeAndLoginInteraction() {
        prefs.setLoggedIn(false)
        prefs.setGuestMode(false)
        assertTrue(prefs.shouldShowLogin())

        prefs.setGuestMode(true)
        assertFalse(prefs.shouldShowLogin())

        prefs.setGuestMode(false)
        prefs.setLoggedIn(true)
        assertFalse(prefs.shouldShowLogin())
    }

    @Test
    fun botJsEnabledFlag() {
        assertFalse(prefs.isBotJsEnabled)
        prefs.setBotJsEnabled(true)
        assertTrue(prefs.isBotJsEnabled)
    }
}
