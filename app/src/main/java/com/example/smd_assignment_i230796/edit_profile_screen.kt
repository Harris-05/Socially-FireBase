package com.example.smd_assignment_i230796

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

class edit_profile_screen : BaseActivity() {

    private lateinit var cancelBtn: TextView
    private lateinit var doneBtn: TextView
    private lateinit var ivProfilePic: ImageView
    private lateinit var nameEt: EditText
    private lateinit var usernameEt: EditText
    private lateinit var websiteEt: EditText
    private lateinit var bioEt: EditText
    private lateinit var changephoto: TextView
    private lateinit var emailEt: EditText
    private lateinit var phoneEt: EditText
    private lateinit var genderEt: EditText

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageBase64: String? = null

    private val auth = FirebaseAuth.getInstance()
    private val usersRef = FirebaseDatabase.getInstance().getReference("Users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_profile_screen)

        // 🔹 Bind views
        cancelBtn = findViewById(R.id.cancel_btn)
        doneBtn = findViewById(R.id.done_btn)
        ivProfilePic = findViewById(R.id.iv_profile_pic)
        nameEt = findViewById(R.id.name_edittext)
        usernameEt = findViewById(R.id.username_edittext)
        websiteEt = findViewById(R.id.website_edittext)
        bioEt = findViewById(R.id.bio_edittext)
        emailEt = findViewById(R.id.email_edittext)
        phoneEt = findViewById(R.id.phone_edittext)
        changephoto = findViewById(R.id.changephoto)
        genderEt = findViewById(R.id.gender_edittext)

        val uid = auth.currentUser?.uid ?: return

        // 🔹 Load current data from Firebase
        usersRef.child(uid).get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue(User::class.java)
            user?.let {
                val fullName = listOfNotNull(it.firstName, it.lastName).joinToString(" ")
                nameEt.setText(fullName)
                usernameEt.setText(it.username ?: "")
                websiteEt.setText("") // optional, not in User class
                bioEt.setText(it.bio ?: "")
                emailEt.setText(it.email ?: "")
                phoneEt.setText(it.phoneNumber ?: "")
                genderEt.setText(it.gender ?: "")

                if (!it.profileImage.isNullOrEmpty()) {
                    val decodedBytes = Base64.decode(it.profileImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    ivProfilePic.setImageBitmap(bitmap)
                }
            }
        }

        // 🔹 Cancel button
        cancelBtn.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        // 🔹 Pick new profile image
        changephoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // 🔹 Done (Save changes)
        doneBtn.setOnClickListener {
            val fullName = nameEt.text.toString().trim()
            val firstName = fullName.substringBefore(" ", fullName)
            val lastName = fullName.substringAfter(" ", "")
            val updates = mutableMapOf<String, Any>()

            updates["firstName"] = firstName
            updates["lastName"] = lastName
            updates["username"] = usernameEt.text.toString().trim()
            updates["bio"] = bioEt.text.toString().trim()
            updates["email"] = emailEt.text.toString().trim()
            updates["phoneNumber"] = phoneEt.text.toString().trim()
            updates["gender"] = genderEt.text.toString().trim()

            selectedImageBase64?.let {
                updates["profileImage"] = it
            }

            usersRef.child(uid).updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                    overridePendingTransition(0, 0)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // 🔹 Handle image selection result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            ivProfilePic.setImageBitmap(bitmap)
            selectedImageBase64 = encodeImageToBase64(bitmap)
        }
    }

    // 🔹 Convert image to Base64
    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val bytes = baos.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }
}
