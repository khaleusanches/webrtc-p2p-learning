package com.rycco.webrtcsampleapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.firestore.FirebaseFirestore
import org.webrtc.SurfaceViewRenderer

@Composable
fun VideoCallScreen(roomId : String){
    // create room
    val firestore = remember { FirebaseFirestore.getInstance() }

    fun checkRoomCapacityAndSetup(){
        val roomDocRef = firestore.collection("rooms").document(roomId)
        roomDocRef
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context)
            },
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context)
            },
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        )
    }
}
