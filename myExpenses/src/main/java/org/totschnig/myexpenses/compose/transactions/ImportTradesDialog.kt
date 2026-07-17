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
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var parsedIntents by remember { mutableStateOf<List<TradeIntent>?>(null) }

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
                val currentIntents = parsedIntents
                if (currentIntents == null) {
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
                            color = MaterialTheme.colorScheme.error
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
                                        val intents = parseCsv(
                                            context,
                                            uri,
                                            assets,
                                            fundingSource,
                                            fundingAccountId,
                                            dateFormat
                                        )
                                        if (intents.isNotEmpty()) {
                                            parsedIntents = intents
                                            statusMessage = null
                                        } else {
                                            statusMessage = "No valid trades found in file."
                                        }
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

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .height(300.dp)
                    ) {
                        items(currentIntents) { intent ->
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
                                onImport(currentIntents)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.menu_import))
                        }

                        OutlinedButton(
                            onClick = { parsedIntents = null },
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

private suspend fun parseCsv(
    context: android.content.Context,
    uri: Uri,
    assets: List<CurrencyUnit>,
    fundingSource: FundingSource,
    fundingAccountId: Long?,
    dateFormat: QifDateFormat,
): List<TradeIntent> = withContext(Dispatchers.IO) {
    val intents = mutableListOf<TradeIntent>()
    val assetMap = assets.associateBy { it.code }

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

                    val date = QifUtils.parseDate(dateStr, dateFormat)
                    val type = when (typeStr.uppercase()) {
                        "BUY", "B" -> TradeType.AssetTrade.BUY
                        "SELL", "S" -> TradeType.AssetTrade.SELL
                        else -> continue
                    }

                    val asset = assetMap[assetCode] ?: continue

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
                    // skip invalid row
                }
            }
        }
    } catch (e: Exception) {
        CrashHandler.report(e)
    }
    intents
}

private fun String.toBigDecimalOrNull(): BigDecimal? = try {
    BigDecimal(this)
} catch (_: Exception) {
    null
}
