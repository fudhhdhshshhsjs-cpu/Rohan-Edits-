package com.example.util

import com.example.data.model.DateFormatOption
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

val ASIA_KOLKATA: ZoneId = ZoneId.of("Asia/Kolkata")
val DEFAULT_JOURNEY_START_DATE: LocalDate = LocalDate.of(2026, 7, 6)

interface DateProvider {
    fun today(): LocalDate
}

object SystemDateProvider : DateProvider {
    override fun today(): LocalDate = LocalDate.now(ASIA_KOLKATA)
}

object Formatters {
    private val indianLocale = Locale("en", "IN")

    fun formatCurrency(amount: Double, symbol: String = "₹", includeDecimals: Boolean = false): String {
        val formatter = NumberFormat.getNumberInstance(indianLocale).apply {
            minimumFractionDigits = if (includeDecimals && amount % 1.0 != 0.0) 2 else 0
            maximumFractionDigits = if (amount % 1.0 != 0.0) 2 else 0
        }
        val sign = if (amount < 0) "-" else ""
        val formatted = formatter.format(kotlin.math.abs(amount))
        return "$sign$symbol$formatted"
    }

    fun formatDate(
        epochDay: Long,
        option: DateFormatOption = DateFormatOption.DD_MMM_YYYY
    ): String = formatDate(LocalDate.ofEpochDay(epochDay), option)

    fun formatDate(
        date: LocalDate,
        option: DateFormatOption = DateFormatOption.DD_MMM_YYYY
    ): String {
        val pattern = when (option) {
            DateFormatOption.DD_MMM_YYYY -> "dd MMM yyyy"
            DateFormatOption.DD_MM_YYYY -> "dd/MM/yyyy"
            DateFormatOption.YYYY_MM_DD -> "yyyy-MM-dd"
        }
        return date.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
    }

    fun formatMonthYear(year: Int, month: Int): String {
        val date = LocalDate.of(year, month, 1)
        return date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
    }

    fun formatShortMonth(year: Int, month: Int): String {
        val date = LocalDate.of(year, month, 1)
        return date.format(DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH))
    }

    fun formatTimestamp(epochMillis: Long, option: DateFormatOption = DateFormatOption.DD_MMM_YYYY): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ASIA_KOLKATA).toLocalDate()
        return formatDate(date, option)
    }

    fun calculateExperience(startDate: LocalDate, today: LocalDate): ExperienceBreakdown {
        if (today.isBefore(startDate)) {
            val daysUntil = ChronoUnit.DAYS.between(today, startDate).toInt()
            return ExperienceBreakdown(
                totalDays = 0,
                months = 0,
                remainingDays = 0,
                years = 0,
                daysUntilStart = daysUntil
            )
        }
        val totalDays = ChronoUnit.DAYS.between(startDate, today).toInt()
        val period = Period.between(startDate, today)
        val totalMonths = period.years * 12 + period.months
        return ExperienceBreakdown(
            totalDays = totalDays,
            months = totalMonths,
            remainingDays = period.days,
            years = period.years,
            daysUntilStart = 0
        )
    }

    fun calculateConsistencyPercent(doneDays: Int, missedDays: Int): Double {
        val totalMarked = doneDays + missedDays
        if (totalMarked <= 0) return 0.0
        return (doneDays.toDouble() / totalMarked.toDouble()) * 100.0
    }

    fun formatPercent(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) {
            "${rounded.toInt()}%"
        } else {
            "$rounded%"
        }
    }
}

data class ExperienceBreakdown(
    val totalDays: Int,
    val months: Int,
    val remainingDays: Int,
    val years: Int,
    val daysUntilStart: Int
) {
    val summaryText: String
        get() = when {
            daysUntilStart > 0 -> "Starts in $daysUntilStart days"
            totalDays == 0 -> "Day 0 (Start Day)"
            months == 0 -> "$totalDays day${if (totalDays == 1) "" else "s"}"
            else -> "$months mo, $remainingDays d"
        }
}
