package org.totschnig.myexpenses.sync.json

import kotlinx.serialization.Serializable


@Serializable
data class TagInfo(val label: String, val color: Int?)