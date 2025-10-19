package com.example.smd_assignment_i230796

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView
import java.util.UUID

class outgoing_call : AppCompatActivity() {

    private lateinit var tvCalleeName: TextView
    private lateinit var btnCancelCall: ImageView
    private lateinit var imgCallee: CircleImageView

    private var receiverId: String? = null
    private var receiverName: String? = null
    private var receiverProfileBase64: String? = null
    private var callId: String? = null
    private var callType: String? = null

    private var channelName: String = UUID.randomUUID().toString()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outgoing_call)

        imgCallee = findViewById(R.id.imgCallee)
        tvCalleeName = findViewById(R.id.tvCalleeName)
        btnCancelCall = findViewById(R.id.btnCancelCall)

        receiverId = intent.getStringExtra("receiverId")
        receiverName = intent.getStringExtra("receiverName")
        receiverProfileBase64 = intent.getStringExtra("receiverProfileBase64")
        callType = intent.getStringExtra("callType")
        callId = UUID.randomUUID().toString()

        tvCalleeName.text = "Calling ${receiverName ?: "Unknown"}..."
        setProfilePicture(receiverProfileBase64)

        if (currentUserId != null) {
            createCallInFirebase()
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnCancelCall.setOnClickListener { endCall() }

        Toast.makeText(this, "Connecting call...", Toast.LENGTH_SHORT).show()
    }

    private fun createCallInFirebase() {
        val callRef = FirebaseDatabase.getInstance().getReference("calls").child(callId!!)

        // Fetch current user info from Users node
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId!!)
        userRef.get().addOnSuccessListener { snapshot ->
            val callerName = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
            val callerPic = snapshot.child("profileImage").getValue(String::class.java) ?: ""

            val callData = mapOf(
                "callerId" to currentUserId,
                "callerName" to callerName,
                "callerProfile" to callerPic,
                "receiverId" to receiverId,
                "receiverName" to receiverName,
                "status" to "ringing",
                "type" to callType,
                "channelName" to channelName,
                "timestamp" to System.currentTimeMillis(),
                "launched" to false
            )

            callRef.setValue(callData)
                .addOnSuccessListener {
                    listenForCallResponse()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to start call", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }.addOnFailureListener {
            Toast.makeText(this, "Could not fetch user info", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun listenForCallResponse() {
        val callRef = FirebaseDatabase.getInstance().getReference("calls").child(callId!!)
        callRef.child("status").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                when (status) {
                    "accepted" -> {
                        val intent = Intent(this@outgoing_call, call_screen::class.java).apply {
                            putExtra("callId", callId)
                            putExtra("callType", callType)
                            putExtra("receiverId", receiverId)
                            putExtra("channelName", channelName)
                        }
                        startActivity(intent)
                        finish()
                    }
                    "rejected", "cancelled" -> {
                        Toast.makeText(this@outgoing_call, "Call ended", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
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
