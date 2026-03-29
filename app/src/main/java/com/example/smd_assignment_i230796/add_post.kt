package com.example.smd_assignment_i230796

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

class add_post : BaseActivity() {

    private val pickImagesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (!uris.isNullOrEmpty()) openImagePreview(uris)
        }
    //change
    private val postPreviewLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uris = result.data!!.getStringArrayListExtra("imageUris")?.map { Uri.parse(it) } ?: emptyList()
                val caption = result.data!!.getStringExtra("caption") ?: ""
                addNewPost(uris, caption)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_post)

        // Button to start image selection
        findViewById<Button>(R.id.btnPickImages).setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }
    }

    private fun openImagePreview(imageUris: List<Uri>) {
        val intent = Intent(this, post_preview::class.java)
        intent.putStringArrayListExtra("imageUris", ArrayList(imageUris.map { it.toString() }))
        postPreviewLauncher.launch(intent)
    }

    private fun addNewPost(imageUris: List<Uri>, caption: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = user.uid
        val usersRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

        usersRef.get().addOnSuccessListener { snapshot ->
            val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown User"
            val location = snapshot.child("location").getValue(String::class.java) ?: ""
            val profileImageUrl = snapshot.child("profileImage").getValue(String::class.java) ?: ""

            val imageBase64List = mutableListOf<String>()
            for (uri in imageUris) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                    val base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
                    imageBase64List.add(base64)
                    inputStream?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val postRef = FirebaseDatabase.getInstance().getReference("Posts").push()
            val postId = postRef.key ?: return@addOnSuccessListener

            val newPost = Post(
                postId = postId,
                userId = uid,
                username = username,
                location = location,
                caption = caption,
                imageBase64List = imageBase64List,
                profileImageUrl = profileImageUrl,
                likedByName = null,
                likeCount = 0,
                isVerified = false,
                comments = mutableListOf()
            )

            postRef.setValue(newPost)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Post uploaded successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "❌ Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
