package org.totschnig.myexpenses.next.feature.merchants

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.next.core.ui.merchants.PayeeImage
import org.totschnig.myexpenses.next.data.merchants.MerchantLogoConsent
import org.totschnig.myexpenses.next.data.merchants.MerchantLogoStore
import org.totschnig.myexpenses.next.data.merchants.Merchants
import org.totschnig.myexpenses.next.data.merchants.PayeeDomainStore
import org.totschnig.myexpenses.next.data.merchants.PayeeIconStore
import org.totschnig.myexpenses.next.data.merchants.PayeeLook
import org.totschnig.myexpenses.next.data.merchants.RawPayee
import org.totschnig.myexpenses.next.data.merchants.domainOf
import org.totschnig.myexpenses.next.data.merchants.payeeLooks

/**
 * The logos and icons of the payees: icons the user chose, see [PayeeIconStore], logos of
 * [Merchants] once the user agreed, see [MerchantLogoConsent], and of the web addresses the user
 * entered for payees, see [PayeeDomainStore]. Each domain is loaded once, in the background, while
 * the list shows the monogram. Without consent the logos of the merchants are removed, those
 * entered stay.
 */
class MerchantLogos(
    /** The payees of the user, as stored */
    private val payees: Flow<List<RawPayee>>,
    private val consent: MerchantLogoConsent,
    private val domainStore: PayeeDomainStore,
    private val iconStore: PayeeIconStore,
    private val store: MerchantLogoStore,
) : ViewModel() {

    /** The web addresses the user entered, by payee id */
    val enteredDomains: StateFlow<Map<Long, String>> =
        domainStore.domains.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** The icons the user chose, by payee id */
    val chosenIcons: StateFlow<Map<Long, String>> =
        iconStore.values.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** What to show for each payee that has more than its monogram */
    private val looks: Flow<Map<Long, PayeeLook>> =
        combine(payees, domainStore.domains, iconStore.values, consent.isGiven, ::payeeLooks)
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    /** The logos loaded, by domain */
    private val byDomain = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())

    private val requested = HashSet<String>()

    /** The logo or icon of each payee that has one; a logo only once it is loaded */
    val images: StateFlow<Map<Long, PayeeImage>> =
        combine(looks, byDomain) { looks, byDomain ->
            looks.values.filterIsInstance<PayeeLook.Domain>().mapTo(HashSet()) { it.domain }.forEach(::request)
            buildMap {
                looks.forEach { (payeeId, look) ->
                    when (look) {
                        is PayeeLook.Icon -> put(payeeId, PayeeImage.Icon(look.name))
                        is PayeeLook.Domain -> byDomain[look.domain]?.let { put(payeeId, PayeeImage.Logo(it)) }
                    }
                }
            }
        }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            combine(consent.isGiven, domainStore.domains, ::Pair).collect { (isGiven, entered) ->
                if (!isGiven) {
                    val keep = entered.values.toSet()
                    requested.retainAll(keep)
                    byDomain.update { it.filterKeys(keep::contains) }
                    store.clear(keep)
                }
            }
        }
    }

    private fun request(domain: String) {
        if (!requested.add(domain)) return
        viewModelScope.launch {
            // Without logo the monogram stays, a failure is asked again on the next start
            decode(store.logo(domain))?.let { logo -> byDomain.update { it + (domain to logo) } }
        }
    }

    private suspend fun decode(bytes: ByteArray?) = bytes?.let {
        withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }

    /**
     * Loads the logo of the web address the user entered for [payeeId] and keeps the address if
     * there is one. Asks again also if the domain had no logo recently. Goes on when the dialog
     * asking is closed before.
     *
     * @param onResult gets the logo, null if the address is no domain or there is no logo for it
     */
    fun enterDomain(payeeId: Long, address: String, onResult: (ImageBitmap?) -> Unit) {
        viewModelScope.launch { onResult(loadEntered(payeeId, address)) }
    }

    private suspend fun loadEntered(payeeId: Long, address: String): ImageBitmap? {
        val domain = domainOf(address) ?: return null
        val logo = decode(store.logo(domain, retry = true)) ?: return null
        requested += domain
        byDomain.update { it + (domain to logo) }
        domainStore.set(payeeId, domain)
        iconStore.set(payeeId, null)
        return logo
    }

    /** Forgets the web address entered for [payeeId], its merchant or the monogram is shown again */
    fun removeDomain(payeeId: Long) {
        viewModelScope.launch { domainStore.set(payeeId, null) }
    }

    /** Shows [icon] for [payeeId] in place of its logo, null for the logo or monogram again */
    fun setIcon(payeeId: Long, icon: String?) {
        viewModelScope.launch {
            iconStore.set(payeeId, icon)
            if (icon != null) domainStore.set(payeeId, null)
        }
    }

    /** Forgets what the user chose for [payeeId], icon and web address */
    fun useAutomatic(payeeId: Long) {
        viewModelScope.launch {
            iconStore.set(payeeId, null)
            domainStore.set(payeeId, null)
        }
    }
}
