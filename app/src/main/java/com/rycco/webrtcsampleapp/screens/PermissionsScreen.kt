package com.rycco.webrtcsampleapp.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.window.SplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.rycco.webrtcsampleapp.ui.theme.RED700
import com.rycco.webrtcsampleapp.ui.theme.Teal700
import com.rycco.webrtcsampleapp.ui.theme.WebRTCSampleAppTheme

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(navigateToHomeScreen: () -> Unit){
    val context = LocalContext.current
    val permission = rememberMultiplePermissionsState(listOf(Manifest.permission.CAMERA))
    if(permission.allPermissionsGranted){
        navigateToHomeScreen.invoke()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Let's Get You Connected",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Teal700
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "To start a video call, we need acess to your camera and microphone",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                permission.launchMultiplePermissionRequest()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Teal700
            )
        ) {
            Text(
                text = "Grand Permission now!"
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Why do we need these Permission?",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = "Camera: To share yor smiling face with others in video calls.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Teal700
                )
                Text(
                    text = "Microphone: So others can hear your voice loud and clear",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Teal700
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Why do we need these Permission?",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addCategory(Intent.CATEGORY_DEFAULT)
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
                                    or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        )
                    }
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = RED700
            )
        ) {
            Text(
                text = "Open App Settings"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPErmissionsScreen(){
    WebRTCSampleAppTheme {
        PermissionsScreen({})
    }
}