package com.example.nkdsify.ui.utils

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlin.math.sqrt

class ShakeDetector : SensorEventListener {
    private var onShakeListener: (() -> Unit)? = null
    private var lastTime: Long = 0
    private var lastShakeTime: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private val shakeThreshold = 2000
    private val shakeTimeout = 1000

    fun setOnShakeListener(listener: () -> Unit) {
        this.onShakeListener = listener
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTime > 100) {
            val diffTime = currentTime - lastTime
            lastTime = currentTime

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val deltaX = x - lastX
            val deltaY = y - lastY
            val deltaZ = z - lastZ

            val speed = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / diffTime * 10000f

            if (speed > shakeThreshold) {
                if (currentTime - lastShakeTime > shakeTimeout) {
                    lastShakeTime = currentTime
                    onShakeListener?.invoke()
                }
            }

            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}