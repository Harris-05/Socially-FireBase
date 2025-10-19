package com.example.smd_assignment_i230796

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_assignment_i230796.adapters.ChatPreviewAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class dm_feed : BaseActivity() {



    private lateinit var recyclerChats: RecyclerView
    private lateinit var chatAdapter: ChatPreviewAdapter
    private val chatList = mutableListOf<ChatPreview>()

    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageView
    private lateinit var btnAdd: ImageView
    private lateinit var dbRef: DatabaseReference
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dm_feed)

        recyclerChats = findViewById(R.id.recyclerChats)
        etSearch = findViewById(R.id.etSearch)
        btnBack = findViewById(R.id.btnBack)
        btnAdd = findViewById(R.id.btnAdd)
        val username = findViewById<TextView>(R.id.tvUsername)

        val db = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)

        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val un = snapshot.child("username").getValue(String::class.java)
                username.text=un
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })



        btnBack.setOnClickListener { finish() }
        btnAdd.setOnClickListener { startActivity(Intent(this, AddChatActivity::class.java)) }

        recyclerChats.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatPreviewAdapter(
            chatList,
            onChatClick = { chat ->
                val intent = Intent(this, chat_screen::class.java)
                intent.putExtra("chatId", chat.chatId)
                intent.putExtra("receiverId", chat.otherUserId)
                intent.putExtra("receiverName", chat.username)
                intent.putExtra("receiverProfileBase64", chat.profileImage)
                startActivity(intent)
            },
            onCameraClick = { /* optional */ }
        )
        recyclerChats.adapter = chatAdapter

        dbRef = FirebaseDatabase.getInstance().getReference("chats")
        loadChats()
    }

    private fun loadChats() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()

                for (chatSnap in snapshot.children) {
                    val userId1 = chatSnap.child("userId1").getValue(String::class.java)
                    val userId2 = chatSnap.child("userId2").getValue(String::class.java)
                    val lastMsg = chatSnap.child("lastMessage").getValue(String::class.java) ?: ""
                    val timestamp = chatSnap.child("timestamp").getValue(Long::class.java) ?: 0L

                    if (userId1.isNullOrEmpty() || userId2.isNullOrEmpty()) continue

                    // Generate canonical chat ID (same for both users)
                    val canonicalChatId = if (userId1 < userId2) {
                        "${userId1}_${userId2}"
                    } else {
                        "${userId2}_${userId1}"
                    }

                    // Only show chats related to current user
                    if (userId1 == currentUserId || userId2 == currentUserId) {
                        val otherUserId = if (userId1 == currentUserId) userId2 else userId1
                        fetchUserInfo(otherUserId!!, lastMsg, timestamp, canonicalChatId)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchUserInfo(
        userId: String,
        lastMsg: String,
        timestamp: Long,
        chatId: String
    ) {
        val usersRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                val profileBase64 = snapshot.child("profileImage").getValue(String::class.java) ?: ""

                val chatPreview = ChatPreview(
                    chatId = chatId,
                    otherUserId = userId,
                    username = username,
                    profileImage = profileBase64,
                    lastMessage = lastMsg,
                    lastMessageTime = timestamp
                )

                // Prevent duplicates
                val existingIndex = chatList.indexOfFirst { it.chatId == chatId }
                if (existingIndex >= 0) {
                    chatList[existingIndex] = chatPreview
                } else {
                    chatList.add(chatPreview)
                }

                chatList.sortByDescending { it.lastMessageTime }
                chatAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
