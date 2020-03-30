package io.github.erfangc.common

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

    /**
     * Utility method that universally determines the most recent month end
     *
     * If data is not live, we can hard code the most recent date into this method
     */
    fun mostRecentMonthEnd(): LocalDate {
        val now = LocalDate.now()
        val lastMonth = now.minusMonths(1)
        return lastMonth.minusDays(lastMonth.dayOfMonth.toLong() - 1)
    }
}