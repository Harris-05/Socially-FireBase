package com.example.smd_assignment_i230796

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
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
import kotlin.jvm.java

class chat_screen : AppCompatActivity() {

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
        btncall = findViewById<ImageView>(R.id.btnVideo)
        btncall.setOnClickListener {
            // Step 1: Show popup for call type
            val options = arrayOf("Audio Call", "Video Call")

            AlertDialog.Builder(this)
                .setTitle("Choose Call Type")
                .setItems(options) { _, which ->
                    val callType = if (which == 0) "audio" else "video"
                    initiateCall(callType)
                }
                .show()
        }



        chatId = intent.getStringExtra("chatId")
        receiverId = intent.getStringExtra("receiverId")
        receiverName = intent.getStringExtra("receiverName")
        receiverProfileBase64 = intent.getStringExtra("receiverProfileBase64")

        val chatId = intent.getStringExtra("chatId")
        val receiverId = intent.getStringExtra("receiverId")
        val receiverName = intent.getStringExtra("receiverName")
        val receiverProfileBase64 = intent.getStringExtra("receiverProfileBase64")

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
        } else {
            throw IllegalArgumentException("Chat cannot be opened without chatId or receiverId")
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
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        canvas.drawBitmap(bitmap, -left.toFloat(), -top.toFloat(), paint)
        return output
    }

    private fun fetchOrCreateChatForReceiver() {
        if (receiverId.isNullOrEmpty()) return

        val chatsRef = FirebaseDatabase.getInstance().getReference("chats")
        val canonicalId = if (currentUserId < receiverId!!) "${currentUserId}_${receiverId}" else "{$receiverId}_${currentUserId}"

        // Check if a chat exists with this canonicalId
        chatsRef.orderByChild("chatId").equalTo(canonicalId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        chatId = snapshot.children.first().key
                        initMessagesRef(chatId!!)
                    } else {
                        // If chat doesn't exist (receiver opens first time), create it
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

        val message = ChatMessage(messageId, currentUserId, "", timestamp,base64Image , false)
        messagesRef.child(messageId).setValue(message)
        FirebaseDatabase.getInstance().getReference("chats").child(chatId!!)
            .child("lastMessage").setValue("[Image]")
    }
    private fun initiateCall(callType: String) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val selectedUid = receiverId ?: return

        val callId = if (currentUid < selectedUid) "${currentUid}_${selectedUid}" else "${selectedUid}_${currentUid}"

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
                val intent = Intent(this, call_screen::class.java)
                intent.putExtra("callId", callId)
                intent.putExtra("callType", callType)
                intent.putExtra("receiverId", selectedUid)
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

        val options = if (canEdit) arrayOf("Edit", "Delete", "Cancel") else arrayOf("Delete", "Cancel")

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
