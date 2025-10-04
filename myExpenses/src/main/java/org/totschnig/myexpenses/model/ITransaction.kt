package org.totschnig.myexpenses.model

import android.net.Uri
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.model.Plan.Recurrence
import org.totschnig.myexpenses.provider.PlannerUtils
import org.totschnig.myexpenses.ui.DisplayParty
import org.totschnig.myexpenses.viewmodel.data.Tag
import java.time.LocalDate
import java.time.ZonedDateTime

interface ITransaction: IModel {
    var status: Int
    var methodId: Long?
    val methodLabel: String?
    var catId: Long?
    var categoryIcon: String?
    var categoryPath: String?
    var crStatus: CrStatus
    var equivalentAmount: Money?
    var originalAmount: Money?
    var referenceNumber: String?
    var party: DisplayParty?
    var comment: String?
    var valueDate: Long
    var date: Long
    var originTemplateId: Long?
    var amount: Money
    var transferAmount: Money?
    var accountId: Long
    var parentId: Long?
    var originPlanInstanceId: Long?
    var originPlanId: Long?
    var debtId: Long?

    val isTransfer: Boolean
    val isSplit: Boolean

    fun setDate(zonedDateTime: ZonedDateTime)
    fun setValueDate(zonedDateTime: ZonedDateTime)
    fun setInitialPlan(initialPlan: Triple<String?, Recurrence, LocalDate>)
    fun save(repository: Repository, plannerUtils: PlannerUtils, withCommit: Boolean): Uri

    fun saveTags(repository: Repository, tags: List<Tag>)
}