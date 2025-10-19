package com.example.smd_assignment_i230796

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.example.smd_assignment_i230796.UserAdapter
import com.google.firebase.database.*

class AddChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference
    private lateinit var chatsRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private val userList = mutableListOf<User>()
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_chat)

        recyclerView = findViewById(R.id.recyclerUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        database = FirebaseDatabase.getInstance()
        usersRef = database.getReference("Users")
        chatsRef = database.getReference("chats")
        auth = FirebaseAuth.getInstance()

        loadAllUsers()
    }

    private fun loadAllUsers() {
        val currentUid = auth.currentUser?.uid ?: return

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                if (snapshot.exists()) {
                    for (userSnap in snapshot.children) {
                        val user = userSnap.getValue(User::class.java)
                        if (user != null && user.uid != currentUid) {
                            userList.add(user)
                        }
                    }
                }

                adapter = UserAdapter(userList) { selectedUser ->
                    startOrOpenChat(selectedUser)
                }
                recyclerView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun startOrOpenChat(selectedUser: User) {
        val currentUid = auth.currentUser?.uid ?: return
        val selectedUid = selectedUser.uid ?: return

        // Deterministic chatId using canonical order
        val chatId = if (currentUid < selectedUid) "${currentUid}_${selectedUid}" else "${selectedUid}_${currentUid}"


        // Check if this chat exists already
        chatsRef.child(chatId).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                // Create new chat with canonical key
                val chatData = mapOf(
                    "chatId" to chatId,  // use same key
                    "userId1" to currentUid,
                    "userId2" to selectedUid,
                    "lastMessage" to "",
                    "timestamp" to System.currentTimeMillis()
                )
                chatsRef.child(chatId).setValue(chatData)
            }

            // Open chat screen
            openChat(selectedUser, chatId)
        }.addOnFailureListener {
            // fallback if needed
        }
    }

    private fun openChat(selectedUser: User, chatId: String) {
        val intent = Intent(this, chat_screen::class.java)
        intent.putExtra("chatId", chatId)
        intent.putExtra("receiverId", selectedUser.uid)
        intent.putExtra("receiverName", selectedUser.username)
        intent.putExtra("receiverProfileBase64", selectedUser.profileImage)
        startActivity(intent)
    }
}
