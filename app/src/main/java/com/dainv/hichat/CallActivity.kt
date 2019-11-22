package com.dainv.hichat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.dainv.hichat.const.SocketConstant
import com.dainv.hichat.socket.SocketConnector
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import dagger.android.AndroidInjection

import kotlinx.android.synthetic.main.content_call.*
import org.webrtc.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.collections.ArrayList

class CallActivity : AppCompatActivity() {

    companion object {
        const val LOCAL_AUDIO_TRACK_ID = "dainv_audio"
        const val LOCAL_VIDEO_TRACK_ID = "dainv_video"
    }

    @Inject
    lateinit var socketConnector: SocketConnector

    @Inject
    lateinit var gson: Gson

    private lateinit var room: String
    private var isInit: Boolean = false
    private lateinit var pCObserver: PCObserver
    private var peerConnection: PeerConnection? = null
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var elgBase: EglBase
    private val executor = Executors.newSingleThreadExecutor()
    private var videoCapturer: VideoCapturer? = null
    private var audioConstraints: MediaConstraints? = null
    private var sdpMediaConstraints: MediaConstraints? = null
    private var queueICECandidate: Queue<IceCandidate>? = null
    private var iceServes: ArrayList<PeerConnection.IceServer>? = null
    private val sdpObserver = SDPObserver()
    private lateinit var audioSource: AudioSource
    private lateinit var localAudioTrack: AudioTrack
    private lateinit var videoSource: VideoSource
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var sufaceTextureHelper: SurfaceTextureHelper


    //sdp
    private var localSDP: SessionDescription? = null
    private var remoteSDP: SessionDescription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        getParamIntent()
        // check permission
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
                ), 100
            )
            return
        }
        startConfig()
        pCObserver = PCObserver(socketConnector)

        // connect to room
        connectToRoom()
    }

    private fun connectToRoom() {
        if (room == null) {
            return
        }
        socketConnector.connectToRoom(room) {
            getDataFromString(it)
            val jsonObject = JsonParser().parse(it).asJsonObject
            if (jsonObject.get("isFull").asBoolean) {
                Toast.makeText(applicationContext, "Room full slot", Toast.LENGTH_LONG).show()
                return@connectToRoom
            }
            connectedToRoom(it)
        }
    }

    private fun connectedToRoom(data: String) {
        videoCapturer = createCameraCapturer()
        executor.execute {
            val jsonObject = JsonParser().parse(data).asJsonObject
            sdpMediaConstraints = MediaConstraints()
            sdpMediaConstraints?.mandatory?.add(
                MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")
            )
            sdpMediaConstraints?.mandatory?.add(
                MediaConstraints.KeyValuePair("OfferToReceiveVideo", true.toString())
            )
            queueICECandidate = ArrayDeque<IceCandidate>()
            val rtcConfig = PeerConnection.RTCConfiguration(iceServes)
            rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            rtcConfig.continualGatheringPolicy =
                PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            rtcConfig.keyType = PeerConnection.KeyType.ECDSA
            // Enable DTLS for normal calls and disable for loopback calls.
            rtcConfig.enableDtlsSrtp = false
            rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, pCObserver)!!
            peerConnection?.addTrack(createAudioTrack())
            peerConnection?.addTrack(createVideoTrack())
            sdpMediaConstraints = MediaConstraints()
            sdpMediaConstraints!!.mandatory.add(
                MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")
            )
            sdpMediaConstraints!!.mandatory.add(
                MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")
            )
            if (isInit) {
                peerConnection?.createOffer(sdpObserver, sdpMediaConstraints)
            } else {
                if (jsonObject["dataRooms"] != null) {
                    val dataRooms = jsonObject["dataRooms"].asJsonObject
                    if (dataRooms["sdp"] != null) {
                        val remoteSDP = dataRooms["sdp"].asString
                        peerConnection?.setRemoteDescription(
                            sdpObserver,
                            SessionDescription(SessionDescription.Type.OFFER, remoteSDP)
                        )
                        peerConnection?.createAnswer(sdpObserver, sdpMediaConstraints)
                    }
                    if (dataRooms["iceCandidates"] != null) {
                        val iceCandidates = Gson().fromJson<List<IceCandidate>>(
                            dataRooms["iceCandidates"].asString,
                            object : TypeToken<List<IceCandidate>>() {}.type
                        )
                        iceCandidates.forEach {
                            addIceCandidate(it)
                        }
                    }
                }
            }
        }
        listenEventInRoom()
    }

    private fun addIceCandidate(iceCandidate: IceCandidate) {
        executor.execute {
            peerConnection?.let {
                if (queueICECandidate == null) {
                    peerConnection?.addIceCandidate(iceCandidate)
                } else {
                    queueICECandidate?.add(iceCandidate)
                }
            }
        }
    }


    private fun listenEventInRoom() {
        socketConnector.socket?.on(SocketConstant.SDP_EVENT) {
            val data = it[0].toString()
            val sdp = gson.fromJson<SessionDescription>(data, SessionDescription::class.java)
            if (isInit) {
                val sdpAnswer = SessionDescription(SessionDescription.Type.ANSWER, sdp.description)
                peerConnection?.setRemoteDescription(sdpObserver, sdpAnswer)

            }
        }
        socketConnector.socket?.on(SocketConstant.ICE_CANDIDATE_EVENT) {
            val data = it[0].toString()
            val iceCandidate = gson.fromJson<IceCandidate>(data, IceCandidate::class.java)
            addIceCandidate(iceCandidate)
        }
    }

    private fun getParamIntent() {
        room = intent.getStringExtra("room")
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val camera2Enumerator = Camera2Enumerator(applicationContext)
        val deviceNames = camera2Enumerator.deviceNames
        for (deviceName in deviceNames) {
            if (camera2Enumerator.isFrontFacing(deviceName)) {
                val videoCapturer = camera2Enumerator.createCapturer(deviceName, null)

                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    private fun startConfig() {
        elgBase = EglBase.create()
        videoLocal.init(elgBase.eglBaseContext, null)
        videoLocal.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        videoRemote.init(elgBase.eglBaseContext, null)
        videoRemote.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        videoLocal.setZOrderMediaOverlay(true)
        videoLocal.setEnableHardwareScaler(true /* enabled */)
        videoRemote.setEnableHardwareScaler(false /* enabled */)
        createPeerConnectionFactory()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 100) {
            for (grant in grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }
            startConfig()
        }
    }

    private fun createPeerConnectionFactory() {
        executor.execute {
            val option = PeerConnectionFactory.Options()
            val encoderFactory = DefaultVideoEncoderFactory(
                elgBase.eglBaseContext, true /* enableIntelVp8Encoder */, true
            )
            val decoderFactory = DefaultVideoDecoderFactory(elgBase.eglBaseContext)
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(applicationContext)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
            )

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(option)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory()
        }
    }

    fun createAudioTrack(): AudioTrack {
        val mediaConstraints = MediaConstraints()
        audioSource = peerConnectionFactory.createAudioSource(mediaConstraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack(LOCAL_AUDIO_TRACK_ID, audioSource)
        return localAudioTrack
    }

    fun createVideoTrack(): VideoTrack {
        sufaceTextureHelper = SurfaceTextureHelper.create("Capture video", elgBase.eglBaseContext)
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer!!.isScreencast)
        videoCapturer!!.initialize(
            sufaceTextureHelper,
            applicationContext,
            videoSource.capturerObserver
        )
        videoCapturer!!.startCapture(1280, 720, 60)
        localVideoTrack = peerConnectionFactory.createVideoTrack(LOCAL_VIDEO_TRACK_ID, videoSource)
        localVideoTrack.setEnabled(true)
        localVideoTrack.addSink(videoLocal)
        return localVideoTrack
    }

    private fun getDataFromString(data: String) {
        val jsonObject = JsonParser().parse(data).asJsonObject
        isInit = jsonObject.get("isInit").asBoolean
        iceServes = gson.fromJson(
            jsonObject.get("iceServers"),
            object : TypeToken<ArrayList<PeerConnection.IceServer>>() {}.type
        )
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    inner class PCObserver : PeerConnection.Observer {
        private var socketConnector: SocketConnector

        constructor(socketConnector: SocketConnector) {
            this.socketConnector = socketConnector
        }

        override fun onRenegotiationNeeded() {
            Timber.e("PCObserver : onRenegotiationNeeded()")
        }

        override fun onIceCandidate(p0: IceCandidate?) {
            Timber.e("PCObserver : onIceCandidate()")
            socketConnector.sendDataOnRoom(SocketConstant.ICE_CANDIDATE_EVENT, gson.toJson(p0))
        }

        override fun onDataChannel(p0: DataChannel?) {
        }

        override fun onIceConnectionReceivingChange(p0: Boolean) {
            Timber.e("PCObserver : onIceConnectionReceivingChange()")

        }

        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
            Timber.e("PCObserver : onIceConnectionChange()")

        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
            Timber.e("PCObserver : onIceConnectionChange()")

        }

        override fun onAddStream(p0: MediaStream?) {
            Timber.e("PCObserver : MediaStream()")
        }

        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
            Timber.e("PCObserver : onSignalingChange  $p0")
        }

        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
            Timber.e("PCObserver : onIceCandidatesRemoved()")

        }

        override fun onRemoveStream(p0: MediaStream?) {
            Timber.e("PCObserver : onRemoveStream()")
        }

        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
            Timber.e("PCObserver : onAddTrack()")
        }

    }

    inner class SDPObserver : SdpObserver {
        override fun onSetFailure(p0: String?) {
            Timber.e("SDPObserver onSetFailure: $p0")
        }

        override fun onSetSuccess() {
            Timber.e("SDPObserver : onSetSuccess")
            if (isInit) { // caller
                if (remoteSDP == null) { // recently set local sdp
                    socketConnector.sendDataOnRoom(SocketConstant.SDP_EVENT, gson.toJson(localSDP))
                } else {// recently set remote local sdp
                    drainRemoteIceCandidate()
                }
            } else { // callee
                if (peerConnection?.localDescription != null) {
                    socketConnector.sendDataOnRoom(SocketConstant.SDP_EVENT, gson.toJson(localSDP))
                    drainRemoteIceCandidate()
                }
            }
        }

        override fun onCreateSuccess(p0: SessionDescription?) {
            Timber.e("SDPObserver : onCreateSuccess")
            if (localSDP == null) {
                localSDP = p0
                executor.execute {
                    peerConnection?.setLocalDescription(sdpObserver, localSDP)
                }
            }
        }

        override fun onCreateFailure(p0: String?) {
            Timber.e("SDPObserver : onCreateFailure" + p0)
        }

    }

    private fun drainRemoteIceCandidate() {
        queueICECandidate?.also {
            for (ice in it) {
                peerConnection?.addIceCandidate(ice)
            }
            queueICECandidate = null
        }
    }
}
