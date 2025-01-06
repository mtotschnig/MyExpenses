package org.totschnig.myexpenses.model2

import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.model.Grouping

@Serializable
data class BudgetExport(
    val title: String,
    val description: String,
    val grouping: Grouping,
    val accountUuid: String?,
    val currency: String,
    val start: String?,
    val end: String?,
    val isDefault: Boolean,
    val categoryFilter: List<CategoryPath>? = null,
    val partyFilter: List<String>? = null,
    val methodFilter: List<String>? = null,
    val statusFilter: List<String>? = null,
    val tagFilter: List<String>? = null,
    val accountFilter: List<String>? = null,
    val allocations: List<BudgetAllocationExport>  = emptyList()
)

@Serializable
data class BudgetAllocationExport(
    val category: CategoryPath?,
    val year: Int?,
    val second: Int?,
    val budget: Long,
    val rolloverPrevious: Long?,
    val rolloverNext: Long?,
    val oneTime: Boolean
)