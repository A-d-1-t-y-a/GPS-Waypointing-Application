package com.nav.tracker.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class GyroscopeMonitor(ctx: Context) {
    private val mgr = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun streamAzimuth(): Flow<Float> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                 event?.let {
                     if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                         val mat = FloatArray(9)
                         SensorManager.getRotationMatrixFromVector(mat, it.values)
                         val orient = FloatArray(3)
                         SensorManager.getOrientation(mat, orient)
                         // Azimuth is orient[0] in radians
                         val deg = Math.toDegrees(orient[0].toDouble()).toFloat()
                         // Normalize
                         val norm = (deg + 360) % 360
                         trySend(norm)
                     }
                 }
            }
            override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
        }
        
        mgr.registerListener(listener, mgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_UI)
        awaitClose { mgr.unregisterListener(listener) }
    }
}
