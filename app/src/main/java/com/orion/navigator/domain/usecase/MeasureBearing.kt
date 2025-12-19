package com.orion.navigator.domain.usecase

import android.location.Location
import com.orion.navigator.data.NavBeacon

class MeasureBearing {
    operator fun invoke(loc: Location, beacon: NavBeacon): Float {
        val res = FloatArray(2)
        Location.distanceBetween(loc.latitude, loc.longitude, beacon.lat, beacon.lng, res)
        return res[1]
    }
}
