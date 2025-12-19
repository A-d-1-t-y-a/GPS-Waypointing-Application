package com.vintagenav.explorer.service

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
class OrientationProvider(appCtx: Context) {
    private val senseMgr = appCtx.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /**
     * Stream of azimuth values in degrees.
     */
    fun sensorStream(): Flow<Float> = callbackFlow {
        val rotSensor = senseMgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotSensor == null) {
            close()
            return@callbackFlow
        }

        val senseListener = object : SensorEventListener {
            override fun onSensorChanged(evt: SensorEvent?) {
                evt?.let {
                    if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        val rMat = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rMat, it.values)
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rMat, orientation)
                        
                        // Azimuth is at index 0, convert to degrees
                        val headingDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        // Invert for compass rotation use
                        trySend(-headingDeg)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No-op
            }
        }

        senseMgr.registerListener(senseListener, rotSensor, SensorManager.SENSOR_DELAY_UI)
        
        awaitClose {
            senseMgr.unregisterListener(senseListener)
        }
    }
}
