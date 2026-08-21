package com.cch.momentmark.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MomentMarkSettingsStoreTest {
    @Test
    fun themeModeRoundTripsAndDefaultsToSystem() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = MomentMarkSettingsStore(context)
        store.clear()

        assertEquals(ThemeMode.SYSTEM, store.themeMode.first())
        store.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, store.themeMode.first())
        store.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, store.themeMode.first())

        store.clear()
    }
}
