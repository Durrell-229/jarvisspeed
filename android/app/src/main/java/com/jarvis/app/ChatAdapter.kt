package com.jarvis.app

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    data class ChatItem(
        val role: String,
        val text: String,
        val actionExecuted: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val items = mutableListOf<ChatItem>()

    companion object {
        const val TYPE_USER = 1
        const val TYPE_JARVIS = 2
        const val TYPE_ACTION = 3
        const val TYPE_SYSTEM = 4
    }

    fun addMessage(role: String, text: String, actionExecuted: String? = null) {
        val pos = items.size
        items.add(ChatItem(role, text, actionExecuted))
        notifyItemInserted(pos)
    }

    fun addSystemMessage(text: String) {
        addMessage("system", text)
    }

    fun clear() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun getItems() = items.toList()

    override fun getItemViewType(position: Int): Int {
        return when (items[position].role) {
            "user" -> TYPE_USER
            "action" -> TYPE_ACTION
            "system" -> TYPE_SYSTEM
            else -> TYPE_JARVIS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> {
                val view = inflater.inflate(R.layout.item_message_user, parent, false)
                UserViewHolder(view)
            }
            TYPE_ACTION -> {
                val view = inflater.inflate(R.layout.item_action_badge, parent, false)
                ActionViewHolder(view)
            }
            TYPE_SYSTEM -> {
                val view = inflater.inflate(R.layout.item_message_system, parent, false)
                SystemViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_message_ai, parent, false)
                JarvisViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        when (holder) {
            is UserViewHolder -> {
                holder.messageText.text = item.text
                holder.itemView.alpha = 0f
                holder.itemView.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            is JarvisViewHolder -> {
                holder.messageText.text = item.text
                if (item.actionExecuted != null) {
                    holder.actionBadge.visibility = View.VISIBLE
                    holder.actionBadge.text = "⚡ ${item.actionExecuted}"
                } else {
                    holder.actionBadge.visibility = View.GONE
                }
                holder.itemView.alpha = 0f
                holder.itemView.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            is ActionViewHolder -> {
                holder.actionText.text = "⚡ ${item.actionExecuted}"
            }
            is SystemViewHolder -> {
                holder.messageText.text = item.text
            }
        }
    }

    override fun getItemCount() = items.size

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
    }

    class JarvisViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
        val actionBadge: TextView = view.findViewById(R.id.actionBadge)
    }

    class ActionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val actionText: TextView = view.findViewById(R.id.actionText)
    }

    class SystemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
    }
}
