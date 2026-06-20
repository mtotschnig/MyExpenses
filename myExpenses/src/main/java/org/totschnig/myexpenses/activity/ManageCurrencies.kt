package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.currencies.EditCurrencyDialog
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CommodityType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.EditCurrencyViewModel

class ManageCurrencies : ProtectedFragmentActivity() {

    private val viewModel: EditCurrencyViewModel by viewModels()

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)

        setContent {
            AppTheme {

                var editingCurrency by rememberSaveable { mutableStateOf<CurrencyUnit?>(null) }
                var showEditDialog by rememberSaveable { mutableStateOf(false) }

                val openEditDialog = { unit: CurrencyUnit? ->
                    viewModel.resetResults()
                    editingCurrency = unit
                    showEditDialog = true
                }

                val tabs = CommodityType.entries
                val pagerState = rememberPagerState { tabs.size }
                val scope = rememberCoroutineScope()
                val units by viewModel.currencyUnits.collectAsStateWithLifecycle(emptyList())

                val deleteComplete by viewModel.deleteComplete.collectAsStateWithLifecycle()
                LaunchedEffect(deleteComplete) {
                    if (deleteComplete == false) {
                        showSnackBar(getString(R.string.currency_still_used))
                        viewModel.resetResults()
                    }
                }

                if (showEditDialog) {
                    EditCurrencyDialog(
                        currency = editingCurrency,
                        defaultType = tabs[pagerState.currentPage],
                        viewModel = viewModel,
                        onDismiss = { showEditDialog = false },
                        onResult = { result, code ->
                            if (result > 0) {
                                showSnackBar(getString(R.string.change_fraction_digits_result, result, code))
                            }
                        }
                    )
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            title = {
                                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                                    tabs.forEachIndexed { index, type ->
                                        Tab(
                                            selected = pagerState.currentPage == index,
                                            onClick = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(
                                                        index
                                                    )
                                                }
                                            },
                                            text = {
                                                Text(type.name.lowercase().replaceFirstChar {
                                                    if (it.isLowerCase()) it.titlecase(
                                                        LocalConfiguration.current.locales[0]
                                                    ) else it.toString()
                                                })
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { openEditDialog(null) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                ) { padding ->
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                    ) { page ->
                        val currentType = tabs[page]
                        val filteredUnits = units.filter { it.commodityType == currentType }

                        CurrencyListView(
                            units = filteredUnits,
                            onEdit = { openEditDialog(it) },
                            onDelete = { viewModel.deleteCurrency(it.databaseId) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CurrencyListView(
    units: List<CurrencyUnit>,
    onEdit: (CurrencyUnit) -> Unit,
    onDelete: (CurrencyUnit) -> Unit,
) {
    if (units.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(text = stringResource(R.string.no_data))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(units, key = { it.code }) { unit ->
                var showMenu by remember { mutableStateOf(false) }

                ListItem(
                    modifier = Modifier
                        .animateItem()
                        .clickable{ showMenu = true },
                    leadingContent = {
                        Text(
                            text = unit.code,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(32.dp)
                        )
                    },
                    headlineContent = {
                        Text(
                            unit.description + (unit.symbol.takeIf { it != unit.code }?.let {
                                " ($it)"
                            } ?: "")
                        )
                    },
                    supportingContent = {
                        Text("${stringResource(R.string.number_of_fraction_digits)}: ${unit.fractionDigits}")
                    },
                    trailingContent = {
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_edit)) },
                                onClick = {
                                    onEdit(unit)
                                    showMenu = false
                                }
                            )
                            if (!Utils.isKnownCurrency(unit.code)) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_delete)) },
                                    onClick = {
                                        onDelete(unit)
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}