package com.example.smd_assignment_i230796

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class incoming_call : AppCompatActivity() {

    private lateinit var tvCallerName: TextView
    private lateinit var btnAccept: ImageView
    private lateinit var btnReject: ImageView
    private lateinit var imgCallerProfile: de.hdodenhof.circleimageview.CircleImageView

    private var callerId: String? = null
    private var callerName: String? = null
    private var callerProfileBase64: String? = null
    private var callType: String? = null
    private var callId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        imgCallerProfile = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.imgCaller)
        tvCallerName = findViewById(R.id.tvCallerName)
        btnAccept = findViewById<ImageView>(R.id.btnAccept)
        btnReject = findViewById<ImageView>(R.id.btnReject)

        // Receive data
        callerId = intent.getStringExtra("callerId")
        callerName = intent.getStringExtra("callerName")
        callerProfileBase64 = intent.getStringExtra("callerProfileBase64")
        callType = intent.getStringExtra("callType")
        callId = intent.getStringExtra("callId")

        tvCallerName.text = callerName ?: "Unknown Caller"
        setProfilePicture(callerProfileBase64)

        // Accept button → Go to ongoing call
        btnAccept.setOnClickListener {
            val intent = Intent(this, call_screen::class.java)
            intent.putExtra("callId", callId)
            intent.putExtra("callType", callType)
            intent.putExtra("receiverId", callerId)
            startActivity(intent)
            finish()
        }

        // Reject button → End the call or update Firebase
        btnReject.setOnClickListener {
            // Example: update Firebase call status
            if (callId != null) {
                val callRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("calls").child(callId!!)
                callRef.child("status").setValue("rejected")
            }
            finish()
        }
    }

    private fun setProfilePicture(base64String: String?) {
        if (!base64String.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imgCallerProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imgCallerProfile.setImageResource(R.drawable.jack_profile)
            }
        } else {
            imgCallerProfile.setImageResource(R.drawable.jack_profile)
        }
    }
}
