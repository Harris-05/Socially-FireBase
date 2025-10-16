package com.example.smd_assignment_i230796

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_assignment_i230796.UserAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserAdapter
    private val userList = ArrayList<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        recyclerView = findViewById(R.id.recyclerViewUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val userId = intent.getStringExtra("userId")
        val type = intent.getStringExtra("type") // "followers" or "following"

        if (userId == null || type == null) {
            Toast.makeText(this, "Invalid request", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Use your existing adapter with click support
        adapter = UserAdapter(userList) { user ->
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val clickedUserId = user.uid

            if (clickedUserId == currentUserId) {
                // If it's the current user, open their own profile
                val intent = Intent(this, your_profile_screen::class.java)
                startActivity(intent)
            } else {
                // Otherwise, open the visited user's profile
                val intent = Intent(this, kyan_colman_profile::class.java)
                intent.putExtra("visitedUserId", clickedUserId)
                startActivity(intent)
            }
        }
        recyclerView.adapter = adapter

        loadUsers(userId, type)
    }

    private fun loadUsers(userId: String, type: String) {
        val dbRef = FirebaseDatabase.getInstance()
            .getReference("Users").child(userId).child(type)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    FirebaseDatabase.getInstance().getReference("Users").child(id)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnap: DataSnapshot) {
                                val user = userSnap.getValue(User::class.java)
                                if (user != null) {
                                    userList.add(user)
                                    adapter.notifyDataSetChanged()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
