package org.totschnig.myexpenses.next.core.model.ledger

/**
 * What a screen of an analysis shows: its result only after the user agreed that transactions may
 * be analysed, see [LedgerSource.consent]
 */
sealed interface AnalysisState<out T> {
    data object Loading : AnalysisState<Nothing>

    /** The user has not been asked yet whether transactions may be analysed */
    data object AskConsent : AnalysisState<Nothing>

    data object Declined : AnalysisState<Nothing>

    data class Ready<out T>(val value: T) : AnalysisState<T>
}

val <T> AnalysisState<T>.valueOrNull: T?
    get() = (this as? AnalysisState.Ready)?.value

/**
 * @param value null while loading
 */
fun <T> analysisState(consent: Boolean?, value: T?): AnalysisState<T> = when {
    consent == null -> AnalysisState.AskConsent
    !consent -> AnalysisState.Declined
    value == null -> AnalysisState.Loading
    else -> AnalysisState.Ready(value)
}
