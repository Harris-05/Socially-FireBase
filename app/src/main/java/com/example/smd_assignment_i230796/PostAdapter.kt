package com.example.smd_assignment_i230796

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_assignment_i230796.databinding.ItemPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PostAdapter(
    private val context: android.content.Context,
    private val posts: MutableList<Post>,
    private val currentUsername: String,
    private val currentUserProfileBase64: String?,
    private val userProfiles: MutableMap<String, String> = mutableMapOf()

) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun getItemCount() = posts.size

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // ---------- FETCH CURRENT USER INFO ----------
        val currentUser = FirebaseAuth.getInstance().currentUser
        var currentUsername = currentUser?.displayName ?: "You"
        val currentUserId = currentUser?.uid ?: ""
        var currentUserProfileBase64 = ""

        // Fetch user's profile image (if stored in Firebase)
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)
        userRef.child("profileImage").get().addOnSuccessListener { snapshot ->
            currentUserProfileBase64 = snapshot.getValue(String::class.java) ?: ""
        }
        userRef.child("username").get().addOnSuccessListener {  snapshot ->
            currentUsername =snapshot.getValue(String::class.java) ?: " "
        }

        //----------BASIC INFO----------
        holder.binding.tvUsername.text = post.username ?: ""
        holder.binding.tvLocation.text = post.location ?: ""
        holder.binding.igpostCaption.text = post.caption ?: ""
        holder.binding.tvLikedByName.text = post.likedByName ?: ""
        holder.binding.tvLikeCount.text = "${post.likeCount ?: 0} others"
        // ---------- INITIALIZE "Liked by" section when post loads ----------
        if (!post.likedBy.isNullOrEmpty() && post.likedBy!!.size > 1) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val likedByKeys = post.likedBy!!.keys

            // pick the first liker that is NOT the current user
            val firstLikerId: String? = likedByKeys.firstOrNull { it != currentUserId } ?: likedByKeys.firstOrNull()

            if (!firstLikerId.isNullOrEmpty()) {
                val usersRef = FirebaseDatabase.getInstance().getReference("Users").child(firstLikerId)
                usersRef.get().addOnSuccessListener { snapshot ->
                    val firstLikerName = snapshot.child("username").getValue(String::class.java)
                    val firstLikerProfile = snapshot.child("profileImage").getValue(String::class.java)

                    holder.binding.layoutLikedBySection.visibility = View.VISIBLE
                    holder.binding.tvLikeCount.text = "${post.likedBy!!.size -1} others"
                    holder.binding.tvLikedByName.text = firstLikerName ?: "Unknown"

                    if (!firstLikerProfile.isNullOrEmpty()) {
                        try {
                            val bytes = Base64.decode(firstLikerProfile, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            holder.binding.ivLikedByProfile.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            holder.binding.ivLikedByProfile.setImageResource(R.drawable.profile)
                        }
                    } else {
                        holder.binding.ivLikedByProfile.setImageResource(R.drawable.profile)
                    }
                }
            } else {
                holder.binding.layoutLikedBySection.visibility = View.GONE
            }
        } else {
            holder.binding.layoutLikedBySection.visibility = View.GONE
        }



        // ---------- PROFILE IMAGE (auto-updating) ----------
        val userProfileBase64 = userProfiles[post.userId] ?: post.profileImageUrl

        if (!userProfileBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(userProfileBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.binding.ivPostProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.binding.ivPostProfile.setImageResource(R.drawable.profile)
            }
        } else {
            holder.binding.ivPostProfile.setImageResource(R.drawable.profile)
        }


        // ---------- POST IMAGES ----------
        when {
            !post.imageBase64List.isNullOrEmpty() -> {
                if (post.imageBase64List!!.size == 1) {
                    holder.binding.ivMainPostImage.visibility = View.VISIBLE
                    holder.binding.viewPagerPost.visibility = View.GONE
                    holder.binding.postIndicator.visibility = View.GONE

                    val bytes = Base64.decode(post.imageBase64List!!.first(), Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    holder.binding.ivMainPostImage.setImageBitmap(bitmap)
                } else {
                    holder.binding.ivMainPostImage.visibility = View.GONE
                    holder.binding.viewPagerPost.visibility = View.VISIBLE
                    holder.binding.postIndicator.visibility = View.VISIBLE

                    val adapter = PostImagePagerAdapter(
                        imageUris = null,
                        imageBase64List = post.imageBase64List,
                        context = context
                    )
                    holder.binding.viewPagerPost.adapter = adapter
                    holder.binding.postIndicator.setViewPager(holder.binding.viewPagerPost)
                }
            }

            !post.imageUris.isNullOrEmpty() -> {
                if (post.imageUris!!.size == 1) {
                    holder.binding.ivMainPostImage.visibility = View.VISIBLE
                    holder.binding.viewPagerPost.visibility = View.GONE
                    holder.binding.postIndicator.visibility = View.GONE
                    holder.binding.ivMainPostImage.setImageURI(post.imageUris!!.first())
                } else {
                    holder.binding.ivMainPostImage.visibility = View.GONE
                    holder.binding.viewPagerPost.visibility = View.VISIBLE
                    holder.binding.postIndicator.visibility = View.VISIBLE

                    val adapter = PostImagePagerAdapter(
                        imageUris = post.imageUris,
                        imageBase64List = null,
                        context = context
                    )
                    holder.binding.viewPagerPost.adapter = adapter
                    holder.binding.postIndicator.setViewPager(holder.binding.viewPagerPost)
                }
            }

            else -> {
                holder.binding.ivMainPostImage.visibility = View.GONE
                holder.binding.viewPagerPost.visibility = View.GONE
                holder.binding.postIndicator.visibility = View.GONE
            }
        }

        // ---------- LIKE HANDLING ----------
        holder.binding.ivLike.setImageResource(
            if (post.isLiked) R.drawable.ic_heart_red else R.drawable.like
        )

        holder.binding.ivLike.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser ?: return@setOnClickListener
            val currentUserId = currentUser.uid
            val postRef = FirebaseDatabase.getInstance().getReference("Posts").child(post.postId ?: return@setOnClickListener)

            val newLiked = !post.isLiked
            post.isLiked = newLiked

            val likedByMap = post.likedBy?.toMutableMap() ?: mutableMapOf()

            // Update likedBy map
            if (newLiked) {
                likedByMap[currentUserId] = true
            } else {
                likedByMap.remove(currentUserId)
            }

            // Update like count
            post.likeCount = likedByMap.size
            post.likedBy = likedByMap

            // 🔹 Update first liker details (for UI section “Liked by …”)
            if (likedByMap.size > 1) {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val firstLikerId = likedByMap.keys.firstOrNull { it != currentUserId } ?: likedByMap.keys.first()

                val usersRef = FirebaseDatabase.getInstance().getReference("Users").child(firstLikerId)
                usersRef.get().addOnSuccessListener { snapshot ->
                    val firstLikerName = snapshot.child("username").getValue(String::class.java)
                    val firstLikerProfile = snapshot.child("profileImage").getValue(String::class.java) // base64

                    post.likedByName = firstLikerName ?: "Unknown"
                    post.likedByProfileResId = null // not used if base64

                    // Update liked-by section dynamically
                    holder.binding.layoutLikedBySection.visibility =
                        if ((post.likeCount ?: 0) <= 1) View.GONE else View.VISIBLE
                    holder.binding.tvLikeCount.text = "${post.likeCount ?: 0} others"

                    if (!firstLikerProfile.isNullOrEmpty()) {
                        try {
                            val bytes = Base64.decode(firstLikerProfile, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            holder.binding.ivLikedByProfile.setImageBitmap(bitmap)
                            holder.binding.tvLikedByName.text=firstLikerName
                        } catch (e: Exception) {
                            holder.binding.ivLikedByProfile.setImageResource(R.drawable.profile)
                        }
                    } else {
                        holder.binding.ivLikedByProfile.setImageResource(R.drawable.profile)
                    }

                    holder.binding.tvLikedByName.text = post.likedByName
                }
            } else {
                holder.binding.layoutLikedBySection.visibility = View.GONE
            }

            // Update like button instantly
            holder.binding.ivLike.setImageResource(
                if (newLiked) R.drawable.ic_heart_red else R.drawable.like
            )

            // Update Firebase atomically
            val updates = mapOf(
                "likeCount" to post.likeCount,
                "likedBy" to likedByMap
            )
            postRef.updateChildren(updates)
        }


        holder.binding.layoutLikedBySection.visibility =
            if ((post.likeCount ?: 0) <= 1) View.GONE else View.VISIBLE

        // ---------- COMMENTS SECTION ----------
        val commentsSection = holder.binding.commentsSection
        val commentCountText = holder.binding.tvCommentCount
        val inputBar = holder.binding.commentInputBar
        val etComment = holder.binding.etAddComment
        val btnPost = holder.binding.btnPostComment
        val btnCancel = holder.binding.btnCancelComment

        val comments = post.comments ?: mutableListOf()
        commentCountText.text = "${comments.size} comments"
        commentsSection.removeAllViews()

        val inflater = LayoutInflater.from(context)

        // ---------- Inflate existing comments (auto-updating with latest profile images) ----------
        if (comments.isNotEmpty()) {
            commentsSection.removeAllViews()

            for (c in comments) {
                val commentView = inflater.inflate(R.layout.item_comment, commentsSection, false)
                val ivProfile = commentView.findViewById<ImageView>(R.id.ivCommentProfile)
                val tvUsername = commentView.findViewById<TextView>(R.id.tvCommentUsername)
                val tvText = commentView.findViewById<TextView>(R.id.tvCommentText)

                tvUsername.text = c.username ?: ""
                tvText.text = c.text ?: ""

                // 🔹 Always prefer latest image from userProfiles (live Firebase updates)
                val latestProfileBase64 = userProfiles[c.userId] ?: c.profileImageBase64

                if (!latestProfileBase64.isNullOrEmpty()) {
                    try {
                        val cleaned = if (latestProfileBase64.contains(",")) latestProfileBase64.substringAfter(",") else latestProfileBase64
                        val bytes = android.util.Base64.decode(cleaned, android.util.Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ivProfile.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        ivProfile.setImageResource(R.drawable.profile)
                    }
                } else {
                    ivProfile.setImageResource(R.drawable.profile)
                }

                commentsSection.addView(commentView)
            }
        }


        // ---------- COMMENT VISIBILITY ----------
        commentsSection.visibility = View.GONE
        inputBar.visibility = View.GONE

        holder.binding.ivComment.setOnClickListener {
            val isVisible = commentsSection.visibility == View.VISIBLE
            commentsSection.visibility = if (isVisible) View.GONE else View.VISIBLE
            inputBar.visibility = if (isVisible) View.GONE else View.VISIBLE
        }

        commentCountText.visibility = View.VISIBLE
        commentCountText.setOnClickListener {
            inputBar.visibility =
                if (inputBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }





        // ---------- ADD NEW COMMENT ----------

        btnPost.setOnClickListener {
            val commentText = etComment.text.toString().trim()

            if (commentText.isNotEmpty()) {
                val newComment = comment(
                    userId = currentUserId,
                    profileImageBase64 = currentUserProfileBase64,
                    username = currentUsername,
                    text = commentText
                )

                // Add locally first
                post.comments?.add(newComment) ?: run {
                    post.comments = mutableListOf(newComment)
                }

                // Instantly show in UI (no Firebase delay)
                val commentView = inflater.inflate(R.layout.item_comment, commentsSection, false)
                val ivProfile = commentView.findViewById<ImageView>(R.id.ivCommentProfile)
                val tvUsername = commentView.findViewById<TextView>(R.id.tvCommentUsername)
                val tvText = commentView.findViewById<TextView>(R.id.tvCommentText)

                tvUsername.text = currentUsername
                tvText.text = commentText

                if (!currentUserProfileBase64.isNullOrEmpty()) {
                    try {
                        val bytes = android.util.Base64.decode(currentUserProfileBase64, android.util.Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ivProfile.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        ivProfile.setImageResource(R.drawable.profile)
                    }
                } else {
                    ivProfile.setImageResource(R.drawable.profile)
                }

                commentsSection.addView(commentView)

                // Clear input instantly
                etComment.text.clear()
                inputBar.visibility = View.GONE

                // Save to Firebase
                val postRef = FirebaseDatabase.getInstance()
                    .getReference("Posts")
                    .child(post.postId ?: return@setOnClickListener)

                postRef.child("comments").setValue(post.comments)
                    .addOnSuccessListener {
                        notifyItemChanged(position)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to add comment: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        btnCancel.setOnClickListener {
            etComment.text.clear()
            inputBar.visibility = View.GONE
        }
    }





    // ---------- LIKE TOGGLE ----------
    private fun toggleLike(post: Post, holder: PostViewHolder) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        val postRef = FirebaseDatabase.getInstance()
            .getReference("Posts")
            .child(post.postId!!)

        // If post already has likedBy map
        val likedBy = post.likedBy?.toMutableMap() ?: mutableMapOf()

        val isCurrentlyLiked = likedBy.containsKey(userId)
        val newLiked = !isCurrentlyLiked

        // Toggle like
        if (newLiked) {
            likedBy[userId] = true
            post.likeCount = (post.likeCount ?: 0) + 1
        } else {
            likedBy.remove(userId)
            post.likeCount = (post.likeCount ?: 0) - 1
            if (post.likeCount!! < 0) post.likeCount = 0
        }

        // Update UI immediately
        post.isLiked = newLiked
        holder.binding.ivLike.setImageResource(
            if (newLiked) R.drawable.ic_heart_red else R.drawable.like
        )
        holder.binding.tvLikeCount.text = "${post.likeCount} others"
        holder.binding.layoutLikedBySection.visibility =
            if ((post.likeCount ?: 0) <= 1) View.GONE else View.VISIBLE

        // Update Firebase atomically
        val updates = mapOf(
            "likeCount" to post.likeCount,
            "likedBy" to likedBy
        )

        postRef.updateChildren(updates)
            .addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    "Failed to update like: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    fun updateUserProfiles(newProfiles: Map<String, String>) {
        userProfiles.clear()
        userProfiles.putAll(newProfiles)

        // 🔹 Refresh all posts and nested comments instantly
        posts.forEach { post ->
            post.profileImageUrl = userProfiles[post.userId] ?: post.profileImageUrl
            post.comments?.forEach { c ->
                c.profileImageBase64 = userProfiles[c.userId] ?: c.profileImageBase64
            }
        }

        notifyDataSetChanged()
    }

}
