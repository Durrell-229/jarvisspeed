package com.jarvis.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    data class ChatItem(val role: String, val text: String, val actionExecuted: String? = null)

    private val items = mutableListOf<ChatItem>()

    companion object {
        const val TYPE_USER = 1
        const val TYPE_JARVIS = 2
        const val TYPE_ACTION = 3
    }

    fun addMessage(role: String, text: String, actionExecuted: String? = null) {
        if (actionExecuted != null) {
            val actionPos = items.size
            items.add(ChatItem("action", "", actionExecuted))
            notifyItemInserted(actionPos)
        }
        val pos = items.size
        items.add(ChatItem(role, text))
        notifyItemInserted(pos)
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
            else -> TYPE_JARVIS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
                UserViewHolder(view)
            }
            TYPE_ACTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
                ActionViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
                JarvisViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is UserViewHolder -> {
                holder.textView.text = "[VOUS] ${item.text}"
                holder.textView.setTextColor(0x9900D4FF.toInt())
                holder.textView.textSize = 12f
            }
            is ActionViewHolder -> {
                holder.textView.text = "⚡ ACTION: ${item.actionExecuted}"
                holder.textView.setTextColor(0xFFFF6B2B.toInt())
                holder.textView.textSize = 10f
            }
            is JarvisViewHolder -> {
                holder.textView.text = "> ${item.text}"
                holder.textView.setTextColor(0xFF00D4FF.toInt())
                holder.textView.textSize = 12f
            }
        }
    }

    override fun getItemCount() = items.size

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    class JarvisViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    class ActionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }
}
