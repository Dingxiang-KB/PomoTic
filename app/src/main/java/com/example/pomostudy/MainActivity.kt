package com.example.pomostudy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pomostudy.data.repository.AppSettings
import com.example.pomostudy.ui.MainScreen
import com.example.pomostudy.ui.theme.PomoStudyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PomoStudyApp).container

        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            PomoStudyTheme(themeMode = settings.themeMode) {
                MainScreen(container = container)
            }
        }
    }
}
