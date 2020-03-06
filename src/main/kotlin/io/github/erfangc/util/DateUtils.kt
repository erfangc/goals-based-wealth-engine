package io.github.erfangc.util

import java.time.LocalDate

object DateUtils {
    // determine the set of months between start -> stop by adding a month until stop
    fun months(start: LocalDate, stop: LocalDate): List<LocalDate> {
        var currentMonth = start
        val months = mutableListOf<LocalDate>()
        while (currentMonth.isBefore(stop)) {
            months.add(currentMonth)
            currentMonth = currentMonth.plusMonths(1)
        }
        return months
    }
}