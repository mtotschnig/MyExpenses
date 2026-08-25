package org.totschnig.myexpenses.compose.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import java.math.RoundingMode
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AmountEdit
import org.totschnig.myexpenses.compose.AmountText
import org.totschnig.myexpenses.compose.LocalCurrencyFormatter
import org.totschnig.myexpenses.compose.LocalDateFormatter
import org.totschnig.myexpenses.compose.TEST_TAG_DIALOG
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.compose.currencies.EditCurrencyDialog
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CommodityType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.util.calculateRealExchangeRate
import org.totschnig.myexpenses.util.toEpochMillis
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.FundingSource
import org.totschnig.myexpenses.viewmodel.data.Trade
import org.totschnig.myexpenses.viewmodel.data.TradeIntent
import org.totschnig.myexpenses.viewmodel.data.TradeType
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeScreen(
    onDismiss: () -> Unit,
    onSave: (TradeIntent, Boolean) -> Unit,
    portfolio: FullAccount,
    reportingCurrency: CurrencyUnit,
    assets: List<CurrencyUnit>,
    fundingAccounts: List<Pair<Long, String>>, // ID to Label
    targetPortfolios: List<Pair<Long, String>> = emptyList(), // ID to Label
    initialAction: Action? = null,
    initialTrade: Trade? = null,
    onCreateAsset: suspend (code: String, symbol: String, fractionDigits: Int, label: String?, commodityType: CommodityType) -> CurrencyUnit? = { _, _, _, _, _ -> null },
    isCurrencyUsed: suspend (String) -> Boolean = { false },
    onLookupMatchingTransactions: (accountId: Long, total: BigDecimal, date: LocalDateTime, isBuy: Boolean) -> Flow<List<Transaction2>> = { _, _, _, _ -> emptyFlow() },
    roundingMode: RoundingMode = RoundingMode.HALF_UP,
    onRoundingModeChange: (RoundingMode) -> Unit = {},
) {
    val isLarge = booleanResource(R.bool.isLarge)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = isLarge
        )
    ) {
        Surface(
            modifier = Modifier
                .testTag(TEST_TAG_DIALOG)
                .conditional(
                    isLarge,
                    ifTrue = {
                        widthIn(max = 560.dp)
                            .heightIn(max = 800.dp) // Limit maximum height
                            .fillMaxHeight(0.95f) // Take up to 95% of screen height
                    }, ifFalse = { fillMaxSize() }
                ),
            shape = if (isLarge) MaterialTheme.shapes.extraLarge else androidx.compose.ui.graphics.RectangleShape
        ) {
            val currencyFormatter = LocalCurrencyFormatter.current

            var selectedSubaccountId by rememberSaveable {
                mutableStateOf(initialTrade?.let { trade ->
                    portfolio.children.find { it.currencyUnit.code == trade.assetSymbol }?.id
                } ?: portfolio.children.find { it.isPortfolioAsset }?.id)
            }
            var selectedAsset by rememberSaveable {
                mutableStateOf(initialTrade?.let { trade ->
                    assets.find { it.code == trade.assetSymbol }
                        ?: portfolio.children.find { it.currencyUnit.code == trade.assetSymbol }?.currencyUnit
                } ?: portfolio.children.firstOrNull { !it.type.isCashAccount }?.currencyUnit)
            }

            var type by rememberSaveable {
                mutableStateOf(
                    initialTrade?.type ?: when (initialAction) {
                        Action.Sell -> TradeType.AssetTrade.SELL
                        Action.AssetTransfer -> TradeType.Transfer(false)
                        Action.Deposit -> TradeType.CashMovement.DEPOSIT
                        Action.Withdraw -> TradeType.CashMovement.WITHDRAW
                        else -> TradeType.AssetTrade.BUY
                    }
                )
            }
            val isAssetTrade = type is TradeType.AssetTrade || type is TradeType.Transfer

            var dateMillis by rememberSaveable {
                mutableLongStateOf(
                    (initialTrade?.date?.toEpochSecond()?.times(1000)) ?: Instant.now()
                        .toEpochMilli()
                )
            }

            val date = remember(dateMillis) {
                Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
            }

            var quantity by rememberSaveable {
                mutableStateOf(initialTrade?.quantity?.amountMajor)
            }
            var price by rememberSaveable {
                mutableStateOf(initialTrade?.price)
            }

            LaunchedEffect(selectedAsset) {
                if (initialTrade == null || selectedAsset?.code != initialTrade.assetSymbol) {
                    portfolio.children.find { it.currencyUnit.code == selectedAsset?.code }?.let { assetAccount ->
                        assetAccount.latestExchangeRate?.second?.let {
                            price = calculateRealExchangeRate(it, assetAccount.currencyUnit, reportingCurrency)
                        }
                    }
                }
            }
            var fee by rememberSaveable {
                mutableStateOf(initialTrade?.fee?.amountMajor)
            }

            var fundingSource by rememberSaveable {
                mutableStateOf(initialTrade?.let { trade ->
                    if (trade.peerAccount != null) {
                        if (fundingAccounts.any { it.first == trade.peerAccount.first }) FundingSource.ACCOUNT else FundingSource.PORTFOLIO
                    } else FundingSource.EXTERNAL
                } ?: FundingSource.EXTERNAL)
            }

            var peerAccountId by rememberSaveable {
                mutableStateOf(initialTrade?.peerAccount?.first)
            }

            var comment by rememberSaveable { mutableStateOf(initialTrade?.comment ?: "") }
            var linkedTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }
            var errorMessage by rememberSaveable { mutableStateOf<Int?>(null) }

            val coroutineScope = rememberCoroutineScope()

            val canSave = if (type is TradeType.Transfer) {
                selectedAsset != null && quantity != null && peerAccountId != null
            } else if (isAssetTrade) {
                selectedAsset != null && quantity != null
            } else {
                quantity != null
            }

            val isRoundingNecessary = remember(quantity, price, type, reportingCurrency) {
                val principalRaw =
                    if (type is TradeType.CashMovement) quantity.orZero else quantity.orZero.multiply(
                        price.orZero
                    )
                principalRaw.movePointRight(reportingCurrency.fractionDigits).stripTrailingZeros()
                    .scale() > 0
            }

            val principalAmount = remember(quantity, price, type, roundingMode) {
                val raw = if (type is TradeType.CashMovement) {
                    quantity.orZero
                } else {
                    quantity.orZero.multiply(price.orZero)
                }
                Money.buildWithMajor(reportingCurrency, raw, roundingMode).getOrNull()?.amountMajor
                    ?: raw
            }

            val totalAmount = remember(type, principalAmount, fee, roundingMode) {
                val raw = if (type.isIncoming) {
                    principalAmount.add(fee.orZero)
                } else {
                    principalAmount.subtract(fee.orZero)
                }
                Money.buildWithMajor(reportingCurrency, raw, roundingMode).getOrNull()?.amountMajor
                    ?: raw
            }

            var showRoundingMenu by remember { mutableStateOf(false) }

            val onSaveClick = { stayOpen: Boolean ->
                runCatching {
                    val asset = if (isAssetTrade) selectedAsset!! else reportingCurrency
                    val finalQuantity =
                        Money.buildWithMajor(asset, quantity.orZero, roundingMode).getOrThrow()
                    val finalPrice = if (isAssetTrade) price.orZero else BigDecimal.ONE
                    val finalPrincipal =
                        Money.buildWithMajor(reportingCurrency, principalAmount, roundingMode)
                            .getOrThrow()
                    val finalFee = if (type is TradeType.Transfer) Money(
                        reportingCurrency,
                        0
                    ) else Money.buildWithMajor(reportingCurrency, fee.orZero, roundingMode)
                        .getOrThrow()
                    TradeIntent(
                        type = type,
                        date = date,
                        targetAsset = asset,
                        quantity = finalQuantity,
                        price = finalPrice,
                        principal = finalPrincipal,
                        peerAccountId = peerAccountId,
                        fee = finalFee,
                        comment = comment,
                        fundingSource = fundingSource,
                        linkedTransactionId = linkedTransactionId,
                        tradeId = initialTrade?.id
                    )
                }.onSuccess { intent ->
                    onSave(intent, stayOpen)
                    if (stayOpen) {
                        quantity = null
                        price = null
                        fee = null
                        comment = ""
                        linkedTransactionId = null
                    }
                }.onFailure {
                    errorMessage = R.string.number_too_large
                }
            }

            val matchingTransactions by remember(
                peerAccountId,
                totalAmount,
                date,
                isAssetTrade,
                type
            ) {
                peerAccountId?.let {
                    onLookupMatchingTransactions(it, totalAmount, date, type.isIncoming)
                } ?: flowOf(emptyList())
            }.collectAsState(emptyList())

            LaunchedEffect(matchingTransactions) {
                if (matchingTransactions.none { it.id == linkedTransactionId }) {
                    linkedTransactionId =
                        if (matchingTransactions.size == 1) matchingTransactions.first().id else null
                }
            }

            var showAddAsset by rememberSaveable { mutableStateOf<Pair<String, CommodityType>?>(null) }

            showAddAsset?.let { (codeToEdit: String, typeToEdit: CommodityType) ->
                EditCurrencyDialog(
                    currency = null,
                    initialCode = codeToEdit,
                    defaultType = typeToEdit,
                    onDismiss = { showAddAsset = null },
                    onConfirm = { code, symbol, fractionDigits, label, commodityType, _ ->
                        coroutineScope.launch {
                            val newAsset =
                                onCreateAsset(code, symbol, fractionDigits, label, commodityType)
                            if (newAsset != null) {
                                selectedAsset = newAsset
                                selectedSubaccountId = null
                            }
                            showAddAsset = null
                        }
                    },
                    isCurrencyUsed = isCurrencyUsed,
                    allowedTypes = listOf(CommodityType.SECURITY, CommodityType.CRYPTO)
                )
            }

            var showDatePicker by rememberSaveable { mutableStateOf(false) }
            var showTimePicker by rememberSaveable { mutableStateOf(false) }

            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = dateMillis
            )
            val timePickerState = rememberTimePickerState(
                initialHour = date.hour,
                initialMinute = date.minute
            )

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                dateMillis =
                                    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
                                        .toLocalDateTime().withHour(date.hour)
                                        .withMinute(date.minute)
                                        .toEpochMillis()
                            }
                            showDatePicker = false
                        }) {
                            Text(stringResource(android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            if (showTimePicker) {
                Dialog(onDismissRequest = { showTimePicker = false }) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TimePicker(state = timePickerState)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showTimePicker = false }) {
                                    Text(stringResource(android.R.string.cancel))
                                }
                                TextButton(onClick = {
                                    dateMillis = date.withHour(timePickerState.hour)
                                        .withMinute(timePickerState.minute).toEpochMillis()
                                    showTimePicker = false
                                }) {
                                    Text(stringResource(android.R.string.ok))
                                }
                            }
                        }
                    }
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            val applicableTypes =
                                if (initialTrade == null) TradeType.entries else when (initialTrade.type) {
                                    is TradeType.AssetTrade -> listOf(
                                        TradeType.AssetTrade.BUY,
                                        TradeType.AssetTrade.SELL
                                    )

                                    is TradeType.CashMovement -> listOf(
                                        TradeType.CashMovement.DEPOSIT,
                                        TradeType.CashMovement.WITHDRAW
                                    )

                                    else -> listOf(TradeType.Transfer(true))
                                }
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                applicableTypes.forEachIndexed { index, tradeType ->
                                    SegmentedButton(
                                        enabled = applicableTypes.size > 1,
                                        selected = when (type) {
                                            is TradeType.Transfer -> tradeType is TradeType.Transfer
                                            else -> type == tradeType
                                        },
                                        onClick = {
                                            type = if (tradeType is TradeType.Transfer) {
                                                tradeType.copy(isIncoming = type.isIncoming)
                                            } else {
                                                tradeType
                                            }
                                            if (type is TradeType.CashMovement && fundingSource == FundingSource.PORTFOLIO) {
                                                fundingSource = FundingSource.EXTERNAL
                                            }
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = applicableTypes.size
                                        ),
                                        icon = {}
                                    ) {
                                        Text(
                                            stringResource(tradeType.label),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.menu_back)
                                )
                            }
                        },
                        actions = {}
                    )
                },
                bottomBar = {
                    Surface(tonalElevation = 2.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (initialTrade == null) {
                                OutlinedButton(
                                    onClick = { onSaveClick(true) },
                                    modifier = Modifier.weight(1f),
                                    enabled = canSave
                                ) {
                                    Text(stringResource(R.string.menu_save_and_new))
                                }
                            }
                            Button(
                                onClick = { onSaveClick(false) },
                                modifier = Modifier.weight(1f),
                                enabled = canSave
                            ) {
                                Text(stringResource(R.string.menu_save))
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Date and Time selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = LocalDateFormatter.current.format(date.toLocalDate()),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.date)) },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDatePicker = true },
                            enabled = false,
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                            )
                        )

                        OutlinedTextField(
                            value = date.toLocalTime().format(
                                DateTimeFormatter.ofLocalizedTime(
                                    FormatStyle.SHORT
                                )
                            ),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.time)) },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showTimePicker = true },
                            enabled = false,
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                            )
                        )
                    }

                    if (isAssetTrade) {
                        AssetSelector(
                            label = stringResource(R.string.trade_target_asset),
                            selectedAsset = selectedAsset,
                            subaccounts = portfolio.children,
                            assets = assets,
                            onSelectionChanged = { asset, accountId ->
                                selectedAsset = asset
                                selectedSubaccountId = accountId
                            },
                            onCreateAsset = { (code, type) ->
                                showAddAsset = code to type
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(if (isAssetTrade) R.string.trade_quantity else R.string.amount),
                                style = MaterialTheme.typography.labelMedium
                            )
                            AmountEdit(
                                value = quantity,
                                onValueChange = { quantity = it },
                                fractionDigits = (if (isAssetTrade) selectedAsset else reportingCurrency)?.fractionDigits
                                    ?: 2,
                                enabled = !isAssetTrade || selectedAsset != null
                            )
                            if (!type.isIncoming) {
                                val (available, currency) = if (isAssetTrade) {
                                    remember(selectedSubaccountId, portfolio.children) {
                                        (portfolio.children.find { it.id == selectedSubaccountId }?.currentBalance
                                            ?: 0L) + (
                                                initialTrade?.let { initial ->
                                                    initial.quantity.amountMinor.let {
                                                        if (initial.type.isIncoming) it.unaryMinus() else it
                                                    }
                                                } ?: 0L) to selectedAsset
                                    }
                                } else {
                                    remember(portfolio.children) {
                                        portfolio.children.find { it.type.isCashAccount }?.currentBalance to reportingCurrency
                                    }
                                }
                                currency?.let { currencyUnit ->
                                    val availableMajor = remember(available, currencyUnit) {
                                        BigDecimal.valueOf(available ?: 0L)
                                            .movePointLeft(currencyUnit.fractionDigits)
                                    }
                                    val isOverLimit = quantity?.let { it > availableMajor } == true
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.available) + ":",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        AmountText(
                                            amount = available ?: 0L,
                                            currency = currencyUnit,
                                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                            fontWeight = MaterialTheme.typography.labelSmall.fontWeight,
                                            color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        if (isAssetTrade) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.trade_price),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                AmountEdit(
                                    value = price,
                                    onValueChange = { price = it },
                                    fractionDigits = 10
                                )
                            }
                        }
                    }

                    // Principal Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.value),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                currencyFormatter.formatCurrency(
                                    principalAmount,
                                    reportingCurrency
                                ),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Box {
                                if (isRoundingNecessary) {
                                    IconButton(
                                        onClick = { showRoundingMenu = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = stringResource(R.string.rounding),
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = showRoundingMenu,
                                    onDismissRequest = { showRoundingMenu = false }
                                ) {
                                    Text(
                                        text = stringResource(R.string.rounding),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                    val applicableModes = remember {
                                        listOf(
                                            RoundingMode.UP,
                                            RoundingMode.DOWN,
                                            RoundingMode.HALF_UP,
                                            RoundingMode.HALF_DOWN,
                                            RoundingMode.HALF_EVEN
                                        )
                                    }
                                    applicableModes.forEach { mode ->
                                        val isSelected = mode == roundingMode
                                        DropdownMenuItem(
                                            modifier = Modifier.semantics { selected = isSelected },
                                            text = {
                                                Text(
                                                    when (mode) {
                                                        RoundingMode.UP -> "↑↑ (0.1 → 1)"
                                                        RoundingMode.DOWN -> "↓↓ (0.9 → 0)"
                                                        RoundingMode.HALF_UP -> "½↑ (0.5 → 1)"
                                                        RoundingMode.HALF_DOWN -> "½↓ (0.5 → 0)"
                                                        RoundingMode.HALF_EVEN -> "½⚖ (0.5 → 2n)"
                                                        else -> mode.name
                                                    }
                                                )
                                            },
                                            onClick = {
                                                onRoundingModeChange(mode)
                                                showRoundingMenu = false
                                            },
                                            trailingIcon = {
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Fee
                    if (type !is TradeType.Transfer) {
                        Column {
                            Text(
                                stringResource(R.string.trade_fee),
                                style = MaterialTheme.typography.labelMedium
                            )
                            AmountEdit(
                                value = fee,
                                onValueChange = { fee = it },
                                fractionDigits = reportingCurrency.fractionDigits
                            )
                        }
                    }

                    // Total Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.menu_aggregates),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            currencyFormatter.formatCurrency(totalAmount, reportingCurrency),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    if (type is TradeType.Transfer) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val clickLabel = stringResource(R.string.menu_invert_transfer)
                            IconButton(
                                modifier = Modifier.semantics {
                                    // Provide the interaction hint
                                    onClick(label = clickLabel, action = null)
                                },
                                onClick = {
                                    type =
                                        (type as TradeType.Transfer).copy(isIncoming = !type.isIncoming)
                                },
                            ) {
                                Icon(
                                    if (type.isIncoming) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = stringResource(
                                        if (type.isIncoming) R.string.transfer_from_account else R.string.transfer_to_account
                                    )
                                )
                            }
                            TargetPortfolioSelector(
                                label = stringResource(if (type.isIncoming) R.string.transfer_from_account else R.string.transfer_to_account),
                                selectedPortfolioId = peerAccountId,
                                portfolios = targetPortfolios,
                                onSelectionChanged = { peerAccountId = it },
                                modifier = Modifier.weight(1f),
                                enabled = initialTrade == null
                            )
                        }
                    } else {
                        // Funding Account Selection
                        FundingSourceSelector(
                            label = stringResource(R.string.trade_funding_source),
                            portfolio = portfolio,
                            selectedSource = fundingSource,
                            selectedAccountId = peerAccountId,
                            accounts = fundingAccounts,
                            onSourceSelected = { source, account ->
                                fundingSource = source
                                peerAccountId = account
                                linkedTransactionId = null
                            },
                            showPortfolio = isAssetTrade
                        )
                    }

                    if (fundingSource == FundingSource.ACCOUNT && matchingTransactions.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.trade_matching_transaction_found),
                                style = MaterialTheme.typography.labelMedium
                            )
                            matchingTransactions.forEach { transaction ->
                                FilterChip(
                                    selected = linkedTransactionId == transaction.id,
                                    onClick = {
                                        linkedTransactionId =
                                            if (linkedTransactionId == transaction.id) null else transaction.id
                                    },
                                    label = {
                                        Column {
                                            (transaction.party?.name
                                                ?: transaction.comment)?.let { Text(it) }
                                            Text(
                                                currencyFormatter.formatCurrency(
                                                    transaction.displayAmount.amountMajor,
                                                    reportingCurrency
                                                ),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    },
                                    leadingIcon = if (linkedTransactionId == transaction.id) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }

                    // Comment
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text(stringResource(R.string.notes)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            errorMessage?.let {
                AlertDialog(
                    onDismissRequest = { errorMessage = null },
                    confirmButton = {
                        TextButton(onClick = { errorMessage = null }) {
                            Text(stringResource(android.R.string.ok))
                        }
                    },
                    text = { Text(stringResource(it)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetSelector(
    label: String,
    selectedAsset: CurrencyUnit?,
    subaccounts: List<FullAccount>,
    assets: List<CurrencyUnit>,
    onSelectionChanged: (CurrencyUnit, Long?) -> Unit,
    onCreateAsset: (Pair<String, CommodityType>) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    var selectedTypes by rememberSaveable {
        mutableStateOf(setOf(CommodityType.SECURITY, CommodityType.CRYPTO))
    }

    val filteredSubaccounts = remember(selectedTypes, subaccounts) {
        subaccounts.filter { it.currencyUnit.commodityType in selectedTypes }
    }

    val filteredAssets = remember(selectedTypes, assets, subaccounts) {
        val subaccountCodes = subaccounts.map { it.currencyUnit.code }
        assets.filter { asset ->
            asset.commodityType in selectedTypes && asset.code !in subaccountCodes
        }.take(20)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedAsset?.description ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Type Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CommodityType.entries.filter { it != CommodityType.FIAT }.forEach { type ->
                    FilterChip(
                        modifier = Modifier.weight(1f, fill = false),
                        selected = type in selectedTypes,
                        onClick = {
                            selectedTypes = if (type in selectedTypes) {
                                if (selectedTypes.size > 1) {
                                    selectedTypes - type
                                } else {
                                    // If toggling off the only selected type, switch to the other types
                                    CommodityType.entries.filter { it != CommodityType.FIAT && it != type }
                                        .toSet()
                                }
                            } else {
                                selectedTypes + type
                            }
                        },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        leadingIcon = if (type in selectedTypes) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            if (filteredSubaccounts.isNotEmpty()) {
                filteredSubaccounts.forEach { account ->
                    val isSelected = account.currencyUnit.code == selectedAsset?.code
                    DropdownMenuItem(
                        modifier = Modifier.semantics { selected = isSelected },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${account.label} (${account.currencyUnit.code})")
                                AmountText(
                                    amount = account.currentBalance,
                                    currency = account.currencyUnit,
                                    currencySymbol = ""
                                )
                            }
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        onClick = {
                            onSelectionChanged(account.currencyUnit, account.id)
                            expanded = false
                        }
                    )
                }
            }

            if (filteredAssets.isNotEmpty()) {
                HorizontalDivider()
                filteredAssets.forEach { asset ->
                    val isSelected = asset.code == selectedAsset?.code
                    DropdownMenuItem(
                        modifier = Modifier.semantics { selected = isSelected },
                        text = { Text("${asset.description} (${asset.code})") },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        onClick = {
                            onSelectionChanged(asset, null)
                            expanded = false
                        }
                    )
                }
            }

            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_add)) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    onCreateAsset("" to (selectedTypes.firstOrNull() ?: CommodityType.SECURITY))
                    expanded = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetPortfolioSelector(
    label: String,
    selectedPortfolioId: Long?,
    portfolios: List<Pair<Long, String>>,
    onSelectionChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = portfolios.find { it.first == selectedPortfolioId }?.second ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedText, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { if (enabled) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            enabled = enabled
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            portfolios.forEach { portfolio ->
                val isSelected = selectedPortfolioId == portfolio.first
                DropdownMenuItem(
                    modifier = Modifier.semantics { selected = isSelected },
                    text = { Text(portfolio.second) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null,
                    onClick = {
                        onSelectionChanged(portfolio.first)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundingSourceSelector(
    label: String,
    portfolio: FullAccount,
    selectedSource: FundingSource,
    selectedAccountId: Long?,
    accounts: List<Pair<Long, String>>,
    onSourceSelected: (FundingSource, Long?) -> Unit,
    showPortfolio: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = when (selectedSource) {
        FundingSource.PORTFOLIO -> stringResource(R.string.trade_funding_portfolio) + " (${portfolio.label})"
        FundingSource.EXTERNAL -> stringResource(R.string.trade_funding_external_description)
        FundingSource.ACCOUNT -> accounts.find { it.first == selectedAccountId }?.second ?: ""
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedText, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // Group 1: This Portfolio
            if (showPortfolio) {
                val isSelected = selectedSource == FundingSource.PORTFOLIO
                DropdownMenuItem(
                    modifier = Modifier.semantics { selected = isSelected },
                    text = {
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stringResource(R.string.trade_funding_portfolio))
                                Text(portfolio.label, style = MaterialTheme.typography.labelSmall)
                            }
                            AmountText(
                                amount = portfolio.children.find { it.type.isCashAccount }?.currentBalance
                                    ?: 0L, currency = portfolio.currencyUnit
                            )
                        }
                    },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null,
                    onClick = { onSourceSelected(FundingSource.PORTFOLIO, null); expanded = false }
                )
            }
            // Group 2: External
            val isExternalSelected = selectedSource == FundingSource.EXTERNAL
            DropdownMenuItem(
                modifier = Modifier.semantics { selected = isExternalSelected },
                text = {
                    Text(
                        stringResource(R.string.trade_funding_external_description),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                leadingIcon = if (isExternalSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null,
                onClick = { onSourceSelected(FundingSource.EXTERNAL, null); expanded = false }
            )
            HorizontalDivider()
            // Group 3: Other Accounts
            accounts.forEach { account ->
                val isAccountSelected =
                    selectedSource == FundingSource.ACCOUNT && selectedAccountId == account.first
                DropdownMenuItem(
                    modifier = Modifier.semantics { selected = isAccountSelected },
                    text = { Text(account.second) },
                    leadingIcon = if (isAccountSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null,
                    onClick = {
                        onSourceSelected(FundingSource.ACCOUNT, account.first); expanded = false
                    }
                )
            }
        }
    }
}

val BigDecimal?.orZero: BigDecimal
    get() = this ?: BigDecimal.ZERO

@Preview
@Composable
fun TradeScreenPreview() {
    TradeScreen(
        onDismiss = {},
        onSave = { _, _ -> },
        portfolio = FullAccount(
            id = 1,
            label = "Portfolio",
            currencyUnit = CurrencyUnit.DebugInstance,
            type = AccountType.CASH
        ),
        reportingCurrency = CurrencyUnit("EUR", "€", 2),
        assets = listOf(
            CurrencyUnit("AAPL", "AAPL", 2),
            CurrencyUnit("BTC", "₿", 8)
        ),
        fundingAccounts = listOf(
            1L to "Cash",
            2L to "Bank Account"
        ),
        initialAction = Action.Buy
    )
}
