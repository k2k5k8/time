package com.cch.momentmark

import android.os.Bundle
import android.os.Build
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cch.momentmark.ui.MomentMarkApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferHighestRefreshRateAtCurrentResolution()
        setContent {
            MomentMarkApp()
        }
    }

    override fun onResume() {
        super.onResume()
        // Display mode can change while the app is backgrounded (for example when the user
        // changes the system refresh-rate setting), so renew this best-effort preference.
        preferHighestRefreshRateAtCurrentResolution()
    }

    /**
     * Requests the fastest mode offered at the display's current physical resolution.
     *
     * This is a window preference, not a forced refresh rate: Android may still lower the
     * frequency for battery saver, thermal throttling, or device-specific display policy.
     * Keeping the current resolution avoids selecting a lower-resolution high-refresh mode.
     */
    private fun preferHighestRefreshRateAtCurrentResolution() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val display = window.decorView.display ?: return
        val currentMode = display.mode
        val preferredMode = display.supportedModes
            .asSequence()
            .filter {
                it.physicalWidth == currentMode.physicalWidth &&
                    it.physicalHeight == currentMode.physicalHeight
            }
            .maxByOrNull(Display.Mode::getRefreshRate)
            ?: return

        if (preferredMode.modeId != currentMode.modeId) {
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = preferredMode.modeId
            }
        }
    }
}
