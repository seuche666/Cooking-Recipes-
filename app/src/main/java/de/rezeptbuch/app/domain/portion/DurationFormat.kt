package de.rezeptbuch.app.domain.portion

/** Formatiert Sekunden als „mm:ss“ bzw. „h:mm:ss“. */
fun formatTimer(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

/** „1 Std. 15 Min.“ / „45 Min.“ */
fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "$h Std. $m Min."
        h > 0 -> "$h Std."
        else -> "$m Min."
    }
}
