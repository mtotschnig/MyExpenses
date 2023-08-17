package org.totschnig.myexpenses.db2

import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTRIBUTE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CONTEXT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.useAndMap
import org.totschnig.myexpenses.viewmodel.FinTsAttribute
import java.lang.IllegalStateException

interface Attribute {
    val name: String
    val context: String
    val userVisible: Boolean

    companion object {
        fun from(cursor: Cursor): Pair<Attribute, String> =
            when(val context = cursor.getString(KEY_CONTEXT)) {
                FinTsAttribute.CONTEXT -> FinTsAttribute.valueOf(cursor.getString(KEY_ATTRIBUTE_NAME))
                else -> throw IllegalStateException("Unknown context $context")
            } to cursor.getString(KEY_VALUE)
    }
}

fun Repository.configureAttributes(attributes: List<Attribute>) {
    val ops = ArrayList<ContentProviderOperation>()
    attributes.forEach {
        ops.add(
            ContentProviderOperation.newInsert(TransactionProvider.ATTRIBUTES_URI)
                .withValue(KEY_ATTRIBUTE_NAME, it.name)
                .withValue(KEY_CONTEXT, it.context)
                .build()
        )
    }
    contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)
}

fun Repository.saveAttributes(transactionId: Long, attributes: Map<out Attribute, String>) {
    val ops = ArrayList<ContentProviderOperation>()
    attributes.forEach {
        ops.add(
            ContentProviderOperation.newInsert(TransactionProvider.TRANSACTIONS_ATTRIBUTES_URI)
                .withValue(KEY_TRANSACTIONID, transactionId)
                .withValue(KEY_ATTRIBUTE_NAME, it.key.name)
                .withValue(KEY_CONTEXT, it.key.context)
                .withValue(KEY_VALUE, it.value)
                .build()
        )
    }
    contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)
}

@SuppressLint("Recycle")
fun Repository.loadAttributes(transactionId: Long): List<Pair<Attribute, String>> {
    return contentResolver.query(TransactionProvider.TRANSACTIONS_ATTRIBUTES_URI, null,
        "$KEY_TRANSACTIONID = ?", arrayOf(transactionId.toString()), null
    )?.useAndMap { Attribute.from(it) } ?: emptyList()
}