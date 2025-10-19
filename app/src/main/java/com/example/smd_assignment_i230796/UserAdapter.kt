package com.example.smd_assignment_i230796

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.smd_assignment_i230796.R
import com.example.smd_assignment_i230796.User

class UserAdapter(
    private val userList: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProfile: ImageView = view.findViewById(R.id.imgProfile)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.tvUsername.text = user.username

        if (!user.profileImage.isNullOrBlank()) {
            try {
                // Decode Base64 → Bitmap
                val imageBytes = android.util.Base64.decode(user.profileImage, android.util.Base64.DEFAULT)
                val decodedImage = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.imgProfile.setImageBitmap(decodedImage)
            } catch (e: Exception) {
                e.printStackTrace()
                holder.imgProfile.setImageResource(R.drawable.jack_profile)
            }
        } else {
            // Default placeholder if no image
            holder.imgProfile.setImageResource(R.drawable.jack_profile)
        }

        holder.itemView.setOnClickListener { onUserClick(user) }
    }


    override fun getItemCount() = userList.size
}
