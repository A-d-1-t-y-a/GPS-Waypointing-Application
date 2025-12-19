package com.zen.pathfinder.mind

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class StoneArchive(private val ctx: Context) {
    private val doc = "zen_markers.json"

    suspend fun inscribe(stones: List<GuideStone>) = withContext(Dispatchers.IO) {
        try {
            val root = JSONArray()
            stones.forEach { s ->
                root.put(JSONObject().apply {
                    put("y", s.y)
                    put("x", s.x)
                    put("t", s.epoch)
                })
            }
            File(ctx.filesDir, doc).writeText(root.toString(2))
        } catch (ignored: Exception) {}
    }

    suspend fun recall(): List<GuideStone> = withContext(Dispatchers.IO) {
        return@withContext try {
            val f = File(ctx.filesDir, doc)
            if (!f.exists()) return@withContext emptyList()
            
            val arr = JSONArray(f.readText())
            val list = mutableListOf<GuideStone>()
            
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(GuideStone(
                    y = o.getDouble("y"),
                    x = o.getDouble("x"),
                    epoch = o.optLong("t", 0L)
                ))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun tabulaRasa() = withContext(Dispatchers.IO) {
        try {
            File(ctx.filesDir, doc).delete()
        } catch (ignored: Exception) {}
    }
}
