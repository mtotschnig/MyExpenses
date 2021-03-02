package org.totschnig.myexpenses.room

import androidx.room.TypeConverter
import org.threeten.bp.LocalDate

class DateConverter {
    @TypeConverter
    fun fromString(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(value) }
    }

    @TypeConverter
    fun dateToString(date: LocalDate?): String {
        return date.toString()
    }
}
