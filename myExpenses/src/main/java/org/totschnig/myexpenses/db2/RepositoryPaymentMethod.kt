package org.totschnig.myexpenses.db2

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import androidx.core.database.getStringOrNull
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod
import org.totschnig.myexpenses.model2.PaymentMethod
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTTYPES_METHODS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.METHODS_URI
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getEnumOrNull

fun fullProjection(context: Context) = basePaymentMethodProjection(context) + mappingColumns + KEY_ROWID

fun basePaymentMethodProjection(context: Context) = arrayOf(
    localizedLabelSqlColumn(
        context,
        DatabaseConstants.KEY_LABEL
    ) + " AS " + DatabaseConstants.KEY_LABEL, //0
    KEY_ICON, //1
    KEY_TYPE, //2
    DatabaseConstants.KEY_IS_NUMBERED, //3
    preDefinedName + " AS " + DatabaseConstants.KEY_PREDEFINED_METHOD_NAME, //4
)

val mappingColumns = arrayOf(
    "(select count(*) from " + DatabaseConstants.TABLE_TRANSACTIONS + " WHERE " + KEY_METHODID + "=" + DatabaseConstants.TABLE_METHODS + "." + KEY_ROWID + ") AS " + DatabaseConstants.KEY_MAPPED_TRANSACTIONS,
    "(select count(*) from " + DatabaseConstants.TABLE_TEMPLATES + " WHERE " + KEY_METHODID + "=" + DatabaseConstants.TABLE_METHODS + "." + KEY_ROWID + ") AS " + DatabaseConstants.KEY_MAPPED_TEMPLATES

)

val preDefinedName = StringBuilder().apply {
    append("CASE " + DatabaseConstants.KEY_LABEL)
    for (method in PreDefinedPaymentMethod.values()) {
        append(" WHEN '").append(method.name).append("' THEN '").append(method.name)
            .append("'")
    }
    append(" ELSE null END")
}.toString()

fun localizedLabelSqlColumn(ctx: Context, keyLabel: String?) =
    StringBuilder().apply {
        append("CASE ").append(keyLabel)
        for (method in PreDefinedPaymentMethod.values()) {
            append(" WHEN '").append(method.name).append("' THEN ")
            DatabaseUtils.appendEscapedSQLString(this, ctx.getString(method.resId))
        }
        append(" ELSE ").append(keyLabel).append(" END")
    }.toString()

fun Repository.loadPaymentMethod(context: Context, id: Long): PaymentMethod {
    val accountTypes = contentResolver.query(
        ACCOUNTTYPES_METHODS_URI,
        arrayOf(KEY_TYPE),
        "$KEY_METHODID = ?",
        arrayOf(id.toString()),
        null
    )!!.use { cursor ->
        cursor.asSequence.mapNotNull {
            it.getEnumOrNull<AccountType>(0)
        }.toList()
    }
    return contentResolver.query(
        instanceUir(id),
        basePaymentMethodProjection(context),
        null,
        null,
        null
    )!!.use {
        it.moveToFirst()
        PaymentMethod(
            id,
            it.getString(0),
            it.getStringOrNull(1),
            it.getInt(2),
            it.getBoolean(3),
            it.getEnumOrNull<PreDefinedPaymentMethod>(4),
            accountTypes
        )

    }
}

private fun PaymentMethod.toContentValues(context: Context?) = ContentValues().apply {
    require(preDefinedPaymentMethod == null || context != null)
    put(KEY_TYPE, type)
    icon?.also {
        put(KEY_ICON, it)
    } ?: run {
        putNull(KEY_ICON)
    }
    put(DatabaseConstants.KEY_IS_NUMBERED, isNumbered)
    if (preDefinedPaymentMethod == null || preDefinedPaymentMethod.getLocalizedLabel(context!!) != label) {
        put(DatabaseConstants.KEY_LABEL, label)
    }
}

fun Repository.createPaymentMethod(context: Context?, method: PaymentMethod): PaymentMethod {
    val id =
        ContentUris.parseId(contentResolver.insert(METHODS_URI, method.toContentValues(context))!!)
    if (method.accountTypes.isNotEmpty()) {
        setMethodAccountTypes(id, method.accountTypes)
    }
    return method.copy(id = id)
}

private fun instanceUir(id: Long) = ContentUris.withAppendedId(METHODS_URI, id)

fun Repository.updatePaymentMethod(context: Context, method: PaymentMethod) {
    contentResolver.update(
        instanceUir(method.id),
        method.toContentValues(context),
        null,
        null
    )
    setMethodAccountTypes(method.id, method.accountTypes)
}

private fun Repository.setMethodAccountTypes(id: Long, accountTypes: List<AccountType>) {
    contentResolver.delete(
        ACCOUNTTYPES_METHODS_URI,
        "$KEY_METHODID = ?",
        arrayOf(id.toString())
    )
    val initialValues = ContentValues().apply {
        put(KEY_METHODID, id)
    }
    for (accountType in accountTypes) {
        initialValues.put(KEY_TYPE, accountType.name)
        contentResolver.insert(ACCOUNTTYPES_METHODS_URI, initialValues)
    }
}

fun Repository.findPaymentMethod(label: String) = contentResolver.query(
    METHODS_URI,
    arrayOf(KEY_ROWID),
    DatabaseConstants.KEY_LABEL + " = ?",
    arrayOf(label),
    null
)?.use { if (it.moveToFirst()) it.getLong(0) else null }

/**
 * this method does not check, if label is predefined
 */
fun Repository.writePaymentMethod(label: String, accountType: AccountType?): Long {
    return createPaymentMethod(null, PaymentMethod(label = label, accountTypes = listOfNotNull(accountType))).id
}

fun Repository.deleteMethod(id: Long) {
    contentResolver.delete(
        ACCOUNTTYPES_METHODS_URI,
        "$KEY_METHODID = ?",
        arrayOf(id.toString())
    )
    contentResolver.delete(instanceUir(id), null, null)
}
