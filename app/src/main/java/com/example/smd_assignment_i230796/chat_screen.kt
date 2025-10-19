package com.example.smd_assignment_i230796

import android.app.AlertDialog
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream

class chat_screen : BaseActivity() {

    private lateinit var recyclerMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnCamera: ImageView
    private lateinit var imgProfile: ImageView
    private lateinit var tvChatName: TextView
    private lateinit var btncall: ImageView
    private lateinit var messagesRef: DatabaseReference
    private lateinit var messageList: MutableList<ChatMessage>
    private lateinit var adapter: MessageAdapter

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val IMAGE_PICK = 101

    private var chatId: String? = null
    private var receiverId: String? = null
    private var receiverName: String? = null
    private var receiverProfileBase64: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.chat_screen)

        recyclerMessages = findViewById(R.id.recyclerMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnCamera = findViewById(R.id.btnCamera)
        imgProfile = findViewById(R.id.imgProfile)
        tvChatName = findViewById(R.id.tvChatName)
        btncall = findViewById(R.id.btnVideo)

        chatId = intent.getStringExtra("chatId")
        receiverId = intent.getStringExtra("receiverId")
        receiverName = intent.getStringExtra("receiverName")
        receiverProfileBase64 = intent.getStringExtra("receiverProfileBase64")

        if (chatId.isNullOrEmpty() || receiverId.isNullOrEmpty()) {
            Toast.makeText(this, "Chat data missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvChatName.text = receiverName ?: "Unknown"
        setReceiverProfile(receiverProfileBase64)

        messageList = mutableListOf()
        adapter = MessageAdapter(messageList, currentUserId, receiverProfileBase64) { message ->
            if (message.senderId == currentUserId) showEditDeleteDialog(message)
        }

        recyclerMessages.layoutManager = LinearLayoutManager(this)
        recyclerMessages.adapter = adapter

        btnSend.setOnClickListener { sendMessage() }
        btnCamera.setOnClickListener { pickImage() }

        if (!chatId.isNullOrEmpty()) {
            initMessagesRef(chatId!!)
        } else if (!receiverId.isNullOrEmpty()) {
            fetchOrCreateChatForReceiver()
        }

        // 🔹 Call button logic
        btncall.setOnClickListener {
            val options = arrayOf("Audio Call", "Video Call")
            AlertDialog.Builder(this)
                .setTitle("Choose Call Type")
                .setItems(options) { _, which ->
                    val callType = if (which == 0) "audio" else "video"
                    initiateCall(callType)
                }.show()
        }

        startScreenshotDetection()

    }

    // ---------------- SCREENSHOT DETECTION ----------------
    private var lastScreenshotTime = 0L
    private var lastScreenshotSentAt = 0L
    private var lastScreenshotFileName: String? = null
    private var lastScreenshotMessageId: String? = null
    private var screenshotObserver: ContentObserver? = null
    private var observerRegistered = false

    private fun startScreenshotDetection() {
        //Prevent multiple observers if activity is recreated
        if (observerRegistered) {
            Log.d("Screenshot", "⚠️ Screenshot observer already registered — skipping.")
            return
        }

        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missingPermissions = permissions.filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissions(missingPermissions.toTypedArray(), 1001)
            return
        }

        screenshotObserver = object : ContentObserver(Handler(mainLooper)) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                detectScreenshot()
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            screenshotObserver!!
        )
        observerRegistered = true
        Log.d("Screenshot", "📸 Screenshot observer registered.")
    }

    private fun stopScreenshotDetection() {
        if (observerRegistered && screenshotObserver != null) {
            contentResolver.unregisterContentObserver(screenshotObserver!!)
            observerRegistered = false
            Log.d("Screenshot", "🧹 Screenshot observer unregistered.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScreenshotDetection()
    }

    private fun detectScreenshot() {
        val projection = arrayOf(
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Images.Media.DATE_ADDED} > ?",
            arrayOf((lastScreenshotTime / 1000).toString()),
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val dateAdded = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)) * 1000
                val displayName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)) ?: ""

                if (dateAdded > lastScreenshotTime &&
                    (displayName.contains("screenshot", true) || path.contains("Screenshots", true))
                ) {
                    lastScreenshotTime = dateAdded
                    notifyScreenshotTaken(displayName)
                    break
                }
            }
        }
    }

    private fun notifyScreenshotTaken(fileName: String?) {
        val now = System.currentTimeMillis()

        //Ignore duplicate screenshot events (same file or too soon)
        if (now - lastScreenshotSentAt < 4000 && fileName == lastScreenshotFileName) {
            Log.d("Screenshot", "⚠️ Duplicate screenshot ignored (same file or cooldown).")
            return
        }

        lastScreenshotSentAt = now
        lastScreenshotFileName = fileName

        if (chatId.isNullOrEmpty() || receiverId.isNullOrEmpty()) return

        val chatsRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId!!)
        val messageIdToUse = lastScreenshotMessageId ?: chatsRef.child("messages").push().key ?: return
        lastScreenshotMessageId = messageIdToUse

        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)
        userRef.child("username").get().addOnSuccessListener { snapshot ->
            val senderUsername = snapshot.value?.toString() ?: "Someone"

            val screenshotMessage = ChatMessage(
                messageId = messageIdToUse,
                senderId = currentUserId,
                text = "[ $senderUsername took a Screenshot ]",
                timestamp = now,
                imageUrl = null,
                edited = false
            )

            val updates = hashMapOf<String, Any>(
                "/messages/$messageIdToUse" to screenshotMessage,
                "/lastMessage" to "[Screenshot]"
            )
            chatsRef.updateChildren(updates).addOnSuccessListener {
                Log.d("Screenshot", "✅ Screenshot message sent once.")
            }

            // Allow new screenshot message after 5 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                lastScreenshotMessageId = null
            }, 5000)
        }
    }

    private fun setReceiverProfile(profileBase64: String?) {
        if (!profileBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(profileBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imgProfile.setImageBitmap(getCircularBitmap(bitmap))
            } catch (e: Exception) {
                imgProfile.setImageResource(R.drawable.jack_profile)
            }
        } else {
            imgProfile.setImageResource(R.drawable.jack_profile)
        }
    }






    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint().apply { isAntiAlias = true }
        val rect = android.graphics.Rect(0, 0, size, size)
        val rectF = android.graphics.RectF(rect)
        canvas.drawOval(rectF, paint)
        paint.xfermode =
            android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        canvas.drawBitmap(bitmap, -left.toFloat(), -top.toFloat(), paint)
        return output
    }

    private fun fetchOrCreateChatForReceiver() {
        if (receiverId.isNullOrEmpty()) return

        val chatsRef = FirebaseDatabase.getInstance().getReference("chats")
        val canonicalId =
            if (currentUserId < receiverId!!) "${currentUserId}_${receiverId}" else "{$receiverId}_${currentUserId}"

        chatsRef.orderByChild("chatId").equalTo(canonicalId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        chatId = snapshot.children.first().key
                        initMessagesRef(chatId!!)
                    } else {
                        chatId = chatsRef.push().key
                        val chatData = mapOf(
                            "chatId" to canonicalId,
                            "userId1" to currentUserId,
                            "userId2" to receiverId,
                            "lastMessage" to "",
                            "timestamp" to System.currentTimeMillis()
                        )
                        if (chatId != null) {
                            chatsRef.child(chatId!!).setValue(chatData)
                            initMessagesRef(chatId!!)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun initMessagesRef(chatKey: String) {
        messagesRef = FirebaseDatabase.getInstance()
            .getReference("chats")
            .child(chatKey)
            .child("messages")
        loadMessages()
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty() || !::messagesRef.isInitialized) return

        val messageId = messagesRef.push().key ?: return
        val timestamp = System.currentTimeMillis()

        val message = ChatMessage(messageId, currentUserId, text, timestamp, null, false)
        messagesRef.child(messageId).setValue(message)
        etMessage.text.clear()

        FirebaseDatabase.getInstance().getReference("chats").child(chatId!!)
            .child("lastMessage").setValue(text)
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            val base64Image = bitmapToBase64(bitmap)
            sendImageMessage(base64Image)
        }
    }

    private fun sendImageMessage(base64Image: String) {
        if (!::messagesRef.isInitialized) return

        val messageId = messagesRef.push().key ?: return
        val timestamp = System.currentTimeMillis()

        val message = ChatMessage(messageId, currentUserId, "", timestamp, base64Image, false)
        messagesRef.child(messageId).setValue(message)
        FirebaseDatabase.getInstance().getReference("chats").child(chatId!!)
            .child("lastMessage").setValue("[Image]")
    }

    private fun initiateCall(callType: String) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val selectedUid = receiverId ?: return

        val callId =
            if (currentUid < selectedUid) "${currentUid}_${selectedUid}" else "${selectedUid}_${currentUid}"

        val callData = mapOf(
            "callerId" to currentUid,
            "receiverId" to selectedUid,
            "type" to callType,
            "status" to "ringing",
            "channelName" to callId,
            "timestamp" to ServerValue.TIMESTAMP
        )

        FirebaseDatabase.getInstance().getReference("calls")
            .child(callId)
            .setValue(callData)
            .addOnSuccessListener {
                val intent = Intent(this, outgoing_call::class.java)
                intent.putExtra("callId", callId)
                intent.putExtra("callType", callType)
                intent.putExtra("receiverId", selectedUid)
                intent.putExtra("receiverName", receiverName)
                intent.putExtra("receiverProfileBase64", receiverProfileBase64)
                Toast.makeText(this, "Call intiated from chat_screen ", Toast.LENGTH_SHORT).show()
                startActivity(intent)

            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to initiate call.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    private fun loadMessages() {
        if (!::messagesRef.isInitialized) return

        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (child in snapshot.children) {
                    val msg = child.getValue(ChatMessage::class.java)
                    if (msg != null) messageList.add(msg)
                }
                adapter.notifyDataSetChanged()
                recyclerMessages.scrollToPosition(messageList.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showEditDeleteDialog(message: ChatMessage) {
        val timeLimit = 5 * 60 * 1000
        val canEdit = System.currentTimeMillis() - message.timestamp < timeLimit

        val options =
            if (canEdit) arrayOf("Edit", "Delete", "Cancel") else arrayOf("Delete", "Cancel")

        AlertDialog.Builder(this)
            .setItems(options) { dialog, which ->
                when (options[which]) {
                    "Edit" -> showEditDialog(message)
                    "Delete" -> messagesRef.child(message.messageId!!).removeValue()
                }
                dialog.dismiss()
            }.show()
    }

    private fun showEditDialog(message: ChatMessage) {
        val editText = EditText(this)
        editText.setText(message.text)

        AlertDialog.Builder(this)
            .setTitle("Edit Message")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    val updates = mapOf("text" to newText, "edited" to true)
                    messagesRef.child(message.messageId!!).updateChildren(updates)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}
