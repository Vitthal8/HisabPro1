package com.example.data.model

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromJournalLines(lines: List<JournalLine>?): String {
        if (lines == null || lines.isEmpty()) return "[]"
        val array = JSONArray()
        for (line in lines) {
            val obj = JSONObject()
            obj.put("accountName", line.accountName)
            obj.put("debitAmount", line.debitAmount)
            obj.put("creditAmount", line.creditAmount)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toJournalLines(jsonStr: String?): List<JournalLine> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<JournalLine>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    JournalLine(
                        accountName = obj.optString("accountName", ""),
                        debitAmount = obj.optDouble("debitAmount", 0.0),
                        creditAmount = obj.optDouble("creditAmount", 0.0)
                    )
                )
            }
        } catch (_: Exception) {
            // fallback
        }
        return list
    }
}
