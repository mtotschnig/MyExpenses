package org.totschnig.myexpenses.model

import java.io.Serializable

interface IModel: Serializable {
    var id: Long
    var uuid: String?
}