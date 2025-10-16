package com.example.smd_assignment_i230796

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class outgoing_call : AppCompatActivity() {


    private lateinit var tvCalleeName: TextView
    private lateinit var btnCancelCall: ImageView
    private lateinit var imgCallee: de.hdodenhof.circleimageview.CircleImageView
    private var receiverId: String? = null
    private var receiverName: String? = null
    private var receiverProfileBase64: String? = null
    private var callId: String? = null
    private var callType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outgoing_call)

        imgCallee = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.imgCallee)
        tvCalleeName = findViewById(R.id.tvCalleeName)
        btnCancelCall = findViewById(R.id.btnCancelCall)

        // Receive intent data
        receiverId = intent.getStringExtra("receiverId")
        receiverName = intent.getStringExtra("receiverName")
        receiverProfileBase64 = intent.getStringExtra("receiverProfileBase64")
        callId = intent.getStringExtra("callId")
        callType = intent.getStringExtra("callType")

        // Set name and profile dynamically
        tvCalleeName.text = "Calling ${receiverName ?: "Unknown"}..."
        setProfilePicture(receiverProfileBase64)

        // Cancel Call button
        btnCancelCall.setOnClickListener {
            endCall()
        }

        // Optional: Show waiting state message
        Toast.makeText(this, "Connecting call...", Toast.LENGTH_SHORT).show()
    }

    private fun endCall() {
        if (callId != null) {
            val callRef = FirebaseDatabase.getInstance().getReference("calls").child(callId!!)
            callRef.child("status").setValue("cancelled")
                .addOnSuccessListener {
                    Toast.makeText(this, "Call cancelled", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to cancel call", Toast.LENGTH_SHORT).show()
                }
        } else {
            finish()
        }
    }

    private fun setProfilePicture(base64String: String?) {
        if (!base64String.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imgCallee.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imgCallee.setImageResource(R.drawable.jack_profile)
            }
        } else {
            imgCallee.setImageResource(R.drawable.jack_profile)
        }
    }
}
