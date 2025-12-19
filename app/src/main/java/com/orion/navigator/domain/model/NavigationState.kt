package com.orion.navigator.domain.model

import com.orion.navigator.data.NavBeacon

sealed class GuidanceStatus {
    object Standby : GuidanceStatus()
    data class Active(
        val target: NavBeacon,
        val range: Float,
        val azimuth: Float
    ) : GuidanceStatus()
}
