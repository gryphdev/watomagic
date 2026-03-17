package com.parishod.watomagic.model.utils

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NotificationHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        NotificationHelper.resetInstance()
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        NotificationHelper.resetInstance()
    }

    @Test
    fun `getInstance returns non-null instance`() {
        assertNotNull(NotificationHelper.getInstance(context))
    }

    @Test
    fun `getInstance returns same instance on repeated calls`() {
        val first = NotificationHelper.getInstance(context)
        val second = NotificationHelper.getInstance(context)
        assertSame(first, second)
    }

    @Test
    fun `getInstance creates fresh instance after reset`() {
        NotificationHelper.getInstance(context)
        NotificationHelper.resetInstance()
        assertNotNull(NotificationHelper.getInstance(context))
    }

    @Test
    fun `sendNotification posts notification to system`() {
        val helper = NotificationHelper.getInstance(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = Shadows.shadowOf(nm)
        val beforeCount = shadowNm.allNotifications.size
        helper.sendNotification("Test Title", "Test Message", "com.whatsapp")
        assert(shadowNm.allNotifications.size > beforeCount)
    }

    @Test
    fun `sendNotification adds app name prefix for supported apps`() {
        val helper = NotificationHelper.getInstance(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = Shadows.shadowOf(nm)
        helper.sendNotification("User123", "Hello", "com.whatsapp")
        val notifications = shadowNm.allNotifications
        assert(notifications.isNotEmpty()) { "Expected at least one notification" }
        val anyTitleContainsWhatsApp = notifications.any { notification ->
            val title = notification.extras?.getString("android.title") ?: ""
            title.contains("WhatsApp")
        }
        assert(anyTitleContainsWhatsApp) {
            val titles = notifications.map { it.extras?.getString("android.title") ?: "(null)" }
            "Expected at least one notification title to contain 'WhatsApp' but titles were: $titles"
        }
    }

    @Test
    fun `sendNotification creates summary notification for first notification of a package`() {
        val helper = NotificationHelper.getInstance(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = Shadows.shadowOf(nm)
        helper.sendNotification("User", "Message", "com.whatsapp")
        assert(shadowNm.allNotifications.size >= 2)
    }

    @Test
    fun `markNotificationDismissed does not throw for supported app`() {
        val helper = NotificationHelper.getInstance(context)
        helper.markNotificationDismissed("watomatic-com.whatsapp")
    }

    @Test
    fun `markNotificationDismissed does not throw for unknown package`() {
        val helper = NotificationHelper.getInstance(context)
        helper.markNotificationDismissed("watomatic-com.unknown.app")
    }

    @Test
    fun `getInstance creates notification channel`() {
        NotificationHelper.getInstance(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel(Constants.NOTIFICATION_CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(Constants.NOTIFICATION_CHANNEL_NAME, channel.name.toString())
    }
}
