package com.nav.tracker.domain.usecase

import android.location.Location
import com.nav.tracker.data.NavBeacon

/**
 * UseCase to measure distance between location and beacon.
 */
class MeasureDistance {
    /**
     * Calculates distance in meters.
     * @param loc User location.
     * @param beacon Target beacon.
     * @return Distance in meters.
     */
    operator fun invoke(loc: Location, beacon: NavBeacon): Float {
        val res = FloatArray(1)
        Location.distanceBetween(loc.latitude, loc.longitude, beacon.lat, beacon.lng, res)
        return res[0]
    }
}
