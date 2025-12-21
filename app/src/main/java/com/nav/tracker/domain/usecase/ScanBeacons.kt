package com.nav.tracker.domain.usecase

import android.location.Location
import com.nav.tracker.data.NavBeacon

/**
 * UseCase to filter beacons within a certain range.
 */
class ScanBeacons {
    /**
     * Filters list of beacons by distance.
     * @param list Source list.
     * @param center Center location.
     * @param radius Radius in meters.
     * @return List of beacons within radius.
     */
    operator fun invoke(list: List<NavBeacon>, center: Location, radius: Float): List<NavBeacon> {
        return list.filter {
            val res = FloatArray(1)
            Location.distanceBetween(center.latitude, center.longitude, it.lat, it.lng, res)
            res[0] <= radius
        }
    }
}
