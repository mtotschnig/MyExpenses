package org.totschnig.myexpenses.room

import androidx.room.Entity
import org.threeten.bp.LocalDate

@Entity(tableName = "exchange_rates", primaryKeys = ["from_currency", "to_currency", "date", "source"])
data class ExchangeRate(val from_currency: String, val to_currency: String, val date: LocalDate, val rate: Float, val source: String)