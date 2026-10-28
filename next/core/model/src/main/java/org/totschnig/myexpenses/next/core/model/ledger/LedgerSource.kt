package org.totschnig.myexpenses.next.core.model.ledger

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * The [Ledger] with what it depends on, loaded once for all features of the new UI
 */
interface LedgerSource {
    /** Null as long as the user has not been asked whether transactions may be analysed */
    val consent: Flow<Boolean?>

    /** Null while loading or without consent */
    val ledger: Flow<Ledger?>

    /**
     * The current day, changes at midnight. Analyses take it from here instead of the clock, so
     * that it is part of their input and a result of yesterday is not taken for today.
     */
    val today: Flow<LocalDate>

    /** The categories and tags of the user, also without consent */
    val catalog: Flow<Catalog>

    fun setConsent(consent: Boolean)
}
