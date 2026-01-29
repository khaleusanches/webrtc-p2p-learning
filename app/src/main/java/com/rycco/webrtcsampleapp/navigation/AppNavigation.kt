package com.rycco.webrtcsampleapp.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rycco.webrtcsampleapp.screens.HomeScreen
import com.rycco.webrtcsampleapp.screens.PermissionsScreen
import com.rycco.webrtcsampleapp.screens.VideoCallScreen
import timber.log.Timber
import java.security.Permission

@Composable
fun WebRTCAppNavigation(){
    CompositionLocalProvider(
        LocalNavController provides rememberNavController()
    ) {
        SetupNavigation()
    }
}

@Composable
fun SetupNavigation(){
    val navController = LocalNavController.current
    val context = LocalContext.current

    val isCameraPermissionGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    NavHost(
        navController = navController,
        startDestination = if(isCameraPermissionGranted) "Home" else "Permissions"
    ){
        composable(route = "Home") {
            HomeScreen(navigateToCallScreen = { roomId ->
                navController.navigate("VideoCall/${roomId}") {
                    popUpTo(0){inclusive = true}
                    launchSingleTop = true
                }
            })
        }
        composable ("Permissions") {
            PermissionsScreen(navigateToHomeScreen = {
                navController.navigate("Home") {
                    popUpTo(0){inclusive = true}
                    launchSingleTop = true
                }
            })
        }
        composable ("VideoCall/{roomId}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId", "")
            if(roomId.isNullOrEmpty()){
                Timber.e("Room ID not available")
                navController.navigate("Home") {
                    popUpTo(0){inclusive = true}
                    launchSingleTop = true
                }
            }
            else {
                VideoCallScreen(roomId)
            }
        }
    }

}

val LocalNavController = compositionLocalOf<NavHostController> { error("Erro no local provided") }