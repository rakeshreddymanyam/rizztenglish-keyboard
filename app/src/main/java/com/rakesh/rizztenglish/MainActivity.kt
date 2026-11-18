package com.rakesh.rizztenglish

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("rizz_prefs", Context.MODE_PRIVATE)
        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        val btnSaveKey = findViewById<Button>(R.id.btnSaveKey)
        val btnEnableIme = findViewById<Button>(R.id.btnEnableIme)
        val btnSelectIme = findViewById<Button>(R.id.btnSelectIme)

        etApiKey.setText(prefs.getString("gemini_api_key", ""))

        btnSaveKey.setOnClickListener {
            val key = etApiKey.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(this, "Please paste your Gemini API key first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString("gemini_api_key", key).apply()
            Toast.makeText(this, "API Key saved! Now enable RizzTenglish Keyboard.", Toast.LENGTH_SHORT).show()
        }

        btnEnableIme.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        btnSelectIme.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
    }
}
