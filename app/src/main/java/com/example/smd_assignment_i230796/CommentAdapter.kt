package com.example.smd_assignment_i230796

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_assignment_i230796.databinding.ItemCommentBinding
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.codec.binary.Base64

class CommentAdapter(
    private val comments: List<comment>,
    private val userProfiles: MutableMap<String, String>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun getItemCount() = comments.size

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val c = comments[position]

        // 🔹 Always prefer the latest user profile image from Firebase map
        val latestProfileBase64 = userProfiles[c.userId] ?: c.profileImageBase64

        if (!latestProfileBase64.isNullOrBlank()) {
            try {
                val cleaned = if (latestProfileBase64.contains(",")) latestProfileBase64.substringAfter(",") else latestProfileBase64
                val bytes = android.util.Base64.decode(cleaned, android.util.Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) holder.binding.ivCommentProfile.setImageBitmap(bitmap)
                else holder.binding.ivCommentProfile.setImageResource(R.drawable.profile)
            } catch (e: Exception) {
                holder.binding.ivCommentProfile.setImageResource(R.drawable.profile)
            }
        } else {
            holder.binding.ivCommentProfile.setImageResource(R.drawable.profile)
        }

        holder.binding.tvCommentUsername.text = c.username ?: ""
        holder.binding.tvCommentText.text = c.text ?: ""
    }

    fun updateUserProfiles(newProfiles: Map<String, String>) {
        userProfiles.clear()
        userProfiles.putAll(newProfiles)

        // 🔹 Update all bound comment items
        notifyDataSetChanged()
    }

}
