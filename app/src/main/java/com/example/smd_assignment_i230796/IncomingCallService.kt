package com.example.smd_assignment_i230796

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.database.*

class IncomingCallService : Service() {

    private var callsRef: DatabaseReference? = null
    private var userId: String? = null
    private var callListener: ChildEventListener? = null
    private val activeCallIds = mutableSetOf<String>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userId = intent?.getStringExtra("userId")

        if (userId.isNullOrEmpty()) {
            Log.e("IncomingCallService", "❌ No userId provided, stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d("IncomingCallService", "✅ Listening for incoming calls for user: $userId")
        listenForIncomingCalls()
        return START_STICKY
    }

    private fun listenForIncomingCalls() {
        callsRef = FirebaseDatabase.getInstance().getReference("calls")

        callListener = object : ChildEventListener {
            override fun onChildAdded(callSnapshot: DataSnapshot, previousChildName: String?) {
                val call = callSnapshot.value as? Map<*, *> ?: return
                val callId = callSnapshot.key ?: return

                // Prevent duplicate triggers
                if (activeCallIds.contains(callId)) return

                val receiverId = call["receiverId"] as? String ?: return
                val status = call["status"] as? String ?: return
                val launched = call["launched"] as? Boolean ?: false

                if (receiverId == userId && status == "ringing" && !launched) {
                    activeCallIds.add(callId)
                    callSnapshot.ref.child("launched").setValue(true)

                    val callerId = call["callerId"] as? String ?: return
                    val callType = call["type"] as? String ?: "audio"
                    val channelName = call["channelName"] as? String ?: "defaultChannel"

                    fetchCallerDetailsAndLaunch(callerId, callType, callId, channelName)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {
                snapshot.key?.let { activeCallIds.remove(it) }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("IncomingCallService", "Firebase error: ${error.message}")
            }
        }

        callsRef!!.addChildEventListener(callListener!!)
    }

    // ✅ Fixed version of caller info fetching
    private fun fetchCallerDetailsAndLaunch(
        callerId: String,
        callType: String,
        callId: String,
        channelName: String
    ) {
        val usersRef = FirebaseDatabase.getInstance().getReference("Users").child(callerId)
        usersRef.get().addOnSuccessListener { userSnapshot ->
            var callerName = userSnapshot.child("username").getValue(String::class.java)
            var callerPic = userSnapshot.child("profileImage").getValue(String::class.java)

            if (callerName.isNullOrBlank()) callerName = "Unknown"
            if (callerPic == null) callerPic = ""

            Log.d("IncomingCallService", "👤 Caller info fetched: $callerName")

            launchIncomingCallUI(
                callerId = callerId,
                callType = callType,
                callId = callId,
                channelName = channelName,
                callerName = callerName!!,
                callerPic = callerPic!!
            )
        }.addOnFailureListener { e ->
            Log.e("IncomingCallService", "❌ Failed to fetch caller info: ${e.message}")
            launchIncomingCallUI(callerId, callType, callId, channelName, "Unknown", "")
        }
    }

    private fun launchIncomingCallUI(
        callerId: String,
        callType: String,
        callId: String,
        channelName: String,
        callerName: String,
        callerPic: String
    ) {
        val intent = Intent(this, incoming_call::class.java).apply {
            putExtra("callerId", callerId)
            putExtra("callerName", callerName)
            putExtra("callerProfileBase64", callerPic)
            putExtra("callId", callId)
            putExtra("callType", callType)
            putExtra("channelName", channelName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        Log.d("IncomingCallService", "📞 Incoming $callType call from $callerName ($callerId)")
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        callListener?.let { callsRef?.removeEventListener(it) }
        activeCallIds.clear()
        Log.d("IncomingCallService", "🛑 Service stopped and listener removed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
