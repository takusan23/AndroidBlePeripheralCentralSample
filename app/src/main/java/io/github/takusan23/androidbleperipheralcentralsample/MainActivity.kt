package io.github.takusan23.androidbleperipheralcentralsample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.takusan23.androidbleperipheralcentralsample.ui.theme.AndroidBlePeripheralCentralSampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidBlePeripheralCentralSampleTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onPeripheralClick = { navController.navigate("peripheral") },
                onCentralClick = { navController.navigate("central") }
            )
        }
        composable("peripheral") {
            PeripheralScreen()
        }
        composable("central") {
            CentralScreen()
        }
    }
}