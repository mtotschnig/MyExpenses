package org.totschnig.myexpenses.compose.transactions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.export.qif.QifUtils
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.epochMillis2LocalDateTime
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.FundingSource
import org.totschnig.myexpenses.viewmodel.data.TradeIntent
import org.totschnig.myexpenses.viewmodel.data.TradeType
import java.io.InputStreamReader
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportTradesDialog(
    onDismiss: () -> Unit,
    onImport: (List<TradeIntent>) -> Unit,
    portfolio: FullAccount,
    assets: List<CurrencyUnit>,
    fundingAccounts: List<Pair<Long, String>>,
) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fundingSource by remember { mutableStateOf(FundingSource.PORTFOLIO) }
    var fundingAccountId by remember { mutableStateOf<Long?>(null) }
    var dateFormat by remember { mutableStateOf(QifDateFormat.default) }
    var parseResult by remember { mutableStateOf<ImportTradesParseResult?>(null) }

    val statusMessages = listOfNotNull(
        parseResult?.intents?.takeIf { it.isNotEmpty() }?.size?.let {
            pluralStringResource(R.plurals.import_prices_success_message, it, it)
        },
        parseResult?.parseFailures?.takeIf { it > 0 }?.let {
            pluralStringResource(R.plurals.import_prices_failure_message, it, it)
        },
        parseResult?.missingAssets?.takeIf { it.isNotEmpty() }?.let { missing ->
            val count = missing.values.sum()
            val assets = missing.keys.joinToString(", ")
            pluralStringResource(R.plurals.import_trades_missing_assets_message, count, count, assets)
        },
        parseResult?.takeIf { it.intents.isEmpty() && it.parseFailures == 0 && it.missingAssets.isEmpty() }?.let {
            stringResource(R.string.no_data)
        }
    )

    val statusMessage = statusMessages.takeIf { it.isNotEmpty() }?.joinToString(" ")

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val currentResult = parseResult
                if (currentResult == null || currentResult.intents.isEmpty()) {
                    Text(
                        text = stringResource(R.string.menu_import),
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = stringResource(R.string.trade_import_format_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            selectedUri?.lastPathSegment
                                ?: stringResource(R.string.import_source_file)
                        )
                    }

                    Text(
                        text = stringResource(R.string.date_format),
                        style = MaterialTheme.typography.labelMedium
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        QifDateFormat.entries.forEachIndexed { index, format ->
                            SegmentedButton(
                                selected = dateFormat == format,
                                onClick = { dateFormat = format },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = QifDateFormat.entries.size
                                ),
                                icon = {}
                            ) {
                                Text(
                                    format.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    FundingSourceSelector(
                        label = stringResource(R.string.trade_funding_source),
                        portfolio = portfolio,
                        selectedSource = fundingSource,
                        selectedAccountId = fundingAccountId,
                        accounts = fundingAccounts,
                        onSourceSelected = { source, account ->
                            fundingSource = source
                            fundingAccountId = account
                        }
                    )

                    statusMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val uri = selectedUri
                                if (uri != null) {
                                    scope.launch {
                                        parseResult = parseCsv(
                                            context,
                                            uri,
                                            assets,
                                            fundingSource,
                                            fundingAccountId,
                                            dateFormat
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedUri != null
                        ) {
                            Text(stringResource(R.string.menu_parse))
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.preview),
                        style = MaterialTheme.typography.headlineSmall
                    )

                    statusMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .height(300.dp)
                    ) {
                        items(currentResult.intents) { intent ->
                            ListItem(
                                headlineContent = {
                                    Text("${intent.targetAsset.description} (${intent.targetAsset.code})")
                                },
                                supportingContent = {
                                    Text("${intent.date.toLocalDate()} - ${stringResource(intent.type.label)}")
                                },
                                trailingContent = {
                                    Text(intent.quantity.toString())
                                }
                            )
                            HorizontalDivider()
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onImport(currentResult.intents)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.menu_import))
                        }

                        OutlinedButton(
                            onClick = {
                                parseResult = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.menu_back))
                        }
                    }
                }
            }
        }
    }
}

data class ImportTradesParseResult(
    val intents: List<TradeIntent>,
    val parseFailures: Int,
    val missingAssets: Map<String, Int>
)

private suspend fun parseCsv(
    context: android.content.Context,
    uri: Uri,
    assets: List<CurrencyUnit>,
    fundingSource: FundingSource,
    fundingAccountId: Long?,
    dateFormat: QifDateFormat,
): ImportTradesParseResult = withContext(Dispatchers.IO) {
    val intents = mutableListOf<TradeIntent>()
    val assetMap = assets.associateBy { it.code }
    var parseFailures = 0
    val missingAssets = mutableMapOf<String, Int>()

    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = InputStreamReader(inputStream)
            val format = CSVFormat.DEFAULT.builder()
                .setIgnoreSurroundingSpaces(true)
                .setHeader("Date", "Type", "Asset", "Quantity", "Price", "Fee")
                .setSkipHeaderRecord(true)
                .get()
            val parser = CSVParser.parse(reader, format)

            for (record in parser) {
                try {
                    val dateStr = record.get("Date") ?: continue
                    val typeStr = record.get("Type") ?: continue
                    val assetCode = record.get("Asset") ?: continue

                    val asset = assetMap[assetCode]
                    if (asset == null) {
                        missingAssets[assetCode] = (missingAssets[assetCode] ?: 0) + 1
                        continue
                    }

                    val date = QifUtils.parseDate(dateStr, dateFormat)
                    val type = when (typeStr.uppercase()) {
                        "BUY", "B" -> TradeType.AssetTrade.BUY
                        "SELL", "S" -> TradeType.AssetTrade.SELL
                        else -> {
                            parseFailures++
                            continue
                        }
                    }

                    val quantity = record.get("Quantity")?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val price = record.get("Price")?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val fee = record.get("Fee")?.toBigDecimalOrNull() ?: BigDecimal.ZERO

                    intents.add(
                        TradeIntent(
                            targetAsset = asset,
                            type = type,
                            date = epochMillis2LocalDateTime(date.time),
                            quantity = quantity,
                            price = price,
                            fundingSource = fundingSource,
                            fundingAccountId = fundingAccountId,
                            fee = fee
                        )
                    )
                } catch (_: Exception) {
                    parseFailures++
                }
            }
        }
    } catch (e: Exception) {
        CrashHandler.report(e)
    }
    ImportTradesParseResult(intents, parseFailures, missingAssets)
}

private fun String.toBigDecimalOrNull(): BigDecimal? = try {
    BigDecimal(this)
} catch (_: Exception) {
    null
}
