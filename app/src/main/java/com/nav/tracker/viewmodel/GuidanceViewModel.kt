package com.nav.tracker.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import com.nav.tracker.data.NavBeacon
import com.nav.tracker.domain.model.GuidanceStatus
import com.nav.tracker.domain.usecase.MeasureBearing
import com.nav.tracker.domain.usecase.MeasureDistance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GuidanceViewModel : ViewModel() {
    private val distOp = MeasureDistance()
    private val bearOp = MeasureBearing()

    private val _status = MutableStateFlow<GuidanceStatus>(GuidanceStatus.Standby)
    val status: StateFlow<GuidanceStatus> = _status

    fun refresh(loc: Location, target: NavBeacon?) {
        if (target == null) {
            _status.value = GuidanceStatus.Standby
            return
        }

        val d = distOp(loc, target)
        val b = bearOp(loc, target)

        _status.value = GuidanceStatus.Active(target, d, b)
    }
}
