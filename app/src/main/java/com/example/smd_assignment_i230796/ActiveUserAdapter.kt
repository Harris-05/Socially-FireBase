package com.example.smd_assignment_i230796
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class ActiveUserAdapter(
    private val users: List<ActiveUser>,
    private val onUserClick: ((ActiveUser) -> Unit)? = null
) : RecyclerView.Adapter<ActiveUserAdapter.ActiveUserViewHolder>() {

    inner class ActiveUserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProfile: ImageView = view.findViewById(R.id.imgProfile)
        val onlineIndicator: View = view.findViewById(R.id.onlineIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveUserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_active_user, parent, false)
        return ActiveUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActiveUserViewHolder, position: Int) {
        val user = users[position]

        if (user.profileImage.isNotEmpty()) {
            if (user.profileImage.startsWith("http")) {
                // URL case
                Glide.with(holder.itemView.context)
                    .load(user.profileImage)
                    .placeholder(R.drawable.main_profile_circle)
                    .into(holder.imgProfile)
            } else {
                // Base64 case
                try {
                    val bytes = Base64.decode(user.profileImage, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    holder.imgProfile.setImageBitmap(bmp)
                } catch (e: Exception) {
                    holder.imgProfile.setImageResource(R.drawable.main_profile_circle)
                }
            }
        } else {
            holder.imgProfile.setImageResource(R.drawable.main_profile_circle)
        }

        // Disable click — do nothing
        if (onUserClick == null) {
            holder.itemView.isClickable = false
            holder.itemView.isFocusable = false
        } else {
            holder.itemView.setOnClickListener { onUserClick.invoke(user) }
        }

        // Green circle always visible since they’re active
        holder.onlineIndicator.visibility = View.VISIBLE
    }

    override fun getItemCount(): Int = users.size
}
