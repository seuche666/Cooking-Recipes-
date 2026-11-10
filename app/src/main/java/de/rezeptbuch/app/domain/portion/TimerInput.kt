package de.rezeptbuch.app.domain.portion

/** Wandelt die Timer-Eingabe „5“, „1,5“ oder „1:30“ (Minuten) in Sekunden um und zurück. */
object TimerInput {
    fun parse(input: String): Int? {
        val t = input.trim()
        if (t.isEmpty()) return null
        val seconds = if (':' in t) {
            val (m, s) = t.split(':', limit = 2)
            (m.toIntOrNull() ?: 0) * 60 + (s.toIntOrNull() ?: 0)
        } else {
            t.replace(',', '.').toDoubleOrNull()?.let { (it * 60).toInt() } ?: 0
        }
        return seconds.takeIf { it > 0 }
    }

    fun format(seconds: Int?): String = when {
        seconds == null || seconds <= 0 -> ""
        seconds % 60 == 0 -> (seconds / 60).toString()
        else -> "%d:%02d".format(seconds / 60, seconds % 60)
    }
}
