package org.totschnig.myexpenses.model

import android.content.ContentResolver
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE
import org.totschnig.myexpenses.util.ExchangeRateHandler
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.viewmodel.PlanInstanceInfo

fun planCount(contentResolver: ContentResolver): Int = contentResolver.query(
    Template.CONTENT_URI,
    arrayOf("count(*)"),
    "${DatabaseConstants.KEY_PLANID} is not null",
    null,
    null
)?.use {
    if (it.moveToFirst()) it.getInt(0) else 0
} ?: 0

suspend fun instantiateTemplate(
    contentResolver: ContentResolver,
    exchangeRateHandler: ExchangeRateHandler,
    planInstanceInfo: PlanInstanceInfo,
    homeCurrencyUnit: CurrencyUnit,
    ifOpen: Boolean = false
    ) = (if (ifOpen)
    Transaction.getInstanceFromTemplateIfOpen(
        contentResolver,
        planInstanceInfo.templateId,
        planInstanceInfo.instanceId!!
    ) else
    Transaction.getInstanceFromTemplateWithTags(contentResolver, planInstanceInfo.templateId))?.let {
        val (t, tagList, dynamic) = it
        if (planInstanceInfo.date != null) {
            val date = planInstanceInfo.date / 1000
            t.date = date
            t.valueDate = date
            t.originPlanInstanceId = planInstanceInfo.instanceId
        }
        t.status = STATUS_NONE
        if (dynamic) {
            val rate = exchangeRateHandler.loadExchangeRate(
                t.amount.currencyUnit,
                homeCurrencyUnit,
                epoch2LocalDate(t.date)
            )
            t.equivalentAmount = Money(homeCurrencyUnit, t.amount.amountMajor.multiply(rate))
        }
        if (t.save(contentResolver, true) != null) {
            t.saveTags(contentResolver, tagList)
            t
        } else null
    }