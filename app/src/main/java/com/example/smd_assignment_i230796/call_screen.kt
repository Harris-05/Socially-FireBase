package com.example.smd_assignment_i230796

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration

class call_screen : AppCompatActivity() {

    private var mRtcEngine: RtcEngine? = null
    private val appId = "ec3c2b0033e3453684efc4e9919a4cc9"
    private var channelName: String? = null
    private var callType: String? = null // "audio" or "video"

    private var isMuted = false
    private var isSpeakerOn = false

    private val PERMISSION_REQ_ID = 22
    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private lateinit var btnMute: ImageView
    private lateinit var btnSpeaker: ImageView
    private lateinit var btnEnd: ImageView

    private lateinit var audioManager: AudioManager

    // ----------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.call_screen)

        channelName = intent.getStringExtra("channelName") ?: "testChannel"
        callType = intent.getStringExtra("callType") ?: "video"

        btnMute = findViewById(R.id.btn_mic_toggle)
        btnSpeaker = findViewById(R.id.btn_speaker_toggle)
        btnEnd = findViewById(R.id.btn_end_call)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        btnEnd.setOnClickListener { endCall() }

        // 🎙️ Toggle Mute
        btnMute.setOnClickListener {
            isMuted = !isMuted
            mRtcEngine?.muteLocalAudioStream(isMuted)
            val icon = if (isMuted) R.drawable.mute_microphone else R.drawable.mic
            btnMute.setImageResource(icon)
            Toast.makeText(this, if (isMuted) "Microphone muted" else "Microphone unmuted", Toast.LENGTH_SHORT).show()
        }

        // 🔊 Toggle Speaker / Earpiece
        btnSpeaker.setOnClickListener {
            isSpeakerOn = !isSpeakerOn

            if (isSpeakerOn) {
                // Route audio to loudspeaker
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = true
                mRtcEngine?.setEnableSpeakerphone(true)
                btnSpeaker.setImageResource(R.drawable.speaker_icon)
                Toast.makeText(this, "Speaker enabled", Toast.LENGTH_SHORT).show()
            } else {
                // Route audio to earpiece
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false
                mRtcEngine?.setEnableSpeakerphone(false)
                btnSpeaker.setImageResource(R.drawable.earpiece)
                Toast.makeText(this, "Earpiece enabled", Toast.LENGTH_SHORT).show()
            }
        }

        if (permissions.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQ_ID)
        } else {
            setupAgora()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID &&
            grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            setupAgora()
        } else {
            Toast.makeText(this, "Camera & mic permissions required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // ----------------------------------------------------------
    private fun setupAgora() {
        try {
            mRtcEngine = RtcEngine.create(baseContext, appId, mRtcHandler)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Agora init failed: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        mRtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        mRtcEngine?.enableAudio()
        mRtcEngine?.setDefaultAudioRoutetoSpeakerphone(true)
        mRtcEngine?.setEnableSpeakerphone(true)

        if (callType == "video") {
            initVideoConfig()
            setupLocalVideo()
        } else {
            mRtcEngine?.disableVideo()
        }

        mRtcEngine?.joinChannel(null, channelName, "Extra", 0)
    }

    private fun initVideoConfig() {
        mRtcEngine?.enableVideo()
        mRtcEngine?.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
            )
        )
        mRtcEngine?.startPreview()
    }

    // ----------------------------------------------------------
    private val mRtcHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(this@call_screen, "Joined channel: $channel", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(this@call_screen, "Remote user joined ($uid)", Toast.LENGTH_SHORT).show()
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Toast.makeText(this@call_screen, "User left the call", Toast.LENGTH_SHORT).show()
                endCall()
            }
        }

        override fun onError(err: Int) {
            runOnUiThread {
                Toast.makeText(this@call_screen, "Agora error: $err", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ----------------------------------------------------------
    private fun setupLocalVideo() {
        val container = findViewById<FrameLayout>(R.id.local_video_view_container)
        container.removeAllViews()

        val surfaceView = SurfaceView(this)
        surfaceView.setZOrderMediaOverlay(true)
        container.addView(surfaceView)

        mRtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        mRtcEngine?.muteLocalVideoStream(false)
        mRtcEngine?.muteLocalAudioStream(false)
        mRtcEngine?.startPreview()
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = findViewById<FrameLayout>(R.id.remote_video_view_container)
        container.removeAllViews()

        val surfaceView = SurfaceView(this)
        surfaceView.setZOrderMediaOverlay(false)
        container.addView(surfaceView)

        mRtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }

    // ----------------------------------------------------------
    private fun endCall() {
        try {
            mRtcEngine?.leaveChannel()
            RtcEngine.destroy()
            mRtcEngine = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        endCall()
    }
}
