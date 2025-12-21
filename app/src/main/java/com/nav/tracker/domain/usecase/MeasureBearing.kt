package com.nav.tracker.domain.usecase

import android.location.Location
import com.nav.tracker.data.NavBeacon

class MeasureBearing {
    operator fun invoke(loc: Location, beacon: NavBeacon): Float {
        val res = FloatArray(2)
        Location.distanceBetween(loc.latitude, loc.longitude, beacon.lat, beacon.lng, res)
        return res[1]
    }
}
