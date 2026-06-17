package com.example.slideshowclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.slideshowclock.ui.settings.SettingsScreen
import com.example.slideshowclock.ui.slideshow.SlideshowScreen
import com.example.slideshowclock.ui.theme.SlideshowClockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SlideshowClockTheme {
                AppNavHost()
            }
        }
    }
}

private object Routes {
    const val SLIDESHOW = "slideshow"
    const val SETTINGS = "settings"
}

@Composable
private fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.SLIDESHOW) {
        composable(Routes.SLIDESHOW) {
            SlideshowScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
