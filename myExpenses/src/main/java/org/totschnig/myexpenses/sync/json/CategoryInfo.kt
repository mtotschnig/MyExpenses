package org.totschnig.myexpenses.sync.json

data class CategoryInfo(val uuid: String, val label: String, val icon: String?, val color: Int?)

data class CategoryExport(val uuid: String, val label: String, val icon: String?, val color: Int?, val children: List<CategoryExport>)