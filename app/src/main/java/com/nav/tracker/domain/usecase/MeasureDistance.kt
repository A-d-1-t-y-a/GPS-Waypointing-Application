package com.nav.tracker.domain.usecase

import android.location.Location
import com.nav.tracker.data.NavBeacon

class MeasureDistance {
    operator fun invoke(loc: Location, beacon: NavBeacon): Float {
        val res = FloatArray(1)
        Location.distanceBetween(loc.latitude, loc.longitude, beacon.lat, beacon.lng, res)
        return res[0]
    }
}
