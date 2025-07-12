package com.example.mykeyboardapp

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import android.widget.EditText
import android.widget.Button
import android.widget.LinearLayout
import android.view.Gravity
import android.widget.PopupWindow





import android.view.inputmethod.InputConnection

class MyKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var kv: KeyboardView
    private lateinit var keyboard: Keyboard
    private var isSymbol = false

    private var isCaps = false
    private var backspaceHandler: android.os.Handler? = null
    private var backspaceRunnable: Runnable? = null
    private var isBackspaceHeld = false

    override fun onCreateInputView(): View {
        kv = layoutInflater.inflate(R.layout.keyboard_view, null) as KeyboardView
        keyboard = Keyboard(this, R.xml.qwerty)
        kv.keyboard = keyboard
        kv.setOnKeyboardActionListener(this)
        kv.isPreviewEnabled = false
        return kv
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection
        val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(30, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }

        when (primaryCode) {
            -1 -> { // Shift key
                isCaps = !isCaps
                val layout = if (isCaps) R.xml.qwerty_shifted else R.xml.qwerty
                keyboard = Keyboard(this, layout)
                isSymbol = false
                kv.keyboard = keyboard
                kv.invalidateAllKeys()
            }
            android.util.Log.d("someKey", "This key was pressed$primaryCode")
            -102 -> { // 123 or ABC toggle
                android.util.Log.d("123", "123 button pressed")
                if (isSymbol) {
                    keyboard = Keyboard(this, R.xml.qwerty)
                    isCaps = false
                } else {
                    keyboard = Keyboard(this, R.xml.symbols)
                }
                isSymbol = !isSymbol
                kv.keyboard = keyboard
                kv.invalidateAllKeys()
            }
            -101 -> {
                showEmojiPopup()
            }
            -103 -> {
                android.util.Log.d("⚙", "SETTINGS button pressed")
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            -120 -> {
                android.util.Log.d("DRAFT_KEY", "Draft button pressed")
                showTopInputPopup()
            }




            Keyboard.KEYCODE_DELETE -> ic.deleteSurroundingText(1, 0)
            Keyboard.KEYCODE_DONE -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
            else -> {
                var code = primaryCode.toChar()
                if (isCaps && !isSymbol) code = code.uppercaseChar()
                ic.commitText(code.toString(), 1)
            }
        }
    }



    private fun showTopInputPopup() {
        android.util.Log.d("TEST_POPUP", "Popup function triggered")
        val popupView = layoutInflater.inflate(R.layout.top_input_popup, null)
        val editText = popupView.findViewById<EditText>(R.id.temp_input)
        val sendBtn = popupView.findViewById<Button>(R.id.send_temp)

        val popup = android.widget.PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        popup.isFocusable = true
        popup.showAtLocation(kv, android.view.Gravity.TOP, 0, 0)

        sendBtn.setOnClickListener {
            val msg = editText.text.toString()
            if (msg.isNotEmpty()) {
                currentInputConnection.commitText(msg, 1)
                popup.dismiss()
            }
        }
    }




    override fun onPress(primaryCode: Int) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            isBackspaceHeld = true
            val ic = currentInputConnection

            backspaceHandler = android.os.Handler(mainLooper)
            backspaceRunnable = object : Runnable {
                override fun run() {
                    if (isBackspaceHeld) {
                        ic.deleteSurroundingText(1, 0)
                        backspaceHandler?.postDelayed(this, 100) // delete every 100ms
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

    override fun onText(p0: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}

    private fun showEmojiPopup() {
        val popupView = layoutInflater.inflate(R.layout.emoji_popup, null)
        val emojiGrid = popupView.findViewById<android.widget.GridView>(R.id.emoji_grid)

        val emojiList = loadEmojisFromAssets()

        val adapter = EmojiAdapter(this, emojiList)
        emojiGrid.adapter = adapter

        val popup = android.widget.PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popup.isFocusable = true
        popup.showAtLocation(kv, android.view.Gravity.BOTTOM, 0, 0)

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
