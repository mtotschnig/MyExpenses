package org.totschnig.myexpenses.compose

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.viewmodel.data.ExtraIcons
import org.totschnig.myexpenses.viewmodel.data.IIconInfo
import org.totschnig.myexpenses.viewmodel.data.IconCategory
import org.totschnig.myexpenses.viewmodel.data.values
import timber.log.Timber

@Composable
fun IconSelector(
    modifier: Modifier = Modifier,
    iconsForCategory: (Context, IconCategory) -> Map<String, IIconInfo> = IIconInfo.Companion::resolveIconsForCategory,
    iconsForSearch: (Context, String) -> Map<String, IIconInfo> = IIconInfo.Companion::searchIcons,
    onIconSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val categories = IconCategory.values
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(1) }
    var searchTerm by rememberSaveable { mutableStateOf("") }
    val icons = remember {
        derivedStateOf {
            (if (selectedTabIndex > 0)
                iconsForCategory(context, categories[selectedTabIndex - 1])
            else
                if (searchTerm.isNotEmpty()) iconsForSearch(context, searchTerm) else emptyMap()
                    )
                .map { Triple(it.key, it.value, context.getString(it.value.label)) }
                .sortedBy { it.third }
        }
    }

    Column(modifier = modifier) {
        // Since Compose 1.7.3, on Oreo and below we receive an unwanted focus for the
        // Search tab, which overrides the default focus on the selected tab
        // We try to ignore this first focus change
        //TODO revisit if this is still necessary (instead of harmful) with future Compose versions
        var initialFocusProcessed by remember { mutableStateOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) }
        // Get safe drawing insets

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
/*            edgePadding = maxOf(
                TabRowDefaults.ScrollableTabRowEdgeStartPadding,
                safeDrawingInsets.calculateStartPadding(LocalLayoutDirection.current)
            )*/
        ) {
            Tab(
                modifier = Modifier.width(100.dp),
                selected = selectedTabIndex == 0,
                onClick = {
                    selectedTabIndex = 0
                }
            ) {
                TextField(
                    modifier = Modifier
                        .onFocusChanged {
                            if (it.isFocused) {
                                if (initialFocusProcessed) {
                                    selectedTabIndex = 0
                                } else {
                                    initialFocusProcessed = true
                                }
                            }
                        },
                    value = searchTerm,
                    onValueChange = { searchTerm = it },
                    placeholder = { Text("Search") },
                    maxLines = 1
                )
            }
            categories.forEachIndexed { tabIndex, category ->
                val effectiveIndex = tabIndex + 1
                CategoryTab(
                    category = category,
                    selected = selectedTabIndex == effectiveIndex
                ) {
                    selectedTabIndex = effectiveIndex
                }
            }
        }

        val minSize = with(LocalDensity.current) { 96.sp.toDp() }
        Timber.d("minSize: $minSize")
        LazyVerticalGrid(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            columns = GridCells.Adaptive(minSize = minSize)
        ) {
            for (icon in icons.value) {
                item {
                    Column(
                        modifier = Modifier
                            .clickable {
                                onIconSelected(icon.first)
                            }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon.second)
                        Text(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            text = icon.third,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryTab(
    category: IconCategory,
    focusRequester: FocusRequester = FocusRequester(),
    selected: Boolean,
    onclick: () -> Unit
) {
    Tab(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable(),
        selected = selected,
        onClick = onclick,
        text = { Text(text = stringResource(category.label)) }
    )
    LaunchedEffect(selected) {
        if (selected) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun IconSelectorDialog(
    showIconSelection: MutableState<Boolean>,
    icon: MutableState<String?>
) {
    if (showIconSelection.value) {
        Dialog(
            properties = DialogProperties(
                usePlatformDefaultWidth = booleanResource(R.bool.isLarge),
                decorFitsSystemWindows = false
            ),
            onDismissRequest = { showIconSelection.value = false }
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))) {
                    IconSelector(
                        modifier = Modifier.weight(1f),
                        onIconSelected = {
                            icon.value = it
                            showIconSelection.value = false
                        }
                    )

                    ButtonRow2(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                        positiveButton = if (icon.value != null)
                            ButtonDefinition(text = R.string.remove, enabled = true) {
                                icon.value = null
                                showIconSelection.value = false
                            } else null,
                        negativeButton = ButtonDefinition(
                            text = android.R.string.cancel,
                            enabled = true
                        ) {
                            showIconSelection.value = false
                        }
                    )
                }
            }
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    AppTheme {
        Surface {
            IconSelector(
                iconsForCategory = { _, _ ->
                    ExtraIcons
                },
                iconsForSearch = { _, _ ->
                    emptyMap()
                },
                onIconSelected = {}
            )
        }
    }
}