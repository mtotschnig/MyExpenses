package org.totschnig.myexpenses.model

import android.content.ContentResolver
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE
import org.totschnig.myexpenses.util.ExchangeRateHandler
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.viewmodel.PlanInstanceInfo
import kotlin.math.roundToLong

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
    repository: Repository,
    exchangeRateHandler: ExchangeRateHandler,
    planInstanceInfo: PlanInstanceInfo,
    homeCurrencyUnit: CurrencyUnit,
    ifOpen: Boolean = false
) = (if (ifOpen)
    Transaction.getInstanceFromTemplateIfOpen(
        repository.contentResolver,
        planInstanceInfo.templateId,
        planInstanceInfo.instanceId!!
    ) else
    Transaction.getInstanceFromTemplateWithTags(
        repository.contentResolver,
        planInstanceInfo.templateId
    ))?.let { (t, tagList, dynamic) ->
    if (planInstanceInfo.date != null) {
        val date = planInstanceInfo.date / 1000
        t.date = date
        t.valueDate = date
        t.originPlanInstanceId = planInstanceInfo.instanceId
    }
    t.status = STATUS_NONE
    if (dynamic) {
        try {
            val rate = exchangeRateHandler.loadExchangeRate(
                t.amount.currencyUnit,
                homeCurrencyUnit,
                epoch2LocalDate(t.date)
            )
            t.equivalentAmount = Money(homeCurrencyUnit, t.amount.amountMajor.multiply(rate))
        } catch (e: Exception) {
            repository.loadAccount(t.accountId)?.exchangeRate?.also {
                t.equivalentAmount =
                    Money(homeCurrencyUnit, (t.amount.amountMinor * it).roundToLong())
            } ?: run {
                CrashHandler.report(
                    Exception("Could not apply exchange rate to transaction", e)
                )
            }
        }

    }
    t.save(repository.contentResolver, true)
    t.saveTags(repository.contentResolver, tagList)
    t
}