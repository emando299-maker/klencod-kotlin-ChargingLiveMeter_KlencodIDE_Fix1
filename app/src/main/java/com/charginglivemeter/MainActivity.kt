package com.charginglivemeter

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 32, 28, 32)
            setBackgroundColor(Color.rgb(5, 5, 14))
        }

        val title = TextView(this).apply {
            text = "CHARGING LIVE METER"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "خلفية شحن متحركة بتأثيرات نيون"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 30)
        }
        val note = TextView(this).apply {
            text = "تعرض الخلفية نسبة البطارية والقدرة الكهربائية المقدّرة من قراءات الهاتف المتاحة."
            textSize = 14f
            setTextColor(Color.rgb(190, 190, 205))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 28)
        }
        val button = Button(this).apply {
            text = "معاينة وتعيين الخلفية الحية"
            setOnClickListener { openWallpaperPicker() }
        }
        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(note)
        layout.addView(button)
        setContentView(layout)
    }

    private fun openWallpaperPicker() {
        val component = ComponentName(this, ChargingWallpaperService::class.java)
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }
}