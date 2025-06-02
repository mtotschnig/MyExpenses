package org.totschnig.myexpenses.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.util.epochMillis2LocalDate
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
val DateRangePickerState.range: Pair<LocalDate, LocalDate>?
    get() = selectedStartDateMillis?.let { start ->
        selectedEndDateMillis?.let { end ->
            epochMillis2LocalDate(start, ZoneOffset.UTC) to
                    epochMillis2LocalDate(end, ZoneOffset.UTC)
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberDefaultDateRangePickerState(): DateRangePickerState {
    val today = LocalDate.now(ZoneOffset.UTC)
    return rememberDateRangePickerState(
        yearRange = (1970..today.year),
        selectableDates = object: SelectableDates {
            private val endOfToday: Long by lazy {
                ZonedDateTime.of(today, LocalTime.MAX, ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            }
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= endOfToday

            override fun isSelectableYear(year: Int) = year <= today.year
        },
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogFragment.DateRangePickerScaffold(
    confirmButtonText: String,
    state: DateRangePickerState = rememberDefaultDateRangePickerState(),
    warning: String? = null,
    enabled: Boolean? = null,
    doWork: suspend (Pair<LocalDate, LocalDate>) -> Unit,
) {
    Column {
        val dateFormatter = remember { DatePickerDefaults.dateFormatter() }
        var isWorking by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

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

        warning?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp),
                fontWeight = FontWeight.Bold
            )
        }

        ButtonRow(modifier = Modifier.padding(horizontal = 24.dp)) {
            if (isWorking) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
            } else {
                TextButton(onClick = { dismiss() }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
                TextButton(
                    enabled = enabled ?: (state.selectedStartDateMillis != null && state.selectedEndDateMillis != null),
                    onClick = {
                        isWorking = true
                        scope.launch(Dispatchers.IO) {
                            state.range?.let { doWork(it) }
                            dismiss()
                        }
                    }) {
                    Text(confirmButtonText)
                }
            }
        }
    }
}