package org.totschnig.myexpenses.model

import android.net.Uri
import androidx.core.util.Pair
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import org.totschnig.myexpenses.model.Plan.Recurrence
import java.io.Serializable

interface ITransaction: Serializable {
    var status: Int
    var methodId: Long?
    var catId: Long?
    var categoryIcon: String?
    var label: String?
    var crStatus: Transaction.CrStatus
    var equivalentAmount: Money?
    var originalAmount: Money?
    var referenceNumber: String?
    var payee: String?
    var comment: String?
    var valueDate: Long
    var date: Long
    var originTemplateId: Long?
    var amount: Money
    var accountId: Long
    var parentId: Long?
    var id: Long
    var pictureUri: Uri?
    var originPlanInstanceId: Long?

    val isTransfer: Boolean
    val isSplit: Boolean

    fun setDate(zonedDateTime: ZonedDateTime)
    fun setValueDate(zonedDateTime: ZonedDateTime)
    fun setInitialPlan(initialPlan: Pair<Recurrence?, LocalDate?>)
    fun save(withCommit: Boolean): Uri?
}