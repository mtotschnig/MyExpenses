package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.createTransaction
import org.totschnig.myexpenses.db2.savePrice
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.ui.DisplayParty
import org.totschnig.myexpenses.viewmodel.data.*
import java.math.BigDecimal

class TradeViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val portfolioAccountId: Long,
    private val reportingCurrency: CurrencyUnit
) : ContentResolvingAndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TradeIntent(feeAsset = reportingCurrency))
    val uiState: StateFlow<TradeIntent> = _uiState.asStateFlow()

/*    val currencies: Flow<List<CurrencyUnit>> = repository.getCurrencies()
        .map { list -> list.map { currencyContext[it.code] } }

    val accounts: Flow<List<AccountMinimal>> = repository.getAccountsMinimal()*/

    // Reactive calculation: Principal = Quantity * Price
    val principal: StateFlow<BigDecimal> = _uiState.map {
        it.quantity.multiply(it.price)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), BigDecimal.ZERO)

    // Reactive calculation: Total Outlay = Principal + Fee
    val totalOutlay: StateFlow<BigDecimal> = combine(_uiState, principal) { state, princ ->
        if (state.type == TradeType.BUY) princ.add(state.fee) else princ.subtract(state.fee)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), BigDecimal.ZERO)

    fun updateIntent(transform: (TradeIntent) -> TradeIntent) {
        _uiState.update(transform)
    }

    /**
     * The Factory Method: Converts the "Trade" into a standard MyExpenses Split Transaction
     */
    fun save() {
        val intent = _uiState.value
        val tradeUuid = generateUuid()

        // 1. Create the Parent (Container) Transaction in the Portfolio
        // Amount is 0 because the valuation is driven by the splits
        val parent = TransactionEditData(
            accountId = portfolioAccountId,
            amount = Money(reportingCurrency, 0L),
            date = intent.date,
            comment = intent.comment,
            party = intent.payeeId?.let { DisplayParty(it, "") },
            uuid = tradeUuid,
            categoryId = org.totschnig.myexpenses.provider.SPLIT_CATID
        )

        // 2. Create the Split Parts
        val parts = mutableListOf<TransactionEditData>()

        // Part A: The Asset quantity (BUY = positive, SELL = negative)
        intent.targetAsset?.let { asset ->
            parts.add(TransactionEditData(
                accountId = portfolioAccountId,
                amount = Money.buildWithMajor(asset, intent.quantity.let {
                    if (intent.type == TradeType.SELL) it.negate() else it
                }).getOrThrow(),
                isSplitPart = true,
                uuid = tradeUuid
            ))
        }

        // Part B: The Funding (Transfer from/to Bank account)
        if (intent.type != TradeType.SWAP && intent.fundingAccountId != null) {
            parts.add(TransactionEditData(
                accountId = portfolioAccountId,
                amount = Money.buildWithMajor(reportingCurrency, principal.value.let {
                    if (intent.type == TradeType.BUY) it.negate() else it
                }).getOrThrow(),
                transferEditData = TransferEditData(transferAccountId = intent.fundingAccountId),
                isSplitPart = true,
                uuid = tradeUuid
            ))
        }

        // Part C: The Fee
        if (intent.fee > BigDecimal.ZERO) {
            parts.add(TransactionEditData(
                accountId = portfolioAccountId,
                amount = Money.buildWithMajor(intent.feeAsset ?: reportingCurrency, intent.fee.negate()).getOrThrow(),
                isSplitPart = true,
                uuid = tradeUuid
            ))
        }

        val finalTransaction = parent.copy(splitParts = parts)

        viewModelScope.launch {
            // repository.createTransaction handles the split list internally
            repository.createTransaction(
                org.totschnig.myexpenses.viewmodel.data.mapper.TransactionMapper.mapTransaction(finalTransaction)
            )

            // Also update the Price History table for valuation
            if (intent.targetAsset != null && intent.price > BigDecimal.ZERO) {
                repository.savePrice(
                    reportingCurrency,
                    intent.targetAsset,
                    intent.date.toLocalDate(),
                    org.totschnig.myexpenses.retrofit.ExchangeRateSource.User,
                    intent.price
                )
            }
        }
    }
}