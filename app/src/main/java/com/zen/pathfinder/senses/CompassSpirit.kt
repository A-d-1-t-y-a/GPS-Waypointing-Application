package com.zen.pathfinder.senses

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CompassSpirit(ctx: Context) {
    private val sen = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun spirit(): Flow<Float> = callbackFlow {
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent?) {
                e?.let {
                    if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        val m = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(m, it.values)
                        val o = FloatArray(3)
                        SensorManager.getOrientation(m, o)
                        val d = Math.toDegrees(o[0].toDouble()).toFloat()
                        trySend((d + 360) % 360)
                    }
                }
            }
            override fun onAccuracyChanged(s: Sensor?, i: Int) {}
        }
        sen.registerListener(l, sen.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_UI)
        awaitClose { sen.unregisterListener(l) }
    }
}
