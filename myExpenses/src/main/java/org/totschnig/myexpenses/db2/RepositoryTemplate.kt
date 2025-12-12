package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import androidx.annotation.VisibleForTesting
import androidx.core.content.contentValuesOf
import org.totschnig.myexpenses.db2.Repository.Companion.RECORD_SEPARATOR
import org.totschnig.myexpenses.db2.entities.Plan
import org.totschnig.myexpenses.db2.entities.Template
import org.totschnig.myexpenses.db2.entities.Template.Action
import org.totschnig.myexpenses.db2.entities.Transaction
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.CalendarProviderProxy
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_AMOUNT
import org.totschnig.myexpenses.provider.KEY_CATID
import org.totschnig.myexpenses.provider.KEY_COLOR
import org.totschnig.myexpenses.provider.KEY_COMMENT
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.KEY_DEFAULT_ACTION
import org.totschnig.myexpenses.provider.KEY_INSTANCEID
import org.totschnig.myexpenses.provider.KEY_METHODID
import org.totschnig.myexpenses.provider.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.KEY_PAYEEID
import org.totschnig.myexpenses.provider.KEY_PLANID
import org.totschnig.myexpenses.provider.KEY_PLAN_EXECUTION
import org.totschnig.myexpenses.provider.KEY_PLAN_EXECUTION_ADVANCE
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_SEALED
import org.totschnig.myexpenses.provider.KEY_TAGLIST
import org.totschnig.myexpenses.provider.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.KEY_TITLE
import org.totschnig.myexpenses.provider.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.KEY_UUID
import org.totschnig.myexpenses.provider.SPLIT_CATID
import org.totschnig.myexpenses.provider.TABLE_PLAN_INSTANCE_STATUS
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.PLAN_INSTANCE_STATUS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TEMPLATES_URI
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.provider.useAndMapToOne
import org.totschnig.myexpenses.util.ExchangeRateHandler
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.viewmodel.PlanInstanceInfo
import org.totschnig.myexpenses.viewmodel.data.PlanInstance
import org.totschnig.myexpenses.viewmodel.data.Tag
import java.lang.UnsupportedOperationException
import java.time.LocalDate
import kotlin.math.roundToLong

fun Template.asContentValues(forInsert: Boolean): ContentValues {
    return ContentValues().apply {
        put(KEY_TITLE, title)
        put(KEY_AMOUNT, amount)
        // Storing currency is not in TEMPLATE_CREATE, but it is in `fromCursor`.
        // We assume currency comes from the account.
        put(KEY_COMMENT, comment)
        put(KEY_ACCOUNTID, accountId)
        put(KEY_CATID, categoryId)
        put(KEY_PAYEEID, payeeId)
        put(KEY_METHODID, methodId)
        put(KEY_TRANSFER_ACCOUNT, transferAccountId)
        put(KEY_PARENTID, parentId)
        put(KEY_PLANID, planId)
        put(KEY_PLAN_EXECUTION, if (planExecutionAutomatic) 1 else 0)
        put(KEY_PLAN_EXECUTION_ADVANCE, planExecutionAdvance)
        put(KEY_DEFAULT_ACTION, defaultAction.name)
        put(KEY_ORIGINAL_AMOUNT, originalAmount)
        put(KEY_ORIGINAL_CURRENCY, originalCurrency)
        put(KEY_DEBT_ID, debtId)
        if (forInsert) {
            require(uuid.isNotBlank())
            put(KEY_UUID, uuid)
        }
        put(KEY_TAGLIST, tagList.joinToString("$RECORD_SEPARATOR"))
    }
}

data class RepositoryTemplate(
    val data: Template,
    val splitParts: List<RepositoryTemplate>? = null,
    val plan: Plan? = null,
    val tags: List<Tag>? = null,
) {
    val id: Long
        get() = data.id
    val title: String
        get() = data.title
    val isTransfer: Boolean
        get() = data.isTransfer
    val isSplit: Boolean
        get() = splitParts != null

    suspend fun instantiate(
        currencyContext: CurrencyContext,
        exchangeRateHandler: ExchangeRateHandler,
        planInstanceInfo: PlanInstanceInfo? = null,
    ): RepositoryTransaction {
        val date = planInstanceInfo?.date?.div(1000)
        val uuid = generateUuid()
        val instanceData = data.instantiate(uuid).let {
            if (date != null) it.copy(date = date) else it
        }
        return RepositoryTransaction(
            data = instanceData,
            transferPeer = if (data.isTransfer) {
                Transaction(
                    accountId = data.transferAccountId!!,
                    transferAccountId = data.accountId,
                    amount = if (data.currency == data.transferAccountCurrency) -data.amount else {
                        try {
                            val currency = currencyContext[data.currency!!]
                            val transferCurrency = currencyContext[data.transferAccountCurrency!!]
                            val amount = Money(currency, data.amount)
                            val rate = exchangeRateHandler.loadExchangeRate(
                                currency,
                                transferCurrency,
                                LocalDate.now()
                            )
                            Money(transferCurrency, -amount.amountMajor.multiply(rate)).amountMinor
                        } catch (e: Exception) {
                            if (e !is UnsupportedOperationException) {
                                CrashHandler.report(e)
                            }
                            0
                        }
                    },
                    categoryId = data.categoryId,
                    comment = data.comment,
                    categoryPath = data.categoryPath,
                    currency = data.currency,
                    date = instanceData.date,
                    uuid = uuid
                )
            } else null,
            splitParts = splitParts?.map { it.instantiate(currencyContext, exchangeRateHandler, planInstanceInfo) },
            tags = tags
        )
    }

    companion object {
        fun fromTransaction(
            t: RepositoryTransaction,
            title: String = "",
        ) = RepositoryTemplate(
            data = Template.deriveFrom(t.data, title),
            splitParts = t.splitParts?.map {
                RepositoryTemplate(Template.deriveFrom(it.data, ""))
            },
            tags = t.tags
        )
    }
}

fun Repository.getPayeeForTemplate(id: Long) = contentResolver.findBySelection(
    "$KEY_ROWID = ?",
    arrayOf(id.toString()),
    KEY_PAYEEID
)

private fun ContentResolver.findBySelection(
    selection: String,
    selectionArgs: Array<String>,
    column: String,
) =
    query(
        TEMPLATES_URI,
        arrayOf(column),
        selection,
        selectionArgs,
        null
    )?.use {
        if (it.moveToFirst()) it.getLong(0) else null
    } ?: -1


fun Repository.planCount(): Int = contentResolver.query(
    TEMPLATES_URI,
    arrayOf("count(*)"),
    "$KEY_PLANID is not null",
    null,
    null
)?.use {
    if (it.moveToFirst()) it.getInt(0) else 0
} ?: 0

private const val IS_OPEN_CHECK =
    "NOT exists(SELECT 1 from $TABLE_PLAN_INSTANCE_STATUS WHERE $KEY_INSTANCEID = ? AND $KEY_TEMPLATEID = $KEY_ROWID)"

fun Repository.loadTemplateIfInstanceIsOpen(templateId: Long, instanceId: Long) =
    loadTemplate(
        templateId,
        selection = "$KEY_ROWID = ? AND $IS_OPEN_CHECK",
        selectionArgs = arrayOf(templateId.toString(), instanceId.toString()),
        require = false
    )

fun Repository.loadTemplateForPlanIfInstanceIsOpen(planId: Long, instanceId: Long) =
    loadTemplate(
        planId,
        selection = "$KEY_PLANID = ? AND $IS_OPEN_CHECK",
        selectionArgs = arrayOf(planId.toString(), instanceId.toString()),
        require = false
    )

fun Repository.loadTemplate(
    templateId: Long,
    selection: String? = "$KEY_ROWID = ?",
    selectionArgs: Array<String>? = arrayOf(templateId.toString()),
    require: Boolean = true,
    withTags: Boolean = false,
) = contentResolver.query(
    TEMPLATES_URI,
    null,
    selection,
    selectionArgs,
    null
)!!.use { cursor ->
    when {
        cursor.moveToFirst() -> Template.fromCursor(cursor).let { template ->
            val tags = if (withTags) loadTagsForTemplate(templateId) else null
            RepositoryTemplate(
                data = template.copy(tagList = tags?.map { it.id } ?: emptyList()),
                splitParts = if (template.isSplit)
                    loadSplitParts(template.id).map {
                        val tags = if (withTags) loadTagsForTemplate(it.id) else null
                        RepositoryTemplate(
                            data = it.copy(tagList = tags?.map { it.id } ?: emptyList()),
                            tags = tags
                        )
                    }
                else null,
                plan = template.planId?.let {
                    //noinspection MissingPermission
                    loadPlan(it)
                },
                tags = tags
            )
        }

        require -> throw IllegalArgumentException("Transaction not found")
        else -> null
    }
}

private fun Repository.loadSplitParts(templateId: Long) = contentResolver.query(
    TEMPLATES_URI, null, "$KEY_PARENTID = ?", arrayOf(templateId.toString()), null
)!!.useAndMapToList {
    Template.fromCursor(it)
}

fun Repository.updateTemplate(
    repositoryTransaction: RepositoryTemplate,
) = when {
    repositoryTransaction.isSplit -> updateSplitTemplate(
        repositoryTransaction.data,
        repositoryTransaction.splitParts!!.map { it.data }
    )

    else -> updateTemplate(repositoryTransaction.data)
}

fun Repository.updateSplitTemplate(
    parentTemplate: Template, splitParts: List<Template>,
): Boolean {
    // --- Validation ---
    require(parentTemplate.isSplit) { "Parent transaction must be a split." }
    require(splitParts.sumOf { it.amount } == parentTemplate.amount) { "Sum of splits must equal parent amount." }
    require(splitParts.all { it.accountId == parentTemplate.accountId }) { "All splits must be in the same account." }

    val operations = ArrayList<ContentProviderOperation>()

    // --- 1. Handle Deletions ---
    // Get IDs of parts that have a non-zero ID (i.e., they already exist in the DB).
    val keepIds = splitParts.mapNotNull { if (it.id != 0L) it.id else null }

    if (keepIds.isEmpty()) {
        // If no existing parts are being kept, delete all children of the parent.
        operations.add(
            ContentProviderOperation.newDelete(TEMPLATES_URI)
                .withSelection("$KEY_PARENTID = ?", arrayOf(parentTemplate.id.toString()))
                .build()
        )
    } else {
        // Otherwise, delete any children that are NOT in the list of IDs to keep.
        val placeholders = List(keepIds.size) { "?" }.joinToString(",")
        val selection = "$KEY_PARENTID = ? AND $KEY_ROWID NOT IN ($placeholders)"
        val selectionArgs = arrayOf(parentTemplate.id.toString()) + keepIds.map { it.toString() }

        operations.add(
            ContentProviderOperation.newDelete(TEMPLATES_URI)
                .withSelection(selection, selectionArgs)
                .build()
        )
    }


    // --- 2. Update Parent Transaction ---
    operations.add(
        ContentProviderOperation.newUpdate(
            ContentUris.withAppendedId(TEMPLATES_URI, parentTemplate.id)
        )
            .withValues(parentTemplate.asContentValues(false))
            .build()
    )

    // --- 3. Insert New Parts and Update Existing Parts ---
    for (template in splitParts) {
        val operation = if (template.id == 0L) {
            // NEW: This is a new split part, so insert it.
            ContentProviderOperation.newInsert(TEMPLATES_URI)
                .withValues(
                    template.asContentValues(true)
                        .apply {
                            put(KEY_PARENTID, parentTemplate.id)
                        })
        } else {
            // EXISTING: This part already exists, so update it.
            ContentProviderOperation.newUpdate(
                ContentUris.withAppendedId(TEMPLATES_URI, template.id)
            )
                .withValues(template.asContentValues(false))
        }
        operations.add(operation.build())
    }

    val results = contentResolver.applyBatch(TransactionProvider.AUTHORITY, operations)
    return results.mapIndexed { index, result ->
        //first result is delete, others either insert or update
        if (index == 0) true else result.uri != null || result.count == 1
    }.all { it }
}

fun Repository.updateTemplate(
    template: Template,
) = contentResolver.update(
    ContentUris.withAppendedId(TEMPLATES_URI, template.id),
    template.asContentValues(false),
    null, null
) == 1

fun Repository.createTemplate(template: RepositoryTemplate) =
    when {
        template.isSplit -> createSplitTemplate(
            template.data,
            template.splitParts!!.map { it.data })

        else -> createTemplate(template.data)
    }.also { template ->
        template.plan?.id?.let {
            updateCustomAppUri(it, template.id)
        }
    }

@VisibleForTesting
fun Repository.createTemplate(template: Template): RepositoryTemplate {
    require(template.id == 0L) { "Use updateTemplate for existing templates" }
    require((template.originalAmount != null) == (template.originalCurrency != null)) {
        "originalAmount and originalCurrency must be set together"
    }
    val id = ContentUris.parseId(
        contentResolver.insert(
            TEMPLATES_URI,
            template.asContentValues(true)
        )!!
    )
    return RepositoryTemplate(template.copy(id = id))
}

@VisibleForTesting
fun Repository.createSplitTemplate(
    parentTemplate: Template,
    splits: List<Template>,
): RepositoryTemplate {
    // --- Validation ---
    require(parentTemplate.isSplit) { "Parent transaction must be a split." }
    require(splits.sumOf { it.amount } == parentTemplate.amount) { "Sum of splits must equal parent amount." }

    val operations = ArrayList<ContentProviderOperation>()

    // --- Operation 0: Insert the Parent Transaction ---
    operations.add(
        ContentProviderOperation.newInsert(TEMPLATES_URI)
            .withValues(parentTemplate.asContentValues(true))
            .build()
    )
    val parentBackRefIndex = 0

    // Prepare to build the complete return objects
    val finalSplitParts = mutableListOf<Template>()
    var opIndex = 1 // Start counting operations after the parent

    // --- Process each split part ---
    splits.forEach { splitPart ->
        // --- This is a REGULAR split part ---
        operations.add(
            ContentProviderOperation.newInsert(TEMPLATES_URI)
                .withValues(splitPart.asContentValues(true))
                .withValueBackReference(KEY_PARENTID, parentBackRefIndex)
                .build()
        )
        // Prepare the final object, ID will be filled in later
        finalSplitParts.add(splitPart)
        opIndex++
    }

    // --- Atomically execute all operations ---
    val results = contentResolver.applyBatch(TransactionProvider.AUTHORITY, operations)

    // --- Construct the final return object ---
    val parentId = ContentUris.parseId(results[0].uri!!)
    val finalParent = parentTemplate.copy(id = parentId)

    var resultIndex = 1 // Start processing results after the parent
    val enrichedSplitParts = finalSplitParts.map { splitPart ->
        // Regular split part
        val newId = ContentUris.parseId(results[resultIndex].uri!!)
        resultIndex++
        RepositoryTemplate(splitPart.copy(id = newId, parentId = parentId))
    }
    return RepositoryTemplate(finalParent, enrichedSplitParts)
}

suspend fun Repository.instantiateTemplate(
    exchangeRateHandler: ExchangeRateHandler,
    planInstanceInfo: PlanInstanceInfo,
    currencyContext: CurrencyContext,
    ifOpen: Boolean = false,
): RepositoryTransaction? {
    val template = (if (ifOpen) loadTemplateIfInstanceIsOpen(
        planInstanceInfo.templateId,
        planInstanceInfo.instanceId!!
    ) else loadTemplate(planInstanceInfo.templateId, withTags = true)) ?: return null

    val t = createTransaction(
        template.instantiate(currencyContext, exchangeRateHandler, planInstanceInfo)
            .let { transaction ->
                if (template.data.dynamic) {
                    val homeCurrency = currencyContext.homeCurrencyUnit
                    val account = loadAccount(template.data.accountId)!!
                    val currency = currencyContext[account.currency]
                    val amount = Money(currency, template.data.amount)
                    transaction.copy(
                        data = transaction.data.copy(
                            equivalentAmount = try {
                                val date = planInstanceInfo.date?.let {
                                    epoch2LocalDate(it / 1000)
                                } ?: LocalDate.now()
                                val rate = exchangeRateHandler.loadExchangeRate(
                                    currency,
                                    homeCurrency,
                                    date
                                )
                                Money(homeCurrency, amount.amountMajor.multiply(rate)).amountMinor
                            } catch (_: Exception) {
                                (amount.amountMinor * account.exchangeRate).roundToLong()
                            }
                        )
                    )
                } else transaction
            }
    )
    if (planInstanceInfo.instanceId != null) {
        linkTemplateWithTransaction(planInstanceInfo.templateId, t.id, planInstanceInfo.instanceId)
    }
    return t
}

fun Repository.updateNewPlanEnabled(licenceHandler: LicenceHandler) {
    var newPlanEnabled = true
    var newSplitTemplateEnabled = true
    if (!licenceHandler.hasAccessTo(ContribFeature.PLANS_UNLIMITED)) {
        if (count(
                TEMPLATES_URI,
                "$KEY_PLANID is not null",
                null
            ) >= ContribFeature.FREE_PLANS
        ) {
            newPlanEnabled = false
        }
    }
    prefHandler.putBoolean(PrefKey.NEW_PLAN_ENABLED, newPlanEnabled)

    if (!licenceHandler.hasAccessTo(ContribFeature.SPLIT_TEMPLATE)) {
        if (count(
                TEMPLATES_URI,
                "$KEY_CATID = $SPLIT_CATID",
                null
            ) >= ContribFeature.FREE_SPLIT_TEMPLATES
        ) {
            newSplitTemplateEnabled = false
        }
    }
    prefHandler.putBoolean(PrefKey.NEW_SPLIT_TEMPLATE_ENABLED, newSplitTemplateEnabled)
}

fun Repository.deleteTemplate(id: Long, deletePlan: Boolean = false) {
    val t = loadTemplate(id) ?: return
    if (t.data.planId != null) {
        if (deletePlan) {
            //noinspection MissingPermission
            deletePlan(t.data.planId)
        }
        contentResolver.delete(
            PLAN_INSTANCE_STATUS_URI,
            "$KEY_TEMPLATEID = ?",
            arrayOf(id.toString())
        )
    }
    contentResolver.delete(
        TEMPLATES_URI.buildUpon().appendPath(id.toString())
            .build(),
        null,
        null
    )
}

fun Repository.getPlanInstance(
    planId: Long,
    date: Long,
) = contentResolver.query(
    TEMPLATES_URI.buildUpon().appendQueryParameter(
        TransactionProvider.QUERY_PARAMETER_WITH_INSTANCE,
        CalendarProviderProxy.calculateId(date).toString()
    ).build(),
    null, "$KEY_PLANID= ?",
    arrayOf(planId.toString()),
    null
)?.useAndMapToOne { c ->
    val instanceId = c.getLongOrNull(KEY_INSTANCEID)
    val transactionId = c.getLongOrNull(KEY_TRANSACTIONID)
    val templateId = c.getLong(c.getColumnIndexOrThrow(KEY_ROWID))
    val currency =
        currencyContext[c.getString(c.getColumnIndexOrThrow(KEY_CURRENCY))]
    val amount = Money(
        currency,
        c.getLong(c.getColumnIndexOrThrow(KEY_AMOUNT))
    )
    PlanInstance(
        templateId,
        instanceId,
        transactionId,
        c.getString(c.getColumnIndexOrThrow(KEY_TITLE)),
        date,
        c.getInt(c.getColumnIndexOrThrow(KEY_COLOR)),
        amount,
        c.getInt(c.getColumnIndexOrThrow(KEY_SEALED)) == 1
    )
}

fun Repository.linkTemplateWithTransaction(
    templateId: Long,
    transactionId: Long,
    instanceId: Long,
) {
    contentResolver.insert(
        PLAN_INSTANCE_STATUS_URI,
        contentValuesOf(
            KEY_TEMPLATEID to templateId,
            KEY_INSTANCEID to instanceId,
            KEY_TRANSACTIONID to transactionId
        )
    )
}

fun Repository.insertTemplate(
    title: String,
    accountId: Long,
    amount: Long = 0L,
    categoryId: Long? = null,
    transferAccountId: Long? = null,
    defaultAction: Action = Action.EDIT,
    payeeId: Long? = null,
    methodId: Long? = null,
    comment: String? = null,
    debtId: Long? = null,
) = createTemplate(
    Template(
        title = title,
        accountId = accountId,
        amount = amount,
        defaultAction = defaultAction,
        categoryId = categoryId,
        transferAccountId = transferAccountId,
        uuid = generateUuid(),
        payeeId = payeeId,
        methodId = methodId,
        comment = comment,
        debtId = debtId
    )
)