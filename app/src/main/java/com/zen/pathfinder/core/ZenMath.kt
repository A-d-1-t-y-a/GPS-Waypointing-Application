package com.zen.pathfinder.core

import android.location.Location

object ZenMath {
    fun gap(l1: Location, s: GuideStone): Float {
        val r = FloatArray(1)
        Location.distanceBetween(l1.latitude, l1.longitude, s.y, s.x, r)
        return r[0]
    }

    fun vector(l1: Location, s: GuideStone): Float {
        val r = FloatArray(2)
        Location.distanceBetween(l1.latitude, l1.longitude, s.y, s.x, r)
        return r[1]
    }
}
