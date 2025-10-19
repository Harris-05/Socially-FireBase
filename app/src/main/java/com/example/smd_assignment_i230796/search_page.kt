package com.example.smd_assignment_i230796

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_assignment_i230796.UserAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class search_page : BaseActivity() {

    private lateinit var etSearch: EditText
    private lateinit var recyclerUsers: RecyclerView
    private lateinit var tvNoResults: TextView

    private lateinit var userAdapter: UserAdapter
    private var userList: MutableList<User> = mutableListOf()

    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_page)

        etSearch = findViewById(R.id.et_search)
        recyclerUsers = findViewById(R.id.recyclerUsers)
        tvNoResults = findViewById(R.id.tvNoResults)

        dbRef = FirebaseDatabase.getInstance().getReference("Users")

        recyclerUsers.layoutManager = LinearLayoutManager(this)

        // ✅ Handle user click here
        userAdapter = UserAdapter(userList) { user ->
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

        recyclerUsers.adapter = userAdapter

        val categoriesScroll = findViewById<View>(R.id.categories_scroll)
        val searchGrid = findViewById<View>(R.id.search_grid)
        val bottomNavContainer = findViewById<View>(R.id.bottom_nav_container)

        // Hide other UI when focusing on search
        etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                categoriesScroll.visibility = View.GONE
                searchGrid.visibility = View.GONE
                bottomNavContainer.visibility = View.GONE
                recyclerUsers.visibility = View.VISIBLE
                etSearch.requestFocus()
            } else {
                if (etSearch.text.isEmpty()) {
                    categoriesScroll.visibility = View.VISIBLE
                    searchGrid.visibility = View.VISIBLE
                    bottomNavContainer.visibility = View.VISIBLE
                    recyclerUsers.visibility = View.GONE
                    tvNoResults.visibility = View.GONE
                }
            }
        }

        setupSearchListener()
        bottomNav()
    }

    //-------------bottom nav-----------------------------
    private fun bottomNav() {
        findViewById<ImageView>(R.id.iv_nav_home).setOnClickListener {
            startActivity(Intent(this, main_feed::class.java))
            overridePendingTransition(0, 0)
        }
        findViewById<ImageView>(R.id.iv_nav_search).setOnClickListener {
            startActivity(Intent(this, search_page::class.java))
            overridePendingTransition(0, 0)
        }
        findViewById<ImageView>(R.id.iv_nav_add).setOnClickListener {
            startActivity(Intent(this, AddChoiceActivity::class.java))
            overridePendingTransition(0, 0)
        }
        findViewById<ImageView>(R.id.iv_nav_heart).setOnClickListener {
            startActivity(Intent(this, following_notif::class.java))
            overridePendingTransition(0, 0)
        }
        val icprofile = findViewById<ImageView>(R.id.iv_your_profile)

        val userRef = FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser?.uid ?: "demoUser123")
        userRef.child("profileImage").get()
            .addOnSuccessListener { snapshot ->
                val profileBase64 = snapshot.getValue(String::class.java)
                if (!profileBase64.isNullOrEmpty()) {
                    try {
                        val bytes = Base64.decode(profileBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        icprofile.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        icprofile.setImageResource(R.drawable.profile)
                    }
                } else {
                    icprofile.setImageResource(R.drawable.profile)
                }
            }
            .addOnFailureListener {
                icprofile.setImageResource(R.drawable.profile)
            }

        findViewById<ImageView>(R.id.iv_your_profile).setOnClickListener {
            startActivity(Intent(this, your_profile_screen::class.java))
            overridePendingTransition(0, 0)
        }
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val queryText = s.toString().trim()
                if (queryText.isNotEmpty()) {
                    performUserSearch(queryText)
                } else {
                    recyclerUsers.visibility = View.GONE
                    tvNoResults.visibility = View.GONE
                    userList.clear()
                    userAdapter.notifyDataSetChanged()
                }
            }
        })
    }

    private fun performUserSearch(queryText: String) {
        val queryLower = queryText.lowercase()

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()

                if (snapshot.exists()) {
                    for (userSnap in snapshot.children) {
                        val user = userSnap.getValue(User::class.java)
                        user?.let {
                            val username = it.username?.lowercase() ?: ""
                            if (username.contains(queryLower)) {
                                userList.add(it)
                            }
                        }
                    }

                    if (userList.isEmpty()) {
                        recyclerUsers.visibility = View.GONE
                        tvNoResults.visibility = View.VISIBLE
                    } else {
                        recyclerUsers.visibility = View.VISIBLE
                        tvNoResults.visibility = View.GONE
                    }
                } else {
                    recyclerUsers.visibility = View.GONE
                    tvNoResults.visibility = View.VISIBLE
                }

                userAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                recyclerUsers.visibility = View.GONE
                tvNoResults.visibility = View.VISIBLE
                tvNoResults.text = "Error: ${error.message}"
            }
        })
    }
}
