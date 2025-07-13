package com.example.mykeyboardapp

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.PopupWindow
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp

class MyKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardManager: KeyboardManager
    private var isBackspaceHeld: Boolean = false
    private var backspaceHandler: Handler? = null
    private var backspaceRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        keyboardManager = KeyboardManager(this)
    }

    override fun onCreateInputView(): View {
        // Load a ComposeView from layout XML
        val composeView = layoutInflater.inflate(R.layout.keyboard_view, null) as ComposeView

        composeView.setContent {
            KeyboardUI { text ->
                currentInputConnection.commitText(text, 1)
            }
        }

        return composeView
    }

    @Composable
    fun KeyboardUI(onKeyPress: (String) -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("A", "B", "C", "D", "E").forEach { char ->
                    Button(onClick = { onKeyPress(char) }) {
                        Text(char)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { showEmojiPopup() }) {
                    Text("Emoji 😊")
                }
                Button(onClick = { showTopInputPopup() }) {
                    Text("Draft 📝")
                }
                Button(onClick = {
                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }) {
                    Text("Settings ⚙️")
                }
            }
        }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        // Not used in Compose UI version — handled via buttons
    }

    override fun onPress(primaryCode: Int) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            isBackspaceHeld = true
            val ic = currentInputConnection

            backspaceHandler = Handler(Looper.getMainLooper())
            backspaceRunnable = object : Runnable {
                override fun run() {
                    if (isBackspaceHeld) {
                        ic.deleteSurroundingText(1, 0)
                        backspaceHandler?.postDelayed(this, 100)
                    }
                }
            }
            backspaceHandler?.post(backspaceRunnable!!)
        }
    }

    override fun onRelease(primaryCode: Int) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            isBackspaceHeld = false
            backspaceHandler?.removeCallbacks(backspaceRunnable!!)
        }
    }

    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}

    private fun showTopInputPopup() {
        val popupView = layoutInflater.inflate(R.layout.top_input_popup, null)
        val editText = popupView.findViewById<EditText>(R.id.temp_input)
        val sendBtn = popupView.findViewById<Button>(R.id.send_temp)

        val popup = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popup.isFocusable = true
        popup.showAtLocation(window?.window?.decorView, Gravity.TOP, 0, 0)

        sendBtn.setOnClickListener {
            val msg = editText.text.toString()
            if (msg.isNotEmpty()) {
                currentInputConnection.commitText(msg, 1)
                popup.dismiss()
            }
        }
    }

    private fun showEmojiPopup() {
        val popupView = layoutInflater.inflate(R.layout.emoji_popup, null)
        val emojiGrid = popupView.findViewById<GridView>(R.id.emoji_grid)

        val emojiList = loadEmojisFromAssets()
        val adapter = EmojiAdapter(this, emojiList)
        emojiGrid.adapter = adapter

        val popup = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popup.isFocusable = true
        popup.showAtLocation(window?.window?.decorView, Gravity.BOTTOM, 0, 0)

        emojiGrid.setOnItemClickListener { _, _, position, _ ->
            val emoji = emojiList[position]
            currentInputConnection.commitText(emoji, 1)
            popup.dismiss()
        }
    }

    private fun loadEmojisFromAssets(): List<String> {
        return try {
            val inputStream = assets.open("emojis.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val json = String(buffer, Charsets.UTF_8)
            org.json.JSONArray(json).let { array ->
                List(array.length()) { i -> array.getString(i) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
