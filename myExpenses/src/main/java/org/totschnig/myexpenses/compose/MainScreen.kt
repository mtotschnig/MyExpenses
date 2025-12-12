package org.totschnig.myexpenses.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// 1. Define your data class for an account
data class Account(val name: String, val balance: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenPrototype() {
    // 2. Create your dummy list of accounts
    val accounts = listOf(
        Account("Checking", "$1,250.50"),
        Account("Savings", "$15,800.00"),
        Account("Credit Card", "-$450.20"),
        Account("Holiday Fund", "$800.00"),
        Account("Car Loan", "-$8,500.00"),
        Account("Mortgage", "-$150,000.00")
    )

    // 3. Set up the state for the Pager and a coroutine scope for animations
    val pagerState = rememberPagerState(pageCount = { accounts.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(accounts[pagerState.currentPage].balance) }, // App title is generic
                    actions = {
                        // 4. Global actions like Search or Menu go here
                        IconButton(onClick = { /* TODO: Handle search */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { /* TODO: Handle menu */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                    }
                )
                // 5. The ScrollableTabRow for account navigation
                SecondaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 0.dp
                ) {
                    accounts.forEachIndexed { index, account ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                // Animate to the page when a tab is clicked
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = account.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        // 6. The HorizontalPager that hosts the content for each account
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Use padding from the Scaffold
        ) { page ->
            // This is the content for each page.
            // Replace this with your actual account fragment/composable.
            AccountPageContent(account = accounts[page])
        }
    }
}

// 7. This is a placeholder for your actual account page UI
@Composable
fun AccountPageContent(account: Account) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // As recommended, the balance is now part of the page content
        Text(
            text = account.balance,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(top = 32.dp)
        )
        Text(
            text = "Transactions for ${account.name}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
        // Your list of transactions would go here
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainScreenPrototype()
}
