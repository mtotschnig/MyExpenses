package org.totschnig.myexpenses.next.data.contracts

import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.totschnig.myexpenses.next.core.common.longs
import org.totschnig.myexpenses.next.core.common.parseJsonArray
import org.totschnig.myexpenses.next.core.common.toJsonArray

/*
 * How the decisions about contracts are stored as JSON in the data store, see [ContractStore].
 * What cannot be read, e.g. from a later version, is left out.
 */

internal fun parseNews(json: String): List<ContractNews> = parseJsonArray(json) {
    with(it) {
        ContractNews(
            id = getString("id"),
            type = ContractNews.Type.valueOf(getString("type")),
            ruleId = getString("rule"),
            transactionId = getLong("transaction"),
            date = LocalDate.parse(getString("date")),
            createdAt = LocalDate.parse(getString("created")),
            contractName = getString("name"),
            isIncome = getBoolean("income"),
            isReserve = optBoolean("reserve"),
            amount = getLong("amount"),
            previousAmount = if (has("previous")) getLong("previous") else null,
            cancelledOn = if (has("cancelled")) LocalDate.parse(getString("cancelled")) else null,
            interval = if (has("interval")) ContractInterval.valueOf(getString("interval")) else null,
            isRead = optBoolean("read")
        )
    }
}

internal fun serializeNews(news: List<ContractNews>) = news.toJsonArray { item ->
    JSONObject().apply {
        put("id", item.id)
        put("type", item.type.name)
        put("rule", item.ruleId)
        put("transaction", item.transactionId)
        put("date", item.date.toString())
        put("created", item.createdAt.toString())
        put("name", item.contractName)
        put("income", item.isIncome)
        put("reserve", item.isReserve)
        put("amount", item.amount)
        item.previousAmount?.let { put("previous", it) }
        item.cancelledOn?.let { put("cancelled", it.toString()) }
        item.interval?.let { put("interval", it.name) }
        put("read", item.isRead)
    }
}

internal fun parseRules(json: String): List<ContractRule> = parseJsonArray(json) {
    with(it) {
        ContractRule(
            id = getString("id"),
            kind = ContractRule.Kind.valueOf(getString("kind")),
            direction = ContractDirection.valueOf(getString("direction")),
            payeeIds = optJSONArray("payees")?.longs() ?: emptySet(),
            templateId = if (has("template")) getLong("template") else null,
            amountRange = if (has("min") && has("max")) getLong("min")..getLong("max") else null,
            interval = ContractInterval.valueOf(getString("interval")),
            name = if (has("name")) getString("name") else null,
            areaKey = if (has("area")) getString("area") else null,
            cancelledOn = if (has("cancelled")) LocalDate.parse(getString("cancelled")) else null,
            snapshot = if (has("snapshotName")) ContractRule.Snapshot(
                getString("snapshotName"),
                getLong("snapshotAmount"),
                LocalDate.parse(getString("snapshotDate"))
            ) else null,
            reserve = if (has("reserve")) getBoolean("reserve") else null,
            targetAccountIds = optJSONArray("targetAccounts")?.longs()
            // Stored as single account before rules could hold several
                ?: if (has("targetAccount")) setOf(getLong("targetAccount")) else emptySet(),
            icon = if (has("icon")) getString("icon") else null
        )
    }
}

internal fun serializeRules(rules: List<ContractRule>) = rules.toJsonArray { rule ->
    JSONObject().apply {
        put("id", rule.id)
        put("kind", rule.kind.name)
        put("direction", rule.direction.name)
        put("payees", JSONArray(rule.payeeIds.toList()))
        rule.templateId?.let { put("template", it) }
        rule.amountRange?.let {
            put("min", it.first)
            put("max", it.last)
        }
        put("interval", rule.interval.name)
        rule.name?.let { put("name", it) }
        rule.areaKey?.let { put("area", it) }
        rule.cancelledOn?.let { put("cancelled", it.toString()) }
        rule.snapshot?.let {
            put("snapshotName", it.name)
            put("snapshotAmount", it.lastAmount)
            put("snapshotDate", it.lastDate.toString())
        }
        rule.reserve?.let { put("reserve", it) }
        if (rule.targetAccountIds.isNotEmpty()) put("targetAccounts", JSONArray(rule.targetAccountIds.toList()))
        rule.icon?.let { put("icon", it) }
    }
}

internal fun parseMap(json: String): Map<String, String> = try {
    JSONObject(json).let { obj -> obj.keys().asSequence().associateWith { obj.getString(it) } }
} catch (_: JSONException) {
    emptyMap()
}

internal fun serializeMap(map: Map<String, String>) = JSONObject(map).toString()

internal fun parseCustomAreas(json: String): List<CustomArea> = parseJsonArray(json) {
    CustomArea(it.getString("id"), it.getString("name"))
}

internal fun serializeCustomAreas(areas: List<CustomArea>) =
    areas.toJsonArray { JSONObject(mapOf("id" to it.id, "name" to it.name)) }
