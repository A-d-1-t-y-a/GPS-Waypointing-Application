package com.nav.tracker.domain.model

import com.nav.tracker.data.NavBeacon

/**
 * Represents the current status of the guidance system.
 */
sealed class GuidanceStatus {
    /** System is waiting for a target. */
    object Standby : GuidanceStatus()
    
    /**
     * System is active and tracking a target.
     * @property target The selected NavBeacon.
     * @property range Distance to target in meters.
     * @property azimuth Bearing to target in degrees.
     */
    data class Active(
        val target: NavBeacon,
        val range: Float,
        val azimuth: Float
    ) : GuidanceStatus()
}
