package com.matias.pomodoro.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

object PomodoroDateUtils {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)
    private val isoWeekFields: WeekFields = WeekFields.ISO

    fun todayDate(): String = LocalDate.now().format(dateFormatter)

    fun currentWeek(): String = weekFor(LocalDate.now())

    fun currentMonth(): String = YearMonth.now().format(monthFormatter)

    fun dateDaysAgo(days: Long): String = LocalDate.now().minusDays(days).format(dateFormatter)

    fun weekFor(date: LocalDate): String {
        val weekYear = date.get(isoWeekFields.weekBasedYear())
        val week = date.get(isoWeekFields.weekOfWeekBasedYear())
        return "%04d-W%02d".format(Locale.US, weekYear, week)
    }

    fun monthFor(date: LocalDate): String = YearMonth.from(date).format(monthFormatter)

    fun parseDate(date: String): LocalDate = LocalDate.parse(date, dateFormatter)
}
