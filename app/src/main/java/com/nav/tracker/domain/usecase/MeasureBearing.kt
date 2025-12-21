package com.nav.tracker.domain.usecase

import android.location.Location
import com.nav.tracker.data.NavBeacon

/**
 * UseCase to measure bearing between user and target.
 */
class MeasureBearing {
    /**
     * Calculates bearing in degrees.
     * @param loc User location.
     * @param beacon Target beacon.
     * @return Bearing in degrees.
     */
    operator fun invoke(loc: Location, beacon: NavBeacon): Float {
        val res = FloatArray(2)
        Location.distanceBetween(loc.latitude, loc.longitude, beacon.lat, beacon.lng, res)
        return res[1]
    }
}
