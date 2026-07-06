package com.parishod.watomagic

import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.parishod.watomagic.activity.customreplyeditor.CustomReplyEditorActivity
import com.parishod.watomagic.model.CustomRepliesData
import com.parishod.watomagic.model.preferences.PreferencesManager
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class CustomReplyEditorActivityTest {

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
        CustomRepliesData.resetInstance()
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
        CustomRepliesData.resetInstance()
    }

    private fun launchActivity(): ActivityScenario<CustomReplyEditorActivity> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            CustomReplyEditorActivity::class.java
        )
        return ActivityScenario.launch(intent)
    }

    @Test
    fun activityLaunchesSuccessfully() {
        val scenario = launchActivity()
        scenario.onActivity { activity -> assertNotNull(activity) }
        scenario.close()
    }

    @Test
    fun toolbarIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun autoReplyInputIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.autoReplyTextInputLayout)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun aiProviderCardIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.ai_provider_card)).perform(scrollTo()).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun saveButtonIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.saveCustomReplyBtn)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun scrollViewIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.custom_reply_editor_scroll_view)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun activityCanBeRecreated() {
        val scenario = launchActivity()
        scenario.recreate()
        scenario.onActivity { activity -> assertNotNull(activity) }
        scenario.close()
    }
}
