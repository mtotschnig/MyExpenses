package org.totschnig.myexpenses.activity

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieEntry
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AmountText
import org.totschnig.myexpenses.compose.CheckableMenuEntry
import org.totschnig.myexpenses.compose.ColoredAmountText
import org.totschnig.myexpenses.compose.HierarchicalMenu
import org.totschnig.myexpenses.compose.LocalDateFormatter
import org.totschnig.myexpenses.compose.LocalHomeCurrency
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.PieChartCompose
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.compose.filter.ActionButton
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.epochMillis2LocalDate
import org.totschnig.myexpenses.util.toEpoch
import org.totschnig.myexpenses.viewmodel.data.BalanceAccount
import org.totschnig.myexpenses.viewmodel.data.BalanceAccount.Companion.partitionByAccountType
import org.totschnig.myexpenses.viewmodel.data.BalanceSection
import timber.log.Timber
import java.time.LocalDate
import java.util.Currency
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceSheetView(
    accounts: List<BalanceAccount>,
    debtSum: Long = 0L,
    date: LocalDate = LocalDate.now(),
    onClose: () -> Unit = {},
    onNavigate: (BalanceAccount) -> Unit = {},
    onSetDate: (LocalDate) -> Unit = {},
    onPrint: () -> Unit = {},
) {

    var showHidden by rememberSaveable { mutableStateOf(true) }
    var showZero by rememberSaveable { mutableStateOf(true) }
    var showChart by rememberSaveable { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val horizontalPadding = dimensionResource(R.dimen.padding_main_screen)

    val paddingValues =
        WindowInsets.navigationBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.End)
            .asPaddingValues()
    Column(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
    ) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        //Triple of asset or liability, position in section, index of account or null for Debts
        val highlight = remember { mutableStateOf<Triple<Boolean, Int, Long?>?>(null) }
        TopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            title = {
                Text(text = stringResource(R.string.balance_sheet))
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    androidx.compose.material3.Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close"
                    )
                }
            },
            scrollBehavior = scrollBehavior,
            actions = {
                val expanded = rememberSaveable { mutableStateOf(false) }
                IconButton(onClick = { expanded.value = true }) {
                    androidx.compose.material3.Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Options"
                    )
                }
                HierarchicalMenu(
                    expanded, Menu(
                        listOfNotNull(
                            if (accounts.any { !it.isVisible })
                                CheckableMenuEntry(
                                    label = R.string.show_invisible,
                                    command = "TOGGLE_SHOW_INVISIBLE",
                                    showHidden
                                ) {
                                    showHidden = !showHidden
                                } else null,
                            if (accounts.any { it.currentBalance == 0L })
                                CheckableMenuEntry(
                                    label = R.string.show_zero,
                                    command = "TOGGLE_SHOW_ZERO",
                                    showZero
                                ) {
                                    showZero = !showZero
                                } else null,
                            CheckableMenuEntry(
                                label = R.string.menu_chart,
                                command = "TOGGLE_CHART_BALANCE",
                                showChart
                            ) {
                                showChart = !showChart
                                highlight.value = null
                            },
                            MenuEntry(
                                icon = Icons.Filled.Print,
                                label = R.string.menu_print,
                                command = "PRINT_BALANCE"
                            ) {
                                onPrint()
                            }
                        )))
            },
            colors = TopAppBarDefaults.topAppBarColors()
        )

        Text(
            modifier = Modifier
                .padding(end = horizontalPadding)
                .clickable {
                    datePickerState.selectedDateMillis = date.toEpoch() * 1000
                    showDatePicker = true
                }
                .align(Alignment.End),
            text = LocalDateFormatter.current.format(date),
            textDecoration = TextDecoration.Underline
        )
        if (showDatePicker) {
            Popup(
                properties = PopupProperties(focusable = true),
                onDismissRequest = {
                    showDatePicker = false
                }
            ) {

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 64.dp)
                        .shadow(elevation = 4.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    val maxWidth = this.maxWidth
                    val isNarrow = maxWidth < 360.dp
                    if (isNarrow && datePickerState.displayMode == DisplayMode.Picker) {
                        Box(
                            modifier = Modifier.requiredSizeIn(
                                minWidth = 360.dp,
                                minHeight = 568.dp
                            )
                        ) {
                            DatePicker(
                                modifier = Modifier.scale(maxWidth / 360.dp),
                                state = datePickerState
                            )
                        }
                    } else {
                        DatePicker(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            state = datePickerState
                        )
                    }
                    ActionButton(
                        modifier = Modifier.align(Alignment.TopEnd),
                        hintText = stringResource(R.string.confirm),
                        icon = Icons.Filled.Done
                    ) {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let {
                            onSetDate(epochMillis2LocalDate(it))
                        }
                    }
                }
            }
        }

        val (assets, totalAssets, liabilities, totalLiabilities) = accounts.partitionByAccountType()

        val assetsWithDebts = totalAssets + (debtSum.takeIf { it > 0L } ?: 0L)
        val liabilitiesWithDebts = totalLiabilities + (debtSum.takeIf { it < 0L } ?: 0L)
        val ratio = if (assetsWithDebts > 0L && liabilitiesWithDebts < 0L)
            assetsWithDebts.toFloat() / -liabilitiesWithDebts else 1f
        val angles = if (ratio > 1f) 360f to 360f / ratio else
            360f * ratio to 360f
        LayoutHelper(
            showChart = showChart,
            chart = { modifier ->
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                ) {
                    RenderChart(
                        modifier = Modifier
                            .fillMaxSize(0.95f)
                            .align(Alignment.Center),
                        inner = false,
                        accounts = assets.flatMap { it.accounts },
                        debts = debtSum.takeIf { it > 0 },
                        highlight = highlight,
                        angle = angles.first,
                    )
                    RenderChart(
                        modifier = Modifier
                            .fillMaxSize(0.75f)
                            .align(Alignment.Center),
                        inner = true,
                        accounts = liabilities.flatMap { it.accounts },
                        debts = debtSum.takeIf { it < 0 },
                        highlight = highlight,
                        angle = angles.second,
                    )
                }
            },
            data = { modifier ->

                val listState = rememberLazyListState()
                fun lookup(id: Long?): Int {
                    var totalIndex = 0// Assets
                    assets.forEach { section ->
                        totalIndex++ // section header
                        section.accounts.forEach {
                            totalIndex++ //item
                            if (it.id == id) {
                                return totalIndex
                            }
                        }
                    }
                    totalIndex += 2 //Divider / Liabilities
                    liabilities.forEach { section ->
                        totalIndex++ // section header
                        section.accounts.forEach {
                            totalIndex++ //item
                            if (it.id == id) {
                                return totalIndex
                            }
                        }
                    }
                    totalIndex += 2 //Divider / Debts
                    return totalIndex
                }
                LazyColumn(
                    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding()
                    ),
                    state = listState
                ) {

                    accountTypeChapter(
                        title = R.string.balance_sheet_section_assets,
                        total = totalAssets,
                        sections = assets,
                        showHidden = showHidden,
                        showZero = showZero,
                        highlight = highlight.value?.third,
                        onNavigate = onNavigate
                    )
                    accountTypeChapter(
                        title = R.string.balance_sheet_section_liabilities,
                        total = totalLiabilities,
                        sections = liabilities,
                        showHidden = showHidden,
                        showZero = showZero,
                        highlight = highlight.value?.third,
                        onNavigate = onNavigate
                    )

                    if (debtSum != 0L) {
                        item {
                            BalanceSheetSectionHeaderView(
                                name = stringResource(R.string.debts),
                                total = debtSum,
                                highlight = highlight.value != null && highlight.value?.third == null,
                                absolute = false
                            )
                        }
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }

                    item {
                        BalanceSheetSectionHeaderView(
                            name = stringResource(R.string.balance_sheet_net_worth),
                            total = totalAssets + totalLiabilities + debtSum,
                            absolute = false
                        )
                    }
                }
                LaunchedEffect(highlight.value) {
                    highlight.value?.let {
                        lookup(it.third)
                    }?.let {
                        Timber.d("Scrolling to $it")
                        listState.animateScrollToItem(it)
                    }
                }
            }
        )
    }
}

fun LazyListScope.accountTypeChapter(
    @StringRes title: Int,
    total: Long,
    sections: List<BalanceSection>,
    showHidden: Boolean,
    showZero: Boolean,
    highlight: Long?,
    onNavigate: (BalanceAccount) -> Unit,
) {
    item {
        BalanceSheetSectionHeaderView(
            name = stringResource(title),
            total = total
        )
    }

    sections.forEach {
        accountTypeSection(
            section = it,
            showHidden = showHidden,
            showZero = showZero,
            highlight = highlight,
            onNavigate = onNavigate
        )
    }
    item {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun BalanceSheetSectionHeaderView(
    modifier: Modifier = Modifier,
    name: String,
    total: Long,
    highlight: Boolean = false,
    absolute: Boolean = true,
) {
    val homeCurrency = LocalHomeCurrency.current
    Row(
        modifier = modifier
            .conditional(highlight) {
                background(MaterialTheme.colorScheme.primaryContainer)
            }
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        ColoredAmountText(
            amount = total,
            currency = homeCurrency,
            absolute = absolute,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

fun LazyListScope.accountTypeSection(
    section: BalanceSection,
    showHidden: Boolean,
    showZero: Boolean,
    highlight: Long?,
    onNavigate: (BalanceAccount) -> Unit,
) {
    item {
        val homeCurrency = LocalHomeCurrency.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = section.type.localizedName(LocalContext.current),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            ColoredAmountText(
                amount = section.total,
                currency = homeCurrency,
                absolute = true,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
    section.accounts
        .filter { (showHidden || it.isVisible) && (showZero || it.currentBalance != 0L) }
        .forEach { account ->
            item {
                BalanceAccountItemView(account = account, highlight == account.id, onNavigate)
            }
        }
}

@Composable
fun BalanceAccountItemView(
    account: BalanceAccount,
    highlight: Boolean,
    onNavigate: (BalanceAccount) -> Unit,
) {
    val homeCurrency = LocalHomeCurrency.current
    Row(
        modifier = Modifier
            .conditional(highlight) {
                background(MaterialTheme.colorScheme.primaryContainer)
            }
            .clickable {
                onNavigate(account)
            }
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = account.label,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            ColoredAmountText(account.equivalentCurrentBalance, homeCurrency, absolute = true)
            if (account.currency.code != homeCurrency.code && account.currentBalance != 0L) {
                AmountText(
                    account.currentBalance.absoluteValue,
                    account.currency,
                    fontSize = 10.sp
                )
            }
        }
    }
}


@Composable
fun RenderChart(
    modifier: Modifier,
    inner: Boolean,
    accounts: List<BalanceAccount>,
    debts: Long?,
    highlight: MutableState<Triple<Boolean, Int, Long?>?>,
    angle: Float = 360f,
) {
    val context = LocalContext.current
    val accounts = accounts.filter { it.equivalentCurrentBalance != 0L }
    val pieEntries = accounts.map { account ->
        PieEntry(
            abs(account.equivalentCurrentBalance.toFloat()),
            account.label
        )
    }
    val colors = accounts.map { it.color }
    PieChartCompose(
        modifier = modifier,
        factory = { ctx ->
            PieChart(ctx)
        },
        holeRadius = if (inner) 75f else 85f,
        angle = angle,
        onValueSelected = { index ->
            highlight.value = index?.let { Triple(inner, index, accounts.getOrNull(index)?.id) }
        },
        data = if (debts == null)
            pieEntries else
            pieEntries + PieEntry(
                debts.toFloat().absoluteValue,
                stringResource(R.string.debts)
            ),
        colors = if (debts == null) colors else colors +
                ResourcesCompat.getColor(
                    LocalResources.current,
                    if (inner) R.color.colorExpense else R.color.colorIncome,
                    context.theme
                )
    ) {
        if (highlight.value?.first == !inner) {
            it.highlightValue(null)
            it.centerText = ""
        }
    }
}

@Composable
fun LayoutHelper(
    showChart: Boolean,
    chart: @Composable (Modifier) -> Unit,
    data: @Composable (Modifier) -> Unit,
) {
    if (showChart) {
        BoxWithConstraints {
            if (this.maxHeight > 600.dp || this.maxWidth < 600.dp) {
                Column {
                    chart(Modifier.weight(1f))
                    data(Modifier.weight(1f))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    chart(Modifier.weight(1f))
                    data(Modifier.weight(1f))
                }
            }
        }
    } else {
        data(Modifier)
    }
}

@Preview
@Composable
fun BalanceSheet() {
    BalanceSheetView(
        listOf(
            BalanceAccount(
                label = "Checking Account",
                type = AccountType.CASH,
                currentBalance = 500000, // $5,000.00 (assuming cents)
            ),
            BalanceAccount(
                label = "Savings Account",
                type = AccountType.BANK,
                currentBalance = 1250000, // $12,500.00
            ),
            BalanceAccount(
                label = "Credit Card 1",
                type = AccountType.CCARD,
                currentBalance = -25000, // -$250.00 (negative balance for debt)
            ),
            BalanceAccount(
                label = "Credit Card 2",
                type = AccountType.CCARD,
                currentBalance = -100000, // -$1,000.00
            ),
            BalanceAccount(
                label = "USD Cash",
                type = AccountType.CASH,
                currentBalance = 200000, // €2,000.00
                currency = CurrencyUnit(Currency.getInstance("USD")),
                equivalentCurrentBalance = (200000 * 0.92).roundToLong()
            ),
            BalanceAccount(
                label = "JPY Account",
                type = AccountType.BANK,
                currentBalance = 5000000, // ¥50,000.00
                currency = CurrencyUnit(Currency.getInstance("JPY")),
                equivalentCurrentBalance = (5000000 * 0.0075).roundToLong(),
            ),
            BalanceAccount(
                label = "USD Invest Account",
                type = AccountType.ASSET,
                currentBalance = 2000000, // $20,000
            ),
            BalanceAccount(
                label = "GBP Cash",
                type = AccountType.CASH,
                currentBalance = 100000, // £1,000.00
                currency = CurrencyUnit(Currency.getInstance("GBP")),
                equivalentCurrentBalance = (100000 * 1.28).roundToLong(),
            ),
            BalanceAccount(
                label = "Loan Account",
                type = AccountType.LIABILITY,
                currentBalance = -1000000, // -$10,000.00 (loan debt)
            ),
            BalanceAccount(
                label = "Petty Cash",
                type = AccountType.CASH,
                currentBalance = 10000, // $100.00 (loan debt)
            )
        )
    )
}

