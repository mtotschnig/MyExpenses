package org.totschnig.myexpenses.next.data.merchants

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONException
import org.json.JSONObject
import java.net.IDN

/**
 * A text per payee, kept as JSON object by id in the data store, which is part of the backup
 */
open class PayeeTextStore(private val dataStore: DataStore<Preferences>, private val key: Preferences.Key<String>) {

    /** Text by payee id */
    val values: Flow<Map<Long, String>> =
        dataStore.data.map { parse(it[key]) }.distinctUntilChanged()

    /** Sets the text of [payeeId], null removes it */
    suspend fun set(payeeId: Long, value: String?) {
        dataStore.edit {
            val values = parse(it[key]).toMutableMap()
            if (value == null) values.remove(payeeId) else values[payeeId] = value
            it[key] = JSONObject(values.mapKeys { (id, _) -> id.toString() }).toString()
        }
    }

    private fun parse(json: String?): Map<Long, String> = try {
        json?.let(::JSONObject)?.let { obj ->
            obj.keys().asSequence().mapNotNull { key -> key.toLongOrNull()?.let { it to obj.getString(key) } }.toMap()
        } ?: emptyMap()
    } catch (_: JSONException) {
        emptyMap()
    }
}

/**
 * Web addresses the user entered for payees to load their logo from, e.g. for a local shop that
 * is not in [Merchants]
 */
class PayeeDomainStore(dataStore: DataStore<Preferences>) :
    PayeeTextStore(dataStore, stringPreferencesKey("next_payee_domains")) {
    /** Domain by payee id */
    val domains: Flow<Map<Long, String>> get() = values
}

/**
 * Icons of the [org.totschnig.myexpenses.next.icons.IconCatalog] the user chose for payees, e.g. a
 * loaf for the baker around the corner
 */
class PayeeIconStore(dataStore: DataStore<Preferences>) :
    PayeeTextStore(dataStore, stringPreferencesKey("next_payee_icons"))

private val DOMAIN = Regex("[a-z0-9-]+(\\.[a-z0-9-]+)+")

/**
 * The domain of a web address as the user types it, e.g. "rewe.de" of "https://www.rewe.de/angebote",
 * null if it is none
 */
fun domainOf(address: String): String? {
    val host = address.trim().lowercase()
        .substringAfter("://")
        .substringBefore('/').substringBefore('?').substringBefore('#')
        .substringAfter('@').substringBefore(':')
        .removePrefix("www.")
        .trimEnd('.')
    val ascii = try {
        IDN.toASCII(host)
    } catch (_: IllegalArgumentException) {
        return null
    }
    return ascii.takeIf { it.matches(DOMAIN) }
}

/** What is shown for a payee in place of the first letter of its name */
sealed interface PayeeLook {
    /** An icon of the [org.totschnig.myexpenses.next.icons.IconCatalog] */
    data class Icon(val name: String) : PayeeLook

    /** The logo loaded from a domain */
    data class Domain(val domain: String) : PayeeLook
}

/**
 * What to show for each payee: what the user chose for it, an icon or a web address, else what
 * was chosen for the payee it belongs to, else for its merchant, see [Merchants.ofPayees], if
 * [withCatalog] the logo, else an icon, see [MerchantIcons]
 *
 * @param entered domains by payee id
 * @param icons chosen by payee id; a payee has either an icon or a domain, the icon wins
 */
fun payeeLooks(
    payees: List<RawPayee>,
    entered: Map<Long, String>,
    icons: Map<Long, String>,
    withCatalog: Boolean,
): Map<Long, PayeeLook> {
    val merchants = Merchants.ofPayees(payees)
    fun chosen(id: Long): PayeeLook? = icons[id]?.let(PayeeLook::Icon) ?: entered[id]?.let(PayeeLook::Domain)
    fun ofMerchant(merchant: Merchant): PayeeLook? =
        if (withCatalog) PayeeLook.Domain(merchant.domain) else MerchantIcons.of(merchant)?.let(PayeeLook::Icon)
    return buildMap {
        payees.forEach { payee ->
            (chosen(payee.id) ?: payee.parentId?.let(::chosen) ?: merchants[payee.id]?.let(::ofMerchant))
                ?.let { put(payee.id, it) }
        }
    }
}

/** The domains of [payeeLooks] without icons */
fun payeeDomains(payees: List<RawPayee>, entered: Map<Long, String>, withCatalog: Boolean): Map<Long, String> =
    payeeLooks(payees, entered, emptyMap(), withCatalog).mapNotNull { (id, look) -> (look as? PayeeLook.Domain)?.let { id to it.domain } }.toMap()
