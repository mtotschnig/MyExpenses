package org.totschnig.myexpenses.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.viewmodel.data.FontAwesomeIcons
import org.totschnig.myexpenses.viewmodel.data.IIconInfo

@Composable
fun IconSelector(
    categories: Array<String>,
    labelForCategory: (String) -> String,
    iconsForCategory: (String) -> Map<String, IIconInfo>,
    iconsForSearch: (String) -> Map<String, IIconInfo>,
    onIconSelected: (Map.Entry<String, IIconInfo>) -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableStateOf(1) }
    var searchTerm by rememberSaveable { mutableStateOf("") }
    val icons = derivedStateOf {
        if (selectedTabIndex > 0)
            iconsForCategory(categories[selectedTabIndex - 1])
        else
            if (searchTerm.isNotEmpty()) iconsForSearch(searchTerm) else emptyMap()
    }
    val localFocusManager = LocalFocusManager.current
    Column {
        ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
            Tab(modifier = Modifier.width(100.dp), selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }) {
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
                    text = { Text(text = labelForCategory(category)) }
                )
            }
        }
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            columns = GridCells.Fixed(3)
        ) {
            for (icon in icons.value) {
                item {
                    Column(
                        modifier = Modifier.clickable {
                                onIconSelected(icon)
                        }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon.value)
                        Text(modifier = Modifier.horizontalScroll(rememberScrollState()), text = stringResource(id = icon.value.label), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview() {
    IconSelector(
        stringArrayResource(id = R.array.categories),
        labelForCategory = {
            "Accessibility"
        },
        iconsForCategory = {
            FontAwesomeIcons
        },
        iconsForSearch = {
           emptyMap()
        },
        onIconSelected = {}
    )
}