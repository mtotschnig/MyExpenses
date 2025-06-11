package org.totschnig.myexpenses.di

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object LocalDateAdapter: JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    override fun serialize(
        localDate: LocalDate,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(localDate.toString())
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ) = LocalDate.parse(json.asJsonPrimitive.asString)
}

object LocalTimeAdapter: JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
    override fun serialize(
        localTime: LocalTime,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(localTime.truncatedTo(ChronoUnit.MINUTES).toString())
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ) = LocalTime.parse(json.asJsonPrimitive.asString)
}

object ZonedDateTimeAdapter: JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
    override fun serialize(
        zonedDateTime: ZonedDateTime,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(zonedDateTime.toString())
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ) = ZonedDateTime.parse(json.asJsonPrimitive.asString)
}