package com.rakesh.rizztenglish

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class RizzKeyboardService : InputMethodService() {

    private var isCaps = false
    private var langIndex = 0
    private var vibeIndex = 0

    private val langs = listOf("Only Telugu", "Pure English", "Tenglish Mix")
    private val vibes = listOf("Playful", "Cute", "Bold & Flirty", "Romantic")

    private lateinit var tvStatus: TextView
    private lateinit var hsvSuggestions: HorizontalScrollView
    private lateinit var llSuggestions: LinearLayout
    private val mainHandler = Handler(Looper.getMainLooper())
    private val letterButtons = mutableListOf<Button>()

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.keyboard_view, null)

        val btnLang = root.findViewById<Button>(R.id.btnLangToggle)
        val btnVibe = root.findViewById<Button>(R.id.btnVibeToggle)
        val btnReply = root.findViewById<Button>(R.id.btnReplyClipboard)
        val btnExpand = root.findViewById<Button>(R.id.btnExpandMsg)
        val btnFirst = root.findViewById<Button>(R.id.btnFirstChat)
        val llQwertyRows = root.findViewById<LinearLayout>(R.id.llQwertyRows)

        tvStatus = root.findViewById(R.id.tvRizzStatus)
        hsvSuggestions = root.findViewById(R.id.hsvSuggestions)
        llSuggestions = root.findViewById(R.id.llSuggestionsContainer)

        btnLang.setOnClickListener {
            langIndex = (langIndex + 1) % langs.size
            btnLang.text = langs[langIndex]
        }

        btnVibe.setOnClickListener {
            vibeIndex = (vibeIndex + 1) % vibes.size
            btnVibe.text = vibes[vibeIndex]
        }

        btnReply.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val copiedText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
            if (copiedText.isEmpty()) {
                tvStatus.text = "Copy a chat message in WhatsApp/Instagram first!"
            } else {
                generateRizz(mode = "reply", inputText = copiedText, replaceCurrentBox = false)
            }
        }

        btnExpand.setOnClickListener {
            val extracted = currentInputConnection?.getExtractedText(ExtractedTextRequest(), 0)
            val typedText = extracted?.text?.toString()?.trim() ?: ""
            if (typedText.isEmpty()) {
                tvStatus.text = "Type a short line first (e.g. 'Em chesthunnav?') then tap Expand!"
            } else {
                generateRizz(mode = "expand", inputText = typedText, replaceCurrentBox = true)
            }
        }

        btnFirst.setOnClickListener {
            generateRizz(mode = "first", inputText = "First message opener", replaceCurrentBox = false)
        }

        buildQwertyKeyboard(llQwertyRows)
        return root
    }

    private fun buildQwertyKeyboard(container: LinearLayout) {
        letterButtons.clear()
        val rows = listOf(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "DEL"),
            listOf("GLOBE", ",", "SPACE", ".", "ENTER")
        )

        val density = resources.displayMetrics.density
        val keyHeight = (44 * density).toInt()
        val margin = (2 * density).toInt()

        for (row in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (4 * density).toInt() }
            }

            for (key in row) {
                val weight = when (key) {
                    "SPACE" -> 4.2f
                    "SHIFT", "DEL", "ENTER" -> 1.5f
                    "GLOBE" -> 1.3f
                    else -> 1.0f
                }

                val btn = Button(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, keyHeight, weight).apply {
                        setMargins(margin, 0, margin, 0)
                    }
                    setPadding(0, 0, 0, 0)
                    isAllCaps = false
                    textSize = if (key.length > 1) 12f else 16f
                    setTextColor(Color.WHITE)
                    setBackgroundColor(
                        when (key) {
                            "ENTER" -> Color.parseColor("#E11D48")
                            "SHIFT", "DEL", "GLOBE" -> Color.parseColor("#1E293B")
                            else -> Color.parseColor("#0F172A")
                        }
                    )
                    text = when (key) {
                        "SHIFT" -> "⇧"
                        "DEL" -> "⌫"
                        "GLOBE" -> "🌐"
                        "SPACE" -> "RizzTenglish"
                        "ENTER" -> "Send"
                        else -> if (isCaps) key.uppercase() else key
                    }

                    setOnClickListener { handleKeyClick(key) }
                }

                if (key.length == 1 && key[0].isLetter()) {
                    letterButtons.add(btn)
                }
                rowLayout.addView(btn)
            }
            container.addView(rowLayout)
        }
    }

    private fun handleKeyClick(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "SHIFT" -> {
                isCaps = !isCaps
                for (b in letterButtons) {
                    val ch = b.text.toString()
                    b.text = if (isCaps) ch.uppercase() else ch.lowercase()
                }
            }
            "DEL" -> ic.deleteSurroundingText(1, 0)
            "SPACE" -> ic.commitText(" ", 1)
            "GLOBE" -> {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
            "ENTER" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            else -> {
                val out = if (isCaps) key.uppercase() else key
                ic.commitText(out, 1)
                if (isCaps) {
                    isCaps = false
                    for (b in letterButtons) b.text = b.text.toString().lowercase()
                }
            }
        }
    }

    private fun generateRizz(mode: String, inputText: String, replaceCurrentBox: Boolean) {
        val prefs = getSharedPreferences("rizz_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("gemini_api_key", "")?.trim() ?: ""
        if (apiKey.isEmpty()) {
            tvStatus.text = "Open RizzTenglish app & save your Gemini API key first!"
            return
        }

        val currentLang = langs[langIndex]
        val currentVibe = vibes[vibeIndex]
        tvStatus.text = "Generating $currentLang ($currentVibe)..."
        hsvSuggestions.visibility = View.GONE

        val langRule = when (currentLang) {
            "Only Telugu" -> "Write 100% in natural spoken Telugu using English letters ONLY. ZERO English words allowed!"
            "Pure English" -> "Write 100% in natural conversational English. ZERO Telugu words allowed!"
            else -> "Mix natural spoken Telugu (in English letters) with casual English words like young people text."
        }

        val taskPrompt = when (mode) {
            "expand" -> "Upgrade this boring message "$inputText" into 3 crazy romantic, flirty, charming 1-2 sentence texts that spark a long reply."
            "first" -> "Write 3 natural, smooth first-message openers to start a flirty/romantic chat (never boring 'Hi')."
            else -> "Incoming text from crush: "$inputText". Write 3 direct, natural human replies that match their exact message."
        }

        val fullPrompt = """
            $taskPrompt
            Desired Vibe: $currentVibe.
            $langRule
            Sound 100% like a real 23-year-old human texting on WhatsApp/Instagram, NEVER like AI or poetry.
            Return ONLY a valid JSON array of 3 strings, e.g. ["msg 1", "msg 2", "msg 3"].
        """.trimIndent()

        thread {
            try {
                val urlStr = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=${URLEncoder.encode(apiKey, "UTF-8")}"
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-goog-api-key", apiKey)
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val body = JSONObject().apply {
                    put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(
                            JSONObject().put("text", fullPrompt)
                        ))
                    ))
                    put("generationConfig", JSONObject().apply {
                        put("responseMimeType", "application/json")
                        put("temperature", 0.9)
                    })
                }

                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

                val code = conn.responseCode
                if (code !in 200..299) {
                    mainHandler.post { tvStatus.text = "API Error ($code): Check key/internet" }
                    return@thread
                }

                val respStr = conn.inputStream.bufferedReader().readText()
                val rawJson = JSONObject(respStr)
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val suggestionsArray = JSONArray(rawJson)
                val list = mutableListOf<String>()
                for (i in 0 until suggestionsArray.length()) {
                    list.add(suggestionsArray.getString(i))
                }

                mainHandler.post {
                    showSuggestionChips(list, replaceCurrentBox)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    tvStatus.text = "Error: ${e.localizedMessage ?: "Network failed"}"
                }
            }
        }
    }

    private fun showSuggestionChips(items: List<String>, replaceCurrentBox: Boolean) {
        llSuggestions.removeAllViews()
        if (items.isEmpty()) {
            tvStatus.text = "No suggestions returned. Try again."
            return
        }

        tvStatus.text = "Tap a message below to insert into chat:"
        hsvSuggestions.visibility = View.VISIBLE

        val density = resources.displayMetrics.density
        for (msg in items) {
            val chip = Button(this).apply {
                text = msg
                isAllCaps = false
                textSize = 12f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1E1B4B"))
                setPadding((14 * density).toInt(), (6 * density).toInt(), (14 * density).toInt(), (6 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { rightMargin = (8 * density).toInt() }

                setOnClickListener {
                    val ic = currentInputConnection ?: return@setOnClickListener
                    if (replaceCurrentBox) {
                        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
                        val len = extracted?.text?.length ?: 0
                        if (len > 0) {
                            ic.setSelection(0, len)
                        }
                    }
                    ic.commitText(msg, 1)
                }
            }
            llSuggestions.addView(chip)
        }
    }
}
