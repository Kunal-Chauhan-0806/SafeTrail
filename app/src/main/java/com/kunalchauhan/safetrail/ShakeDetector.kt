package com.kunalchauhan.safetrail

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {
    private var spikeCount = 0
    private var firstSpikeTime = 0L
    private var lastSpikeTime = 0L

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val gX = event.values[0] / SensorManager.GRAVITY_EARTH
        val gY = event.values[1] / SensorManager.GRAVITY_EARTH
        val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)
        if (gForce < SPIKE_G) return

        val now = System.currentTimeMillis()
        if (now - lastSpikeTime < MIN_GAP_MS) return
        lastSpikeTime = now

        if (now - firstSpikeTime > WINDOW_MS) {
            firstSpikeTime = now
            spikeCount = 0
        }
        spikeCount++
        if (spikeCount >= SPIKES_NEEDED) {
            spikeCount = 0
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val SPIKE_G = 2.5f
        private const val MIN_GAP_MS = 100L
        private const val WINDOW_MS = 1500L
        private const val SPIKES_NEEDED = 3
    }
}
