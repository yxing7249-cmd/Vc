package com.nexus.win12desktop.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nexus.win12desktop.NexusApplication
import com.nexus.win12desktop.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = NexusApplication.getPrefs(this)

        val mouseSwitch = findViewById<Switch>(R.id.switch_mouse_enabled)
        mouseSwitch.isChecked = prefs.getBoolean("mouse_enabled", true)
        mouseSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("mouse_enabled", isChecked).apply()
            Toast.makeText(this, if (isChecked) "\u865a\u62df\u9f20\u6807\u5df2\u5f00\u542f" else "\u865a\u62df\u9f20\u6807\u5df2\u5173\u95ed", Toast.LENGTH_SHORT).show()
        }

        val speedSeek = findViewById<SeekBar>(R.id.seek_mouse_speed)
        val speedValue = findViewById<TextView>(R.id.text_speed_value)
        val currentSpeed = prefs.getFloat("mouse_speed", 12f)
        speedSeek.progress = ((currentSpeed - 4f) / 2f).toInt().coerceIn(0, 20)
        speedValue.text = currentSpeed.toInt().toString()
        speedSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = 4f + progress * 2f
                speedValue.text = speed.toInt().toString()
                prefs.edit().putFloat("mouse_speed", speed).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val backSwitch = findViewById<Switch>(R.id.switch_back_right_click)
        backSwitch.isChecked = prefs.getBoolean("back_as_right_click", true)
        backSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("back_as_right_click", isChecked).apply()
        }

        findViewById<TextView>(R.id.btn_set_default_launcher).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                Toast.makeText(this, "\u8bf7\u9009\u62e9 NEXUS WIN12 DESKTOP \u4f5c\u4e3a\u9ed8\u8ba4\u4e3b\u5c4f\u5e55", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "\u65e0\u6cd5\u6253\u5f00\u4e3b\u5c4f\u5e55\u8bbe\u7f6e", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<TextView>(R.id.btn_accessibility).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(this, "\u8bf7\u542f\u7528\u300cNEXUS \u9f20\u6807\u8f85\u52a9\u300d", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "\u65e0\u6cd5\u6253\u5f00\u8f85\u52a9\u529f\u80fd\u8bbe\u7f6e", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<TextView>(R.id.btn_back).setOnClickListener { finish() }
    }
}
