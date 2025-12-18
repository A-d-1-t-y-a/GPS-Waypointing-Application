package com.example.gpswaypointing.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Provider for device physical orientation (Compass).
 */
class OrientationProvider(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /**
     * Stream of azimuth values in degrees.
     */
    fun orientationFlow(): Flow<Float> = callbackFlow {
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationSensor == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        val rMat = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rMat, it.values)
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rMat, orientation)
                        
                        // Azimuth is at index 0, convert to degrees
                        val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        // Invert for compass rotation use
                        trySend(-azimuth)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No-op
            }
        }

        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
