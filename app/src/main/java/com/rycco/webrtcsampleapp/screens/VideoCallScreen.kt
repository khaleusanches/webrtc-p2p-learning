package com.rycco.webrtcsampleapp.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import timber.log.Timber
import java.util.concurrent.Executors

@Composable
fun VideoCallScreen(roomId : String, onNavigateBack : () -> Unit){
    // create room
    val executor = remember { Executors.newSingleThreadExecutor() }

    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val elgBase = remember { EglBase.create() }

    var peerConnectionFactory : PeerConnectionFactory? = remember { null }
    var peerConnector : PeerConnection? = remember { null }

    val localCandidatesToShare = remember { arrayListOf<Map<String, Any?>>()}
    val remoteCandidates = remember { arrayListOf<IceCandidate>()}
    var isOfferer = remember { false }
    var remoteDescriptSet = remember { false }

    var sdpMediaConstraints : MediaConstraints = remember { MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiverVideo", true.toString()))
    } }

    //Renderizador

    var localRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null)}
    var remoteRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null)}

    //Sinalização

    fun sendSignallingMessage(message : Map<String, Any?>){
        Timber.d("sendSignallingMessage() :: ")

        val signallingRef = firestore.collection("rooms").document(roomId)
        signallingRef.set(message, SetOptions.merge())
    }

    var localSdp : SessionDescription? = remember { null }
    val sdpObserver = object : SdpObserver {
        override fun onCreateSuccess(sessionDescription : SessionDescription) {
            Timber.d("onCreateSucess")
            if(localSdp != null){
                Timber.d("Multiple Session Description created")
                return
            }
            localSdp = sessionDescription
            executor.execute {
                peerConnector?.setLocalDescription(this, localSdp)
            }
        }
        override fun onSetSuccess() {
            if(localSdp == null) return
            executor.execute {
                if (isOfferer){
                    if(peerConnector?.remoteDescription == null){
                        sendSignallingMessage(
                            mapOf(
                                "type" to "offer",
                                "sdpOffer" to localSdp.description
                            )
                        )
                    }
                    else {
                        remoteDescriptSet = true
                        addQueuedCandidates()
                    }
                } else {
                    if (peerConnector?.localDescription != null){
                        sendSignallingMessage(
                            mapOf(
                                "type" to "answer",
                                "sdpAnswer" to localSdp.description
                            )
                        )
                        remoteDescriptSet = true
                        addQueuedCandidates()
                    }
                }
            }
        }

        private fun addQueuedCandidates() {
            remoteCandidates.forEach {
                peerConnector?.addIceCandidate(it)
            }
            remoteCandidates.clear()
        }

        override fun onCreateFailure(p0: String?) {
        }
        override fun onSetFailure(p0: String?) {
        }
    }

    fun createAnswer(){
        Timber.d("createAnswer")
        peerConnector?.createAnswer(sdpObserver, sdpMediaConstraints)
    }
    fun createOffer(){
        Timber.d("createOffer()")
        peerConnector?.createOffer(sdpObserver, sdpMediaConstraints)
    }

    fun handleSignallingMessages(data: Map<String, Any>){
        //candidates.
        if(isOfferer && data["iceAnswer"] != null){
            executor.execute{
                val cMDDataList = data["iceAnswer"] as List<*>
                cMDDataList.forEach { map ->
                    val cData = map as HashMap<*,*>
                    val candidate = IceCandidate(
                        cData["sdpMid"] as String,
                        (cData["sdpMLineIndex"] as Long).toInt(),
                        cData["candidate"] as String
                    )

                    //remoteDescription
                    if (remoteDescriptSet){
                        peerConnector?.addIceCandidate(candidate)
                    }
                    else {
                        remoteCandidates.add(candidate)
                    }
                }
                sendSignallingMessage(mapOf("iceAnswer" to null))
            }
        }
        else if(!isOfferer && data["iceOffer"] != null) {
            executor.execute {
                val cMDDataList = data["iceOffer"] as List<*>
                cMDDataList.forEach { map ->
                    val cData = map as HashMap<*, *>
                    val candidate = IceCandidate(
                        cData["sdpMid"] as String,
                        (cData["sdpMLineIndex"] as Long).toInt(),
                        cData["candidate"] as String
                    )

                    //remoteDescription
                    if (remoteDescriptSet) {
                        peerConnector?.addIceCandidate(candidate)
                    } else {
                        remoteCandidates.add(candidate)
                    }
                }
                sendSignallingMessage(mapOf("iceOffer" to null))
            }
        }
        if(isOfferer.not() && data["sdpOffer"] != null){
            executor.execute {
                val offerSdp = data["sdpOffer"] as String
                val offer = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
                sendSignallingMessage(mapOf("sdpOffer" to null))
                peerConnector?.setRemoteDescription(sdpObserver, offer)
                createAnswer()
            }
        }
        if(isOfferer && data["sdpAnswer"] != null){
            executor.execute {
                val answerSdp = data["sdpAnswer"] as String
                val answer = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
                sendSignallingMessage(mapOf("sdpAnswer" to null))
                peerConnector?.setRemoteDescription(sdpObserver, answer)
            }
        }
    }
    fun setupFirebaseListeners(){
        Timber.d("setupFirebaseListeners")
        val signallingRef = firestore.collection("rooms").document(roomId)
        signallingRef.addSnapshotListener { value, error ->
            if (error != null){
                error.printStackTrace()
                return@addSnapshotListener
            }
            value?.data?.let {
                handleSignallingMessages(it)
            }
        }
    }

    fun checkRoomCapacityAndSetup(
        onProceed : () -> Unit,
    ){
        val roomDocRef = firestore.collection("rooms").document(roomId)
        roomDocRef.get().addOnSuccessListener { document ->
            Timber.d("Firebase firestore success")

            if(document != null && document.exists()){
                val participantCount = (document["participantCount"] as? Long)?.toInt() ?: 0
                if(participantCount >=2 ) {
                    Toast.makeText(context, "Room is FULL. Cannot join at the moment",
                        Toast.LENGTH_SHORT).show()
                    onNavigateBack.invoke()
                }else {
                    roomDocRef.update("participantCount", participantCount+1)
                    isOfferer = false
                    onProceed.invoke()
                }
            } else {
                roomDocRef.set(mapOf("participantCount" to 1))
                isOfferer = true
                onProceed.invoke()
            }

        }.addOnFailureListener {
            Timber.e("Firebase Failed to get Firestore DB.")
            onNavigateBack.invoke()
        }
    }
    fun initializeWebRTC() {
        Timber.d("initializeWebRTC() :: ")
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        val videoEncoderFactory = DefaultVideoEncoderFactory(
            elgBase.eglBaseContext, true, false
        )
        val videoDecoderFactory = DefaultVideoDecoderFactory(elgBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory
            .builder()
            .setVideoEncoderFactory(videoEncoderFactory)
            .setVideoDecoderFactory(videoDecoderFactory)
            .createPeerConnectionFactory()
    }
    fun createPeerConnection(){
        Timber.d("createPeerConnection")

        val iceServers = PeerConnection.IceServer.builder(listOf(
            "stun:stun1.google.com:19302",
            "stun:stun2.google.com:19302",
        ))

        val rtcCong = PeerConnection.RTCConfiguration(listOf(iceServers.createIceServer()))
        peerConnector =  peerConnectionFactory?.createPeerConnection(
            rtcCong, object : PeerConnection.Observer {
                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    super.onConnectionChange(newState)
                    Timber.d("createPeerConnection() :: onConnectionChange $newState")
                }

                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                }

                override fun onIceConnectionReceivingChange(p0: Boolean) {
                }

                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                }

                override fun onIceCandidate(candidate: IceCandidate?) {

                    candidate?.let {
                        val key = if(isOfferer) "iceOffer" else "iceAnswer"
                        localCandidatesToShare.add(
                            mapOf(
                                "candidate" to it.sdp,
                                "sdpMid" to it.sdpMid,
                                "sdpMLineIndex" to it.sdpMLineIndex
                            )
                        )
                        //send to signaling server
                        sendSignallingMessage(
                            mapOf(key to localCandidatesToShare)
                        )
                    }
                }

                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate?>?) {
                }

                override fun onAddStream(streams: MediaStream?) {

                }

                override fun onRemoveStream(p0: MediaStream?) {
                }

                override fun onDataChannel(p0: DataChannel?) {
                }

                override fun onRenegotiationNeeded() {
                }

                override fun onTrack(rtcTrackEvent: RtpTransceiver?) {
                    val mediaStreamTrack = rtcTrackEvent?.receiver?.track()
                    if (mediaStreamTrack is VideoTrack) {
                        remoteRenderer?.let { renderer ->
                            mediaStreamTrack.addSink(renderer)
                        }
                    }
                }

                override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream?>?) {
                }

            }
        )
    }

    fun createCameraCapturer() : CameraVideoCapturer? {
        val cameraEnumator = Camera2Enumerator(context)
        val devicesNames = cameraEnumator.deviceNames
        for(deviceName in devicesNames){
            if (cameraEnumator.isFrontFacing(deviceName)){
                val videoCapturer = cameraEnumator.createCapturer(deviceName, null)
                if (videoCapturer != null){
                    return videoCapturer
                }
            }
        }
        for(deviceName in devicesNames){
            if (!cameraEnumator.isFrontFacing(deviceName)){
                val videoCapturer = cameraEnumator.createCapturer(deviceName, null)
                if (videoCapturer != null){
                    return videoCapturer
                }
            }
        }
        return null
    }
    //local rendering
    fun initilializeLocalMediaStream(){
        Timber.d("initializeLocalMediaStream")
        val videoSource = peerConnectionFactory?.createVideoSource(false)
        videoSource?: run {
            Timber.e("Video Source are null!")
        }
        val videoCapturer = createCameraCapturer()
        videoCapturer?.let { vc ->
            vc.initialize(
                SurfaceTextureHelper.create("CaptureThread", elgBase.eglBaseContext),
                context,
                videoSource?.capturerObserver
            )
            vc.startCapture(1280, 720, 30)
        }
        val videoTrack = peerConnectionFactory?.createVideoTrack("1001", videoSource)
        peerConnector?.addTrack(videoTrack)
        val mediaStream = peerConnectionFactory?.createLocalMediaStream("mediaStream")
        mediaStream?.addTrack(videoTrack)
        videoTrack?.addSink(localRenderer)
    }

    LaunchedEffect(Unit) {
        checkRoomCapacityAndSetup(
            onProceed = {
                executor.execute {
                    initializeWebRTC()
                    createPeerConnection()
                    initilializeLocalMediaStream()
                    if (isOfferer){
                        createOffer()
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    init (elgBase.eglBaseContext, null)
                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                    localRenderer = this
                }
            },
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    init (elgBase.eglBaseContext, null)
                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                    remoteRenderer = this
                }
            },
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        )
    }
}

