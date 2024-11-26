package org.totschnig.myexpenses.model2

import org.totschnig.myexpenses.model.Grouping
import java.time.LocalDate

data class BudgetExport(
    val title: String,
    val description: String,
    val grouping: Grouping,
    val accountUuid: String,
    val currency: String,
    val start: String?,
    val end: String?,
    val isDefault: Boolean,
    val categoryFilter: List<String> = emptyList(),
    val partyFilter: List<String> = emptyList(),
    val methodFilter: List<String> = emptyList(),
    val statusFilter: List<String> = emptyList(),
    val tagFilter: List<String> = emptyList(),
    val accountFilter: List<String> = emptyList(),
    val allocations: List<BudgetAllocationExport>  = emptyList()
)

data class BudgetAllocationExport(
    val category: String,
    val year: Int,
    val second: Int,
    val budget: Long,
    val rolloverPrevious: Long,
    val rolloverNext: Long,
    val oneTime: Boolean
)