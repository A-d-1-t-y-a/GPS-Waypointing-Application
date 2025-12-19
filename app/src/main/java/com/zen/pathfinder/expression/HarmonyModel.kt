package com.zen.pathfinder.expression

import android.location.Location
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zen.pathfinder.mind.GuideStone
import com.zen.pathfinder.mind.StoneArchive
import com.zen.pathfinder.mind.ZenMath
import kotlinx.coroutines.launch

class HarmonyModel(private val rep: StoneArchive) : ViewModel() {
    
    var stones = mutableStateOf<List<GuideStone>>(emptyList())
        private set
    
    var focus = mutableStateOf<Int?>(null)
        private set
        
    var self = mutableStateOf<Location?>(null)
        private set
        
    var aura = mutableStateOf(500f) // Range
        private set

    init {
        viewModelScope.launch { stones.value = rep.recall() }
    }

    fun placeStone(l: Location) {
        viewModelScope.launch {
            val n = stones.value + GuideStone(l.latitude, l.longitude)
            stones.value = n
            rep.inscribe(n)
        }
    }

    fun presence(l: Location) {
        self.value = l
        val f = focus.value
        if (f != null && f > 0) {
            val s = stones.value.getOrNull(f)
            if (s != null && ZenMath.gap(l, s) < 10) {
                focus.value = f - 1 // Auto-step back
            }
        }
    }

    fun medidateOn(idx: Int) {
        focus.value = idx
    }

    fun clearMind() {
        viewModelScope.launch {
            stones.value = emptyList()
            focus.value = null
            rep.tabulaRasa()
        }
    }

    fun expandAura(z: Float) {
        aura.value = z.coerceIn(500f, 2000f)
    }
}
