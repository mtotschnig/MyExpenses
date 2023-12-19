package org.totschnig.myexpenses.compose

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import org.totschnig.myexpenses.viewmodel.data.FontAwesomeIcons
import org.totschnig.myexpenses.viewmodel.data.IIconInfo
import org.totschnig.myexpenses.viewmodel.data.IconCategory
import org.totschnig.myexpenses.viewmodel.data.values

@Composable
fun IconSelector(
    modifier: Modifier = Modifier,
    iconsForCategory: (Context, IconCategory) -> Map<String, IIconInfo> = IIconInfo.Companion::resolveIconsForCategory,
    iconsForSearch: (Context, String) -> Map<String, IIconInfo> = IIconInfo.Companion::searchIcons,
    onIconSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val categories = IconCategory.values
    var selectedTabIndex by rememberSaveable { mutableStateOf(1) }
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
    val localFocusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                modifier = Modifier.width(100.dp),
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 }) {
                TextField(
                    modifier = Modifier
                        .onFocusChanged {
                            if (it.isFocused) {
                                selectedTabIndex = 0
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
                Tab(selected = selectedTabIndex == effectiveIndex,
                    onClick = { selectedTabIndex = effectiveIndex; localFocusManager.clearFocus() },
                    text = { Text(text = stringResource(category.label)) }
                )
            }
        }
        LazyVerticalGrid(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            columns = GridCells.Fixed(3)
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

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun Preview() {
    Mdc3Theme {
        Surface {
            IconSelector(
                iconsForCategory = { _, _ ->
                    FontAwesomeIcons
                },
                iconsForSearch = { _, _ ->
                    emptyMap()
                },
                onIconSelected = {}
            )
        }
    }
}