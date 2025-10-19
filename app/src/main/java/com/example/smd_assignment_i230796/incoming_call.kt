package com.example.smd_assignment_i230796

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView

class incoming_call : AppCompatActivity() {

    private lateinit var tvCallerName: TextView
    private lateinit var btnAccept: ImageView
    private lateinit var btnReject: ImageView
    private lateinit var imgCallerProfile: CircleImageView

    private var callerId: String? = null
    private var callerName: String? = null
    private var callerProfileBase64: String? = null
    private var callType: String? = null
    private var callId: String? = null
    private var channelName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        // Bind UI
        imgCallerProfile = findViewById(R.id.imgCaller)
        tvCallerName = findViewById(R.id.tvCallerName)
        btnAccept = findViewById(R.id.btnAccept)
        btnReject = findViewById(R.id.btnReject)

        // Get call info
        callerId = intent.getStringExtra("callerId")
        callerName = intent.getStringExtra("callerName")
        callerProfileBase64 = intent.getStringExtra("callerProfileBase64")
        callType = intent.getStringExtra("callType") ?: "audio"
        callId = intent.getStringExtra("callId")
        channelName = intent.getStringExtra("channelName")

        // Set caller UI
        tvCallerName.text = callerName ?: "Unknown Caller"
        setProfilePicture(callerProfileBase64)

        // Accept button
        btnAccept.setOnClickListener {
            if (callId != null) {
                val callRef = FirebaseDatabase.getInstance().getReference("calls").child(callId!!)
                callRef.child("status").setValue("accepted")
                    .addOnSuccessListener {
                        // Launch Agora call screen
                        val intent = Intent(this, call_screen::class.java)
                        intent.putExtra("callId", callId)
                        intent.putExtra("callType", callType)
                        intent.putExtra("callerId", callerId)
                        intent.putExtra("channelName", channelName)
                        startActivity(intent)
                        this.finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to accept call", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Invalid call data", Toast.LENGTH_SHORT).show()
            }
        }

        // Reject button
        btnReject.setOnClickListener {
            if (callId != null) {
                val callRef = FirebaseDatabase.getInstance().getReference("calls").child(callId!!)
                callRef.child("status").setValue("rejected")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Call rejected", Toast.LENGTH_SHORT).show()
                        this.finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to reject call", Toast.LENGTH_SHORT).show()
                    }
            } else {
                finish()
            }
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
