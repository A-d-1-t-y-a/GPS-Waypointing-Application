package com.orion.navigator.domain.usecase

import android.location.Location
import com.orion.navigator.data.NavBeacon

class ScanBeacons {
    operator fun invoke(list: List<NavBeacon>, center: Location, radius: Float): List<NavBeacon> {
        return list.filter {
            val res = FloatArray(1)
            Location.distanceBetween(center.latitude, center.longitude, it.lat, it.lng, res)
            res[0] <= radius
        }
    }
}
