package llm.slop.spirals.utils

object TimeUtils {
    fun formatPeriod(seconds: Float): String {
        if (seconds <= 0f) return "0s"
        
        if (seconds >= 3600f) {
            val h = (seconds / 3600f).toInt()
            val m = ((seconds % 3600f) / 60f).toInt()
            val s = (seconds % 60f).toInt()
            return when {
                s > 0 -> "${h}h ${m}m ${s}s"
                m > 0 -> "${h}h ${m}m"
                else -> "${h}h"
            }
        } else if (seconds >= 60f) {
            val m = (seconds / 60f).toInt()
            val s = seconds % 60f
            return when {
                s >= 0.01f -> {
                    val sStr = if (s % 1f == 0f) "${s.toInt()}s" else "%.2fs".format(s)
                    "${m}m ${sStr}"
                }
                else -> "${m}m"
            }
        } else {
            return if (seconds % 1f == 0f) "${seconds.toInt()}s" else "%.2fs".format(seconds)
        }
    }

    fun parsePeriod(input: String): Float? {
        val clean = input.trim().lowercase()
        if (clean.isEmpty()) return null

        val hRegex = Regex("""(\d+(?:\.\d+)?)\s*h""")
        val mRegex = Regex("""(\d+(?:\.\d+)?)\s*m""")
        val sRegex = Regex("""(\d+(?:\.\d+)?)\s*s""")

        var hours = 0.0
        var minutes = 0.0
        var seconds = 0.0
        var matchedAny = false

        hRegex.find(clean)?.let {
            hours = it.groupValues[1].toDoubleOrNull() ?: 0.0
            matchedAny = true
        }
        mRegex.find(clean)?.let {
            minutes = it.groupValues[1].toDoubleOrNull() ?: 0.0
            matchedAny = true
        }
        sRegex.find(clean)?.let {
            seconds = it.groupValues[1].toDoubleOrNull() ?: 0.0
            matchedAny = true
        }

        val totalSecs = if (matchedAny) {
            (hours * 3600.0 + minutes * 60.0 + seconds).toFloat()
        } else {
            clean.toFloatOrNull()
        }

        if (totalSecs == null) return null
        return if (totalSecs >= 3600f) totalSecs.toInt().toFloat() else totalSecs
    }
}
