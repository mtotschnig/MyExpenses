package org.totschnig.myexpenses.next.core.model

import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import org.totschnig.myexpenses.next.core.model.ledger.Catalog
import org.totschnig.myexpenses.next.core.model.ledger.Ledger
import org.totschnig.myexpenses.next.core.model.ledger.LedgerSource

/** A [LedgerSource] whose values a test sets */
class FakeLedgerSource(
    consent: Boolean? = true,
    ledger: Ledger? = null,
    catalog: Catalog = Catalog(),
    today: LocalDate = LocalDate.now(),
) : LedgerSource {
    override val consent = MutableStateFlow(consent)
    override val ledger = MutableStateFlow(ledger)
    override val catalog = MutableStateFlow(catalog)
    override val today = MutableStateFlow(today)

    override fun setConsent(consent: Boolean) {
        this.consent.value = consent
    }
}
