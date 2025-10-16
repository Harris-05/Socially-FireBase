package com.example.smd_assignment_i230796

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.database.*

class IncomingCallService : Service() {

    private lateinit var callsRef: DatabaseReference
    private var userId: String? = null
    private var listener: ValueEventListener? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userId = intent?.getStringExtra("userId")

        if (userId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d("IncomingCallService", "Service started for user: $userId")
        listenForIncomingCalls()
        return START_STICKY
    }

    private fun listenForIncomingCalls() {
        callsRef = FirebaseDatabase.getInstance().getReference("calls")

        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (callSnapshot in snapshot.children) {
                    val call = callSnapshot.value as? Map<*, *> ?: continue
                    val receiverId = call["receiverId"] as? String ?: ""
                    val status = call["status"] as? String ?: ""

                    if (receiverId == userId && status == "ringing") {
                        val callerId = call["callerId"] as? String ?: ""
                        val callType = call["type"] as? String ?: "audio"
                        val callId = callSnapshot.key ?: ""
                        val channelName = call["channelName"] as? String ?: ""

                        Log.d("IncomingCallService", "Incoming $callType call from $callerId")

                        // ✅ Fetch caller details from Users node
                        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(callerId)
                        userRef.get().addOnSuccessListener { userSnapshot ->
                            val callerName = userSnapshot.child("username").getValue(String::class.java) ?: "Unknown"
                            val callerPic = userSnapshot.child("profileImage").getValue(String::class.java) ?: ""

                            // ✅ Launch Incoming Call Activity
                            val intent = Intent(this@IncomingCallService, incoming_call::class.java)
                            intent.putExtra("callerId", callerId)
                            intent.putExtra("callerName", callerName)
                            intent.putExtra("callerProfileBase64", callerPic)
                            intent.putExtra("callId", callId)
                            intent.putExtra("callType", callType)
                            intent.putExtra("channelName", channelName)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }.addOnFailureListener {
                            Log.e("IncomingCallService", "Failed to fetch caller details: ${it.message}")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("IncomingCallService", "Firebase error: ${error.message}")
            }
        }

        callsRef.addValueEventListener(listener!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.let { callsRef.removeEventListener(it) }
        Log.d("IncomingCallService", "Service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
