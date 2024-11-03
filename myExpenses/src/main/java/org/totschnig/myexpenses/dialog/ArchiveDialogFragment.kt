package org.totschnig.myexpenses.dialog

import android.os.Bundle
import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.archive
import org.totschnig.myexpenses.db2.canBeArchived
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.util.epochMillis2LocalDate
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

@Parcelize
data class ArchiveInfo(
    val count: Int,
    val hasNested: Boolean,
    val statuses: List<CrStatus>,
    val accountType: AccountType,
) : Parcelable {
    val canArchive: Boolean
        get() = count > 0 &&
                !hasNested &&
                (accountType == AccountType.CASH || statuses.filter { it != CrStatus.VOID }.size <= 1)
}

@OptIn(ExperimentalMaterial3Api::class)
class ArchiveDialogFragment : ComposeBaseDialogFragment2() {
    @Inject
    lateinit var repository: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)
    }

    override val fullScreenIfNotLarge = true

    val accountId: Long
        get() = requireArguments().getLong(KEY_ACCOUNTID)

    val DateRangePickerState.range: Pair<LocalDate, LocalDate>?
        get() = selectedStartDateMillis?.let { start ->
            selectedEndDateMillis?.let { end ->
                epochMillis2LocalDate(start, ZoneOffset.UTC) to
                        epochMillis2LocalDate(end, ZoneOffset.UTC)
            }
        }

    @Composable
    override fun BuildContent() {
        val state = rememberDateRangePickerState()

        var archiveInfo by remember { mutableStateOf<ArchiveInfo?>(null) }

        LaunchedEffect(state) {
            snapshotFlow { state.range }.collect {
                archiveInfo = null
                it?.let { range ->
                    archiveInfo = withContext(Dispatchers.IO) {
                        repository.canBeArchived(accountId, range)
                    }
                }
            }
        }
        Column {
            val dateFormatter = remember { DatePickerDefaults.dateFormatter() }
            DateRangePicker(
                modifier = Modifier.conditional(state.displayMode == DisplayMode.Picker) {
                    weight(1f)
                },
                state = state,
                dateFormatter = dateFormatter,
                title = {
                    DateRangePickerDefaults.DateRangePickerTitle(
                        displayMode = state.displayMode,
                        modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp)
                    )
                },
                headline = {
                    DateRangePickerDefaults.DateRangePickerHeadline(
                        selectedStartDateMillis = state.selectedStartDateMillis,
                        selectedEndDateMillis = state.selectedEndDateMillis,
                        displayMode = state.displayMode,
                        dateFormatter,
                        modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp)
                    )
                }
            )
            archiveInfo?.let { info ->

                Text(
                    text = when {
                        info.canArchive -> stringResource(R.string.archive_warning, info.count)
                        info.hasNested -> stringResource(R.string.warning_nested_archives)
                        info.count == 0 -> stringResource(R.string.warning_empty_archive)
                        info.statuses.size > 1 -> stringResource(
                            R.string.warning_archive_inconsistent_state,
                            info.statuses.joinToString {
                                getString(it.toStringRes())
                            })

                        else -> throw IllegalStateException()
                    },
                    modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            ButtonRow(modifier = Modifier.padding(horizontal = 24.dp)) {
                TextButton(onClick = { dismiss() }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
                TextButton(
                    enabled = archiveInfo?.canArchive == true,
                    onClick = {
                        state.range?.let { repository.archive(accountId, it) }
                        dismiss()
                    }) {
                    Text(
                        text = stringResource(id = R.string.action_archive)
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(account: FullAccount) = ArchiveDialogFragment().apply {
            arguments = Bundle().apply {
                putLong(KEY_ACCOUNTID, account.id)
                putString(KEY_LABEL, account.label)
            }
        }
    }
}