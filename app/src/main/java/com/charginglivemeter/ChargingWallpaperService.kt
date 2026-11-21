package com.charginglivemeter

import android.app.WallpaperManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.content.Intent
import android.view.SurfaceHolder
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import java.util.Locale

class ChargingWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = ChargingEngine()

    inner class ChargingEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var level = 0
        private var charging = false
        private var watts: Double? = null
        private var voltageMv = -1
        private var phase = 0f

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val drawFrame = object : Runnable {
            override fun run() {
                draw()
                if (visible) handler.postDelayed(this, 70L)
            }
        }

        private val batteryReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                if (intent == null) return
                val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                level = (rawLevel * 100 / scale).coerceIn(0, 100)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                updatePower()
            }
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.removeCallbacks(drawFrame)
            if (visible) {
                registerBatteryReceiver()
                handler.post(drawFrame)
            } else {
                unregisterBatteryReceiver()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            draw()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(drawFrame)
            unregisterBatteryReceiver()
            super.onSurfaceDestroyed(holder)
        }

        private var receiverRegistered = false
        private fun registerBatteryReceiver() {
            if (receiverRegistered) return
            val filter = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val sticky = this@ChargingWallpaperService.registerReceiver(batteryReceiver, filter)
            receiverRegistered = true
            if (sticky != null) batteryReceiver.onReceive(this@ChargingWallpaperService, sticky)
        }

        private fun unregisterBatteryReceiver() {
            if (receiverRegistered) {
                try { this@ChargingWallpaperService.unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
                receiverRegistered = false
            }
        }

        private fun updatePower() {
            val manager = getSystemService(BATTERY_SERVICE) as BatteryManager
            val currentUa = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            watts = if (currentUa != Int.MIN_VALUE && currentUa != 0 && voltageMv > 0) {
                // Battery-side power estimate, not guaranteed USB input/charger wattage.
                (kotlin.math.abs(currentUa.toLong()).toDouble() * voltageMv.toDouble() * 1000.0) / 1_000_000_000_000.0
            } else null
        }

        private fun draw() {
            val holder: SurfaceHolder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) render(canvas)
            } catch (_: Exception) {
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
                }
            }
        }

        private fun render(c: Canvas) {
            val w = c.width.toFloat()
            val h = c.height.toFloat()
            val cx = w / 2f
            val cy = h * 0.43f
            val radius = min(w * 0.36f, h * 0.19f)
            c.drawColor(Color.rgb(2, 2, 8))

            // Soft colored aura.
            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(cx, cy, radius * 1.8f,
                intArrayOf(Color.argb(55, 30, 55, 255), Color.argb(24, 120, 20, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 0.58f, 1f), Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, radius * 1.8f, paint)
            paint.shader = null

            // Animated energy rings.
            for (i in 0..3) {
                val r = radius * (0.72f + i * 0.105f)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = if (i == 2) 5.5f else 2.2f
                paint.color = when (i % 3) {
                    0 -> Color.rgb(35, 115, 255)
                    1 -> Color.rgb(115, 45, 255)
                    else -> Color.rgb(95, 210, 255)
                }
                paint.alpha = if (i == 2) 230 else 145
                c.drawArc(cx-r, cy-r, cx+r, cy+r, phase * (if (i % 2 == 0) 1 else -1) + i * 24f,
                    285f, false, paint)
            }

            // Bright moving highlight.
            for (i in 0..5) {
                val angle = Math.toRadians((phase * 1.7f + i * 60f).toDouble())
                val rr = radius * 0.98f
                val x = cx + cos(angle).toFloat() * rr
                val y = cy + sin(angle).toFloat() * rr
                paint.style = Paint.Style.FILL
                paint.color = if (i % 2 == 0) Color.CYAN else Color.MAGENTA
                paint.alpha = 220
                c.drawCircle(x, y, if (i == 0) 7f else 3.5f, paint)
            }

            // Percentage.
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
            paint.color = Color.WHITE
            paint.textSize = radius * 0.62f
            c.drawText("$level", cx, cy + radius * 0.12f, paint)
            paint.textSize = radius * 0.23f
            c.drawText("%", cx + radius * 0.48f, cy + radius * 0.10f, paint)

            paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            paint.textSize = radius * 0.23f
            paint.color = Color.rgb(255, 205, 55)
            val powerText = if (charging && watts != null) String.format(Locale.US, "%.1f W", watts) else if (charging) "-- W" else "NOT CHARGING"
            c.drawText(powerText, cx, cy + radius * 0.48f, paint)
            paint.textSize = radius * 0.35f
            c.drawText(if (charging) "ϟ" else "•", cx, cy + radius * 0.83f, paint)

            // Vertical stream of electric light beneath the ring.
            val lineTop = cy + radius * 1.12f
            val lineBottom = h * 0.94f
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = radius * 0.075f
            paint.shader = android.graphics.LinearGradient(cx, lineTop, cx, lineBottom,
                intArrayOf(Color.TRANSPARENT, Color.rgb(30, 90, 255), Color.rgb(110, 35, 255), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP)
            c.drawLine(cx, lineTop, cx, lineBottom, paint)
            paint.shader = null

            // Tiny rising particles.
            paint.style = Paint.Style.FILL
            for (i in 0..16) {
                val yy = lineTop + ((i * 37f + phase * 2.2f) % (lineBottom - lineTop).coerceAtLeast(1f))
                val xx = cx + sin((i * 1.7f + phase / 22f).toDouble()).toFloat() * (radius * 0.10f)
                paint.color = if (i % 2 == 0) Color.CYAN else Color.rgb(145, 70, 255)
                paint.alpha = 80 + (i * 9 % 170)
                c.drawCircle(xx, yy, 1.2f + (i % 3), paint)
            }

            phase = (phase + 2.2f) % 360f
        }
    }
}
