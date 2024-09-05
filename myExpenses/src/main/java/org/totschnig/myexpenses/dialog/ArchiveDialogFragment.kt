package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.archive
import org.totschnig.myexpenses.db2.canBeArchived
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.util.epochMillis2LocalDate
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

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

        val archiveCount = remember { mutableStateOf<Int?>(null) }
        val hasNested = remember { mutableStateOf(false) }

        LaunchedEffect(state) {
            snapshotFlow { state.range }.collect {
                archiveCount.value = null
                it?.let { range ->
                    val canBeArchived = withContext(Dispatchers.IO) {
                        repository.canBeArchived(accountId, range)
                    }
                    archiveCount.value = canBeArchived.first
                    hasNested.value = canBeArchived.second > 0
                }
            }
        }
        Column {


            DateRangePicker(
                modifier = Modifier.conditional(state.displayMode == DisplayMode.Picker) {
                    weight(1f)
                },
                state = state,
                title = {
                    DateRangePickerDefaults.DateRangePickerTitle(
                        displayMode = state.displayMode,
                        modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp)
                    )
                }
            )
            archiveCount.value?.let {

                Text(
                    text = when {
                        hasNested.value -> "Nested archives are not supported."
                        it == 0 -> "No transactions exist in the selected date range."
                        else -> "All transactions ($it) in the selected date range will be archived."
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
                    enabled = !hasNested.value && archiveCount.value?.let { it > 0 } == true,
                    onClick = {
                        state.range?.let { repository.archive(accountId, it) }
                        dismiss()
                    }) {
                    Text(
                        text = stringResource(id = R.string.archive)
                    )
                }
            }
        }
    }

    override fun initBuilder(): AlertDialog.Builder {
        return super.initBuilder().setTitle(R.string.archive)
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