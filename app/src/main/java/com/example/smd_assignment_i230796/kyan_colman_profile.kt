package com.example.smd_assignment_i230796

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class kyan_colman_profile : AppCompatActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var ivProfilePic: CircleImageView
    private lateinit var tvPostsCount: TextView
    private lateinit var tvFollowersCount: TextView
    private lateinit var tvFollowingCount: TextView
    private lateinit var recyclerProfilePosts: RecyclerView
    private lateinit var btnFollow: androidx.appcompat.widget.AppCompatButton
    private lateinit var tvBio: TextView   // ✅ added for displaying bio

    private lateinit var auth: FirebaseAuth
    private lateinit var usersRef: DatabaseReference
    private lateinit var postsRef: DatabaseReference

    private lateinit var postAdapter: ProfilePostAdapter
    private val postImages = mutableListOf<String>()

    private var visitedUserId: String? = null
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kyan_colman_profile)

        visitedUserId = intent.getStringExtra("visitedUserId")
        if (visitedUserId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val visuId = intent.getStringExtra("visitedUserId")
        if (visuId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        updateStoryBorder(visuId)
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid
        usersRef = FirebaseDatabase.getInstance().getReference("Users")
        postsRef = FirebaseDatabase.getInstance().getReference("Posts")

        // Initialize Views
        ivProfilePic = findViewById(R.id.ivProfilePic)
        tvUsername = findViewById(R.id.tvUsername)
        tvPostsCount = findViewById(R.id.tvPostsCount)
        tvFollowersCount = findViewById(R.id.tvFollowersCount)
        tvFollowingCount = findViewById(R.id.tvFollowingCount)
        btnFollow = findViewById(R.id.stateButton)
        recyclerProfilePosts = findViewById(R.id.recyclerProfilePosts)
        tvBio = findViewById(R.id.tvBio)  // ✅ new TextView for bio (make sure you add it in XML)

        // Click listeners for followers/following
        tvFollowersCount.setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("userId", visitedUserId)
            intent.putExtra("type", "followers")
            startActivity(intent)
        }

        tvFollowingCount.setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("userId", visitedUserId)
            intent.putExtra("type", "following")
            startActivity(intent)
        }

        recyclerProfilePosts.layoutManager = GridLayoutManager(this, 3)
        postAdapter = ProfilePostAdapter(postImages)
        recyclerProfilePosts.adapter = postAdapter

        // Load user info and posts
        loadVisitedUserInfo()
        loadVisitedUserPosts()
        setupFollowButton()
        setupBottomNav()
    }

    // ✅ Now includes bio
    private fun loadVisitedUserInfo() {
        usersRef.child(visitedUserId!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    tvUsername.text = "@${user.username ?: "username"}"
                    tvFollowersCount.text = (user.followers?.size ?: 0).toString()
                    tvFollowingCount.text = (user.following?.size ?: 0).toString()

                    // ✅ Display bio (default if empty)
                    tvBio.text = if (!user.bio.isNullOrEmpty()) user.bio else "No bio yet"

                    // ✅ Decode profile image
                    if (!user.profileImage.isNullOrEmpty()) {
                        try {
                            val bytes = Base64.decode(user.profileImage, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            ivProfilePic.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadVisitedUserPosts() {
        postsRef.orderByChild("userId").equalTo(visitedUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    postImages.clear()
                    for (postSnap in snapshot.children) {
                        val firstImageSnap = postSnap.child("imageBase64List").children.firstOrNull()
                        val imageBase64 = firstImageSnap?.getValue(String::class.java)

                        if (!imageBase64.isNullOrEmpty()) {
                            try {
                                val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                                    postImages.add(imageBase64)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    postAdapter.notifyDataSetChanged()
                    tvPostsCount.text = postImages.size.toString()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupFollowButton() {
        val fm = FollowManager

        fm.isFollowing(currentUserId!!, visitedUserId!!) { isFollowing ->
            if (isFollowing) {
                btnFollow.text = "Following"
                btnFollow.setBackgroundResource(R.drawable.btn_following_bg)
                btnFollow.setOnClickListener {
                    fm.unfollow(currentUserId!!, visitedUserId!!) {
                        if (it) {
                            btnFollow.text = "Follow"
                            btnFollow.setBackgroundResource(R.drawable.btn_follow_bg)
                            updateCounts()
                        }
                    }
                }
            } else {
                fm.isRequestPending(currentUserId!!, visitedUserId!!) { iSentRequest ->
                    if (iSentRequest) {
                        btnFollow.text = "Requested"
                        btnFollow.setBackgroundResource(R.drawable.btn_requested_bg)
                        btnFollow.setOnClickListener {
                            fm.cancelFollowRequest(currentUserId!!, visitedUserId!!) {
                                if (it) {
                                    btnFollow.text = "Follow"
                                    btnFollow.setBackgroundResource(R.drawable.btn_follow_bg)
                                }
                            }
                        }
                    } else {
                        fm.isRequestPending(visitedUserId!!, currentUserId!!) { theySentRequest ->
                            if (theySentRequest) {
                                btnFollow.text = "Accept Request"
                                btnFollow.setBackgroundResource(R.drawable.btn_follow_bg)
                                btnFollow.setOnClickListener {
                                    fm.acceptFollowRequest(visitedUserId!!, currentUserId!!) {
                                        if (it) {
                                            btnFollow.text = "Following"
                                            btnFollow.setBackgroundResource(R.drawable.btn_following_bg)
                                            updateCounts()
                                            Toast.makeText(
                                                this@kyan_colman_profile,
                                                "Follow request accepted",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            } else {
                                btnFollow.text = "Follow"
                                btnFollow.setBackgroundResource(R.drawable.btn_follow_bg)
                                btnFollow.setOnClickListener {
                                    fm.sendFollowRequest(currentUserId!!, visitedUserId!!) {
                                        if (it) {
                                            btnFollow.text = "Requested"
                                            btnFollow.setBackgroundResource(R.drawable.btn_requested_bg)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateCounts() {
        usersRef.child(visitedUserId!!).child("followers").get().addOnSuccessListener {
            tvFollowersCount.text = (it.childrenCount).toString()
        }
        usersRef.child(currentUserId!!).child("following").get().addOnSuccessListener {
            tvFollowingCount.text = (it.childrenCount).toString()
        }
    }

    private fun setupBottomNav() {
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
                        icprofile.setImageResource(R.drawable.profile)
                    }
                } else {
                    icprofile.setImageResource(R.drawable.profile)
                }
            }.addOnFailureListener {
                icprofile.setImageResource(R.drawable.profile)
            }

        icprofile.setOnClickListener {
            startActivity(Intent(this, your_profile_screen::class.java))
            overridePendingTransition(0, 0)
        }
    }

    // --- STORY BORDER ---
    private fun updateStoryBorder(profileOwnerId: String) {
        val viewerId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val usersRef = FirebaseDatabase.getInstance().getReference("Users")
        val storiesRef = FirebaseDatabase.getInstance().getReference("Stories")
        val profileImageView = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.ivProfilePic)

        val borderGray = ContextCompat.getColor(this, R.color.grayy)
        val borderGreen = ContextCompat.getColor(this, R.color.greenn)
        val borderRed = ContextCompat.getColor(this, R.color.redd)

        fun applyBorderColor(isFollowing: Boolean, isCloseFriend: Boolean) {
            if (!isFollowing && !isCloseFriend) {
                profileImageView.borderColor = borderGray
                profileImageView.invalidate()
                return
            }

            storiesRef.child(profileOwnerId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            profileImageView.borderColor = borderGray
                            profileImageView.invalidate()
                            return
                        }

                        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                        sdf.timeZone = TimeZone.getTimeZone("UTC")

                        var latestStoryTime = 0L
                        var latestStoryViewed = true
                        var latestStoryCloseFriends = false

                        for (storySnap in snapshot.children) {
                            val storyTime = storySnap.child("timestamp").value?.toString()
                            val parsedTime = storyTime?.let { sdf.parse(it)?.time ?: 0L } ?: 0L

                            if (parsedTime > latestStoryTime) {
                                latestStoryTime = parsedTime
                                latestStoryViewed = storySnap.child("isViewed")
                                    .child(viewerId).getValue(Boolean::class.java) ?: true
                                latestStoryCloseFriends = storySnap.child("isCloseFriends")
                                    .getValue(Boolean::class.java) ?: false
                            }
                        }

                        val currentTime = System.currentTimeMillis()
                        val storyActive = (currentTime - latestStoryTime) < (24 * 60 * 60 * 1000)

                        if (!storyActive) {
                            profileImageView.borderColor = borderGray
                        } else {
                            if (latestStoryCloseFriends && !isCloseFriend) {
                                profileImageView.borderColor = borderGray
                            } else {
                                profileImageView.borderColor = when {
                                    latestStoryViewed -> borderGray
                                    latestStoryCloseFriends -> borderGreen
                                    else -> borderRed
                                }
                            }
                        }

                        profileImageView.invalidate()
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        val followingRef = usersRef.child(viewerId).child("following").child(profileOwnerId)
        val closeFriendsRef = usersRef.child(profileOwnerId).child("closeFriends").child(viewerId)

        val relationshipListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                followingRef.get().addOnSuccessListener { followingSnap ->
                    val isFollowing = followingSnap.exists()

                    closeFriendsRef.get().addOnSuccessListener { closeFriendSnap ->
                        val isCloseFriend = closeFriendSnap.exists()
                        applyBorderColor(isFollowing, isCloseFriend)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        followingRef.addValueEventListener(relationshipListener)
        closeFriendsRef.addValueEventListener(relationshipListener)
    }
}