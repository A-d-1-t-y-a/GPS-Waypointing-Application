package com.nav.tracker.domain.model

import com.nav.tracker.data.NavBeacon

sealed class GuidanceStatus {
    object Standby : GuidanceStatus()
    data class Active(
        val target: NavBeacon,
        val range: Float,
        val azimuth: Float
    ) : GuidanceStatus()
}
