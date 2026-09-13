package com.dialy.app.core.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Utility functions for date manipulation and formatting.
 */
object DateUtils {
    private val ISO_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Converts a LocalDate to standard ISO-8601 string (yyyy-MM-dd).
     */
    fun toIsoString(date: LocalDate): String {
        return date.format(ISO_FORMATTER)
    }

    /**
     * Parses an ISO-8601 string (yyyy-MM-dd) to LocalDate.
     * Returns null if string is invalid.
     */
    fun parseIsoDate(dateString: String): LocalDate? {
        return try {
            LocalDate.parse(dateString, ISO_FORMATTER)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    /**
     * Returns today's LocalDate.
     */
    fun today(): LocalDate {
        return LocalDate.now()
    }
}
