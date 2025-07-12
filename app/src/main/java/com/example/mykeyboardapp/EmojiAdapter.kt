package com.example.mykeyboardapp


import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class EmojiAdapter(private val context: Context, private val emojis: List<String>) : BaseAdapter() {
    override fun getCount(): Int = emojis.size
    override fun getItem(position: Int): Any = emojis[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val emoji = emojis[position]
        val textView = TextView(context)
        textView.text = emoji
        textView.textSize = 24f
        textView.setPadding(10, 10, 10, 10)
        return textView
    }
}
