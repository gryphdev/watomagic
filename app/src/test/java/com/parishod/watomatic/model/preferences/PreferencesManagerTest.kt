package com.parishod.watomagic.model.preferences

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.parishod.watomagic.model.App
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PreferencesManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: PreferencesManager

    @Before
    fun setUp() {
        PreferencesManager.resetInstance()
        context = ApplicationProvider.getApplicationContext()
        // Clear default shared prefs to get a truly fresh state each test
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        prefs = PreferencesManager.getPreferencesInstance(context)
    }

    @After
    fun tearDown() {
        PreferencesManager.resetInstance()
    }

    // --- Service enabled ---

    @Test
    fun `isServiceEnabled defaults to false`() {
        assertFalse(prefs.isServiceEnabled)
    }

    @Test
    fun `setServicePref to true enables service`() {
        prefs.setServicePref(true)
        assertTrue(prefs.isServiceEnabled)
    }

    @Test
    fun `setServicePref to false disables service`() {
        prefs.setServicePref(true)
        prefs.setServicePref(false)
        assertFalse(prefs.isServiceEnabled)
    }

    // --- Group reply ---

    @Test
    fun `isGroupReplyEnabled defaults to false`() {
        assertFalse(prefs.isGroupReplyEnabled)
    }

    @Test
    fun `setGroupReplyPref to true enables group reply`() {
        prefs.setGroupReplyPref(true)
        assertTrue(prefs.isGroupReplyEnabled)
    }

    @Test
    fun `setGroupReplyPref to false disables group reply`() {
        prefs.setGroupReplyPref(true)
        prefs.setGroupReplyPref(false)
        assertFalse(prefs.isGroupReplyEnabled)
    }

    // --- Auto reply delay ---

    @Test
    fun `getAutoReplyDelay defaults to 0`() {
        assertEquals(0L, prefs.autoReplyDelay)
    }

    @Test
    fun `setAutoReplyDelay persists the value`() {
        prefs.setAutoReplyDelay(5000L)
        assertEquals(5000L, prefs.autoReplyDelay)
    }

    @Test
    fun `setAutoReplyDelay stores zero correctly`() {
        prefs.setAutoReplyDelay(5000L)
        prefs.setAutoReplyDelay(0L)
        assertEquals(0L, prefs.autoReplyDelay)
    }

    // --- Watomatic attribution ---

    @Test
    fun `isAppendwatomagicAttributionEnabled defaults to false`() {
        assertFalse(prefs.isAppendwatomagicAttributionEnabled)
    }

    @Test
    fun `isContactReplyEnabled defaults to false`() {
        assertFalse(prefs.isContactReplyEnabled)
    }

    @Test
    fun `setContactReplyEnabled persists value`() {
        prefs.setContactReplyEnabled(true)
        assertTrue(prefs.isContactReplyEnabled)
    }

    // --- Contact reply mode (blacklist vs whitelist) ---

    @Test
    fun `isContactReplyBlacklistMode defaults to true`() {
        assertTrue(prefs.isContactReplyBlacklistMode)
    }

    @Test
    fun `setContactReplyBlacklistMode false switches to whitelist mode`() {
        prefs.setContactReplyBlacklistMode(false)
        assertFalse(prefs.isContactReplyBlacklistMode)
    }

    @Test
    fun `setContactReplyBlacklistMode true restores blacklist mode`() {
        prefs.setContactReplyBlacklistMode(false)
        prefs.setContactReplyBlacklistMode(true)
        assertTrue(prefs.isContactReplyBlacklistMode)
    }

    // --- AI replies ---

    @Test
    fun `isLoggedIn defaults to false`() {
        assertFalse(prefs.isLoggedIn)
    }

    @Test
    fun `setLoggedIn to true persists`() {
        prefs.setLoggedIn(true)
        assertTrue(prefs.isLoggedIn)
    }

    @Test
    fun `setLoggedIn to false persists`() {
        prefs.setLoggedIn(true)
        prefs.setLoggedIn(false)
        assertFalse(prefs.isLoggedIn)
    }

    // --- Guest mode ---

    @Test
    fun `isGuestMode defaults to false`() {
        assertFalse(prefs.isGuestMode)
    }

    @Test
    fun `setGuestMode to true persists`() {
        prefs.setGuestMode(true)
        assertTrue(prefs.isGuestMode)
    }

    // --- shouldShowLogin ---

    @Test
    fun `shouldShowLogin returns true when not logged in and not in guest mode`() {
        assertTrue(prefs.shouldShowLogin())
    }

    @Test
    fun `shouldShowLogin returns false when logged in`() {
        prefs.setLoggedIn(true)
        assertFalse(prefs.shouldShowLogin())
    }

    @Test
    fun `getOpenAICustomPrompt returns null by default`() {
        assertNull(prefs.openAICustomPrompt)
    }

    @Test
    fun `getOpenApiSource defaults to openai`() {
        assertEquals("openai", prefs.openApiSource)
    }

    @Test
    fun `saveOpenApiSource persists value`() {
        prefs.saveOpenApiSource("Claude")
        assertEquals("Claude", prefs.openApiSource)
    }

    // --- Custom OpenAI URL ---

    @Test
    fun `getCustomOpenAIApiUrl defaults to null`() {
        assertNull(prefs.customOpenAIApiUrl)
    }

    @Test
    fun `saveCustomOpenAIApiUrl persists value`() {
        prefs.saveCustomOpenAIApiUrl("https://my-api.example.com/")
        assertEquals("https://my-api.example.com/", prefs.customOpenAIApiUrl)
    }

    // --- OpenAI model ---

    @Test
    fun `getSelectedOpenAIModel defaults to null`() {
        assertNull(prefs.selectedOpenAIModel)
    }

    @Test
    fun `saveSelectedOpenAIModel persists value`() {
        prefs.saveSelectedOpenAIModel("gpt-4o")
        assertEquals("gpt-4o", prefs.selectedOpenAIModel)
    }

    // --- User email ---

    @Test
    fun `getUserEmail defaults to empty string`() {
        assertEquals("", prefs.userEmail)
    }

    @Test
    fun `setUserEmail persists value`() {
        prefs.setUserEmail("user@example.com")
        assertEquals("user@example.com", prefs.userEmail)
    }

    // --- Locale parsing ---

    @Test
    fun `updateLegacyLanguageKey migrates old format with r prefix`() {
        prefs.setLanguageStr("zh-rCN")
        prefs.updateLegacyLanguageKey()
        assertEquals("zh-CN", prefs.getSelectedLanguageStr(null))
    }

    @Test
    fun `updateLegacyLanguageKey does not change modern language-country format`() {
        prefs.setLanguageStr("zh-CN")
        prefs.updateLegacyLanguageKey()
        assertEquals("zh-CN", prefs.getSelectedLanguageStr(null))
    }

    @Test
    fun `updateLegacyLanguageKey does nothing when no language set`() {
        // Should not throw when called with no language stored
        prefs.updateLegacyLanguageKey()
        assertNull(prefs.getSelectedLanguageStr(null))
    }

    @Test
    fun `updateLegacyLanguageKey does not change language-only value`() {
        prefs.setLanguageStr("de")
        prefs.updateLegacyLanguageKey()
        assertEquals("de", prefs.getSelectedLanguageStr(null))
    }

    // --- Persistent AI errors ---

    @Test
    fun `getOpenAILastPersistentErrorMessage returns null by default`() {
        assertNull(prefs.openAILastPersistentErrorMessage)
    }

    @Test
    fun `saveOpenAILastPersistentError persists message and timestamp`() {
        val ts = System.currentTimeMillis()
        prefs.saveOpenAILastPersistentError("Rate limit exceeded", ts)
        assertEquals("Rate limit exceeded", prefs.openAILastPersistentErrorMessage)
        assertEquals(ts, prefs.openAILastPersistentErrorTimestamp)
    }

    @Test
    fun `clearOpenAILastPersistentError removes both message and timestamp`() {
        prefs.saveOpenAILastPersistentError("Some error", System.currentTimeMillis())
        prefs.clearOpenAILastPersistentError()
        assertNull(prefs.openAILastPersistentErrorMessage)
        assertEquals(0L, prefs.openAILastPersistentErrorTimestamp)
    }

    // --- Last verified time ---

    @Test
    fun `isShowNotificationEnabled is true after new install`() {
        // init() calls setShowNotificationPref(true) for new installs (fresh cleared prefs)
        assertTrue(prefs.isShowNotificationEnabled)
    }

    @Test
    fun `setShowNotificationPref persists false`() {
        prefs.setShowNotificationPref(false)
        assertFalse(prefs.isShowNotificationEnabled)
    }

    @Test
    fun `setShowNotificationPref persists true`() {
        prefs.setShowNotificationPref(false)
        prefs.setShowNotificationPref(true)
        assertTrue(prefs.isShowNotificationEnabled)
    }

    // --- GitHub release notes ID ---

    @Test
    fun `getGithubReleaseNotesId defaults to 0`() {
        assertEquals(0, prefs.getGithubReleaseNotesId())
    }

    @Test
    fun `setGithubReleaseNotesId persists value`() {
        prefs.setGithubReleaseNotesId(42)
        assertEquals(42, prefs.getGithubReleaseNotesId())
    }

    @Test
    fun `setGithubReleaseNotesId overwrites previous value`() {
        prefs.setGithubReleaseNotesId(100)
        prefs.setGithubReleaseNotesId(200)
        assertEquals(200, prefs.getGithubReleaseNotesId())
    }

    // --- Last purged time ---

    @Test
    fun `getLastPurgedTime defaults to 0`() {
        assertEquals(0L, prefs.getLastPurgedTime())
    }

    @Test
    fun `setPurgeMessageTime persists value`() {
        val time = 1_234_567_890L
        prefs.setPurgeMessageTime(time)
        assertEquals(time, prefs.getLastPurgedTime())
    }

    // --- Play store rating ---

    @Test
    fun `getPlayStoreRatingStatus defaults to empty string`() {
        assertEquals("", prefs.getPlayStoreRatingStatus())
    }

    @Test
    fun `setPlayStoreRatingStatus persists value`() {
        prefs.setPlayStoreRatingStatus("done")
        assertEquals("done", prefs.getPlayStoreRatingStatus())
    }

    @Test
    fun `getPlayStoreRatingLastTime defaults to 0`() {
        assertEquals(0L, prefs.getPlayStoreRatingLastTime())
    }

    @Test
    fun `setPlayStoreRatingLastTime persists value`() {
        val time = 9_876_543_210L
        prefs.setPlayStoreRatingLastTime(time)
        assertEquals(time, prefs.getPlayStoreRatingLastTime())
    }

    // --- Foreground service notification ---

    @Test
    fun `isForegroundServiceNotificationEnabled defaults to false`() {
        assertFalse(prefs.isForegroundServiceNotificationEnabled)
    }

    @Test
    fun `setShowForegroundServiceNotification persists true`() {
        prefs.setShowForegroundServiceNotification(true)
        assertTrue(prefs.isForegroundServiceNotificationEnabled)
    }

    @Test
    fun `setShowForegroundServiceNotification persists false`() {
        prefs.setShowForegroundServiceNotification(true)
        prefs.setShowForegroundServiceNotification(false)
        assertFalse(prefs.isForegroundServiceNotificationEnabled)
    }

    // --- Reply to names ---

    @Test
    fun `getReplyToNames defaults to empty set`() {
        assertTrue(prefs.getReplyToNames().isEmpty())
    }

    @Test
    fun `setReplyToNames persists single name`() {
        prefs.setReplyToNames(setOf("Alice"))
        assertEquals(setOf("Alice"), prefs.getReplyToNames())
    }

    @Test
    fun `setReplyToNames persists multiple names`() {
        val names = setOf("Alice", "Bob", "Charlie")
        prefs.setReplyToNames(names)
        assertEquals(names, prefs.getReplyToNames())
    }

    @Test
    fun `setReplyToNames can overwrite with empty set`() {
        prefs.setReplyToNames(setOf("Alice"))
        prefs.setReplyToNames(emptySet())
        assertTrue(prefs.getReplyToNames().isEmpty())
    }

    // --- Custom reply names ---

    @Test
    fun `getCustomReplyNames defaults to empty set`() {
        assertTrue(prefs.getCustomReplyNames().isEmpty())
    }

    @Test
    fun `setCustomReplyNames persists names`() {
        val names = setOf("Alice", "Bob")
        prefs.setCustomReplyNames(names)
        assertEquals(names, prefs.getCustomReplyNames())
    }

    // --- Generic getString / saveString ---

    @Test
    fun `saveString and getString round trip`() {
        prefs.saveString("test_custom_key", "test_custom_value")
        assertEquals("test_custom_value", prefs.getString("test_custom_key", "default"))
    }

    @Test
    fun `getString returns default when key not present`() {
        assertEquals("my_default", prefs.getString("nonexistent_key_xyz_abc", "my_default"))
    }

    @Test
    fun `isOpenAIRepliesEnabled defaults to false`() {
        @Suppress("DEPRECATION")
        assertFalse(prefs.isOpenAIRepliesEnabled())
    }

    @Test
    fun `setEnableOpenAIReplies persists value`() {
        prefs.setEnableOpenAIReplies(true)
        @Suppress("DEPRECATION")
        assertTrue(prefs.isOpenAIRepliesEnabled())
    }

    // --- Enabled apps ---

    @Test
    fun `saveEnabledApps by package name adds package`() {
        prefs.saveEnabledApps("com.whatsapp", true)
        assertTrue(prefs.isAppEnabled("com.whatsapp"))
    }

    @Test
    fun `saveEnabledApps by package name removes package`() {
        prefs.saveEnabledApps("com.whatsapp", true)
        prefs.saveEnabledApps("com.whatsapp", false)
        assertFalse(prefs.isAppEnabled("com.whatsapp"))
    }

    @Test
    fun `saveEnabledApps can add multiple packages`() {
        prefs.saveEnabledApps("com.whatsapp", true)
        prefs.saveEnabledApps("org.telegram.messenger", true)
        assertTrue(prefs.isAppEnabled("com.whatsapp"))
        assertTrue(prefs.isAppEnabled("org.telegram.messenger"))
    }

    @Test
    fun `saveEnabledApps with App object adds package`() {
        val app = App("WhatsApp", "com.whatsapp", false)
        prefs.saveEnabledApps(app, true)
        assertTrue(prefs.isAppEnabled(app))
    }

    @Test
    fun `saveEnabledApps with App object removes package`() {
        val app = App("WhatsApp", "com.whatsapp", false)
        prefs.saveEnabledApps(app, true)
        prefs.saveEnabledApps(app, false)
        assertFalse(prefs.isAppEnabled(app))
    }

    @Test
    fun `isAppEnabled returns false for package not in list`() {
        assertFalse(prefs.isAppEnabled("com.nonexistent.app.xyz"))
    }

    @Test
    fun `getEnabledApps returns set containing explicitly added package`() {
        prefs.saveEnabledApps("com.whatsapp", true)
        assertTrue(prefs.getEnabledApps().contains("com.whatsapp"))
    }

    // --- isFirstInstall ---

    @Test
    fun `isFirstInstall returns true in Robolectric test environment`() {
        // In Robolectric, firstInstallTime == lastUpdateTime (both 0), so returns true
        assertTrue(PreferencesManager.isFirstInstall(context))
    }

    // --- Edge cases ---

    @Test
    fun `getReplyToNames returns empty set when prefs have no value`() {
        val result = prefs.replyToNames
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getSelectedLanguage handles empty locale string`() {
        prefs.saveString("KEY_SELECTED_APP_LANGUAGE", "")
        val result = prefs.getString("KEY_SELECTED_APP_LANGUAGE", "en")
        assertNotNull(result)
    }

    @Test
    fun `resetInstance does not throw when called multiple times`() {
        PreferencesManager.resetInstance()
        PreferencesManager.resetInstance()
        prefs = PreferencesManager.getPreferencesInstance(context)
        assertNotNull(prefs)
    }

    // --- Show notification preference ---

    @Test
    fun `isShowNotificationEnabled is true after new install`() {
        // init() calls setShowNotificationPref(true) for new installs (fresh cleared prefs)
        assertTrue(prefs.isShowNotificationEnabled)
    }

    @Test
    fun `setShowNotificationPref persists false`() {
        prefs.setShowNotificationPref(false)
        assertFalse(prefs.isShowNotificationEnabled)
    }

    @Test
    fun `setShowNotificationPref persists true`() {
        prefs.setShowNotificationPref(false)
        prefs.setShowNotificationPref(true)
        assertTrue(prefs.isShowNotificationEnabled)
    }

    // --- GitHub release notes ID ---

    @Test
    fun `getGithubReleaseNotesId defaults to 0`() {
        assertEquals(0, prefs.getGithubReleaseNotesId())
    }

    @Test
    fun `setGithubReleaseNotesId persists value`() {
        prefs.setGithubReleaseNotesId(42)
        assertEquals(42, prefs.getGithubReleaseNotesId())
    }

    @Test
    fun `setGithubReleaseNotesId overwrites previous value`() {
        prefs.setGithubReleaseNotesId(100)
        prefs.setGithubReleaseNotesId(200)
        assertEquals(200, prefs.getGithubReleaseNotesId())
    }

    // --- Last purged time ---

    @Test
    fun `getLastPurgedTime defaults to 0`() {
        assertEquals(0L, prefs.getLastPurgedTime())
    }

    @Test
    fun `setPurgeMessageTime persists value`() {
        val time = 1_234_567_890L
        prefs.setPurgeMessageTime(time)
        assertEquals(time, prefs.getLastPurgedTime())
    }

    // --- Play store rating ---

    @Test
    fun `getPlayStoreRatingStatus defaults to empty string`() {
        assertEquals("", prefs.getPlayStoreRatingStatus())
    }

    @Test
    fun `setPlayStoreRatingStatus persists value`() {
        prefs.setPlayStoreRatingStatus("done")
        assertEquals("done", prefs.getPlayStoreRatingStatus())
    }

    @Test
    fun `getPlayStoreRatingLastTime defaults to 0`() {
        assertEquals(0L, prefs.getPlayStoreRatingLastTime())
    }

    @Test
    fun `setPlayStoreRatingLastTime persists value`() {
        val time = 9_876_543_210L
        prefs.setPlayStoreRatingLastTime(time)
        assertEquals(time, prefs.getPlayStoreRatingLastTime())
    }

    // --- Foreground service notification ---

    @Test
    fun `isForegroundServiceNotificationEnabled defaults to false`() {
        assertFalse(prefs.isForegroundServiceNotificationEnabled)
    }

    @Test
    fun `setShowForegroundServiceNotification persists true`() {
        prefs.setShowForegroundServiceNotification(true)
        assertTrue(prefs.isForegroundServiceNotificationEnabled)
    }

    @Test
    fun `setShowForegroundServiceNotification persists false`() {
        prefs.setShowForegroundServiceNotification(true)
        prefs.setShowForegroundServiceNotification(false)
        assertFalse(prefs.isForegroundServiceNotificationEnabled)
    }

    // --- Reply to names ---

    @Test
    fun `getReplyToNames defaults to empty set`() {
        assertTrue(prefs.getReplyToNames().isEmpty())
    }

    @Test
    fun `setReplyToNames persists single name`() {
        prefs.setReplyToNames(setOf("Alice"))
        assertEquals(setOf("Alice"), prefs.getReplyToNames())
    }

    @Test
    fun `setReplyToNames persists multiple names`() {
        val names = setOf("Alice", "Bob", "Charlie")
        prefs.setReplyToNames(names)
        assertEquals(names, prefs.getReplyToNames())
    }

    @Test
    fun `setReplyToNames can overwrite with empty set`() {
        prefs.setReplyToNames(setOf("Alice"))
        prefs.setReplyToNames(emptySet())
        assertTrue(prefs.getReplyToNames().isEmpty())
    }

    // --- Custom reply names ---

    @Test
    fun `getCustomReplyNames defaults to empty set`() {
        assertTrue(prefs.getCustomReplyNames().isEmpty())
    }

    @Test
    fun `setCustomReplyNames persists names`() {
        val names = setOf("Alice", "Bob")
        prefs.setCustomReplyNames(names)
        assertEquals(names, prefs.getCustomReplyNames())
    }

    // --- Generic getString / saveString ---

    @Test
    fun `saveString and getString round trip`() {
        prefs.saveString("test_custom_key", "test_custom_value")
        assertEquals("test_custom_value", prefs.getString("test_custom_key", "default"))
    }

    @Test
    fun `getString returns default when key not present`() {
        assertEquals("my_default", prefs.getString("nonexistent_key_xyz_abc", "my_default"))
    }

    @Test
    fun `saveString overwrites previous value`() {
        prefs.saveString("overwrite_key", "first_value")
        prefs.saveString("overwrite_key", "second_value")
        assertEquals("second_value", prefs.getString("overwrite_key", "default"))
    }

    // --- Deprecated isOpenAIRepliesEnabled ---

    @Test
    fun `isOpenAIRepliesEnabled defaults to false`() {
        @Suppress("DEPRECATION")
        assertFalse(prefs.isOpenAIRepliesEnabled())
    }

    @Test
    fun `setEnableOpenAIReplies persists value`() {
        prefs.setEnableOpenAIReplies(true)
        @Suppress("DEPRECATION")
        assertTrue(prefs.isOpenAIRepliesEnabled())
    }

    // --- Enabled apps ---

    @Test
    fun `saveEnabledApps by package name adds package`() {
        prefs.saveEnabledApps("com.whatsapp", true)
        assertTrue(prefs.isAppEnabled("com.whatsapp"))
    }

    @Test
    fun `saveEnabledApps by package name removes package`() {
        prefs.saveEnabledApps("com.whatsapp", true)
        prefs.saveEnabledApps("com.whatsapp", false)
        assertFalse(prefs.isAppEnabled("com.whatsapp"))
    }

    @Test
    fun `saveEnabledApps can add multiple packages`() {
        prefs.saveEnabledApps("com.whatsapp", true)
        prefs.saveEnabledApps("org.telegram.messenger", true)
        assertTrue(prefs.isAppEnabled("com.whatsapp"))
        assertTrue(prefs.isAppEnabled("org.telegram.messenger"))
    }

    @Test
    fun `saveEnabledApps with App object adds package`() {
        val app = App("WhatsApp", "com.whatsapp", false)
        prefs.saveEnabledApps(app, true)
        assertTrue(prefs.isAppEnabled(app))
    }

    @Test
    fun `saveEnabledApps with App object removes package`() {
        val app = App("WhatsApp", "com.whatsapp", false)
        prefs.saveEnabledApps(app, true)
        prefs.saveEnabledApps(app, false)
        assertFalse(prefs.isAppEnabled(app))
    }

    @Test
    fun `isAppEnabled returns false for package not in list`() {
        assertFalse(prefs.isAppEnabled("com.nonexistent.app.xyz"))
    }

    @Test
    fun `getEnabledApps returns set containing explicitly added package`() {
        prefs.saveEnabledApps("com.whatsapp", true)
        assertTrue(prefs.getEnabledApps().contains("com.whatsapp"))
    }

    // --- isFirstInstall ---

    @Test
    fun `isFirstInstall returns true in Robolectric test environment`() {
        // In Robolectric, firstInstallTime == lastUpdateTime (both 0), so returns true
        assertTrue(PreferencesManager.isFirstInstall(context))
    }
}
