package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider

interface Attribute {
    val name: String
    val context: String
}

fun Repository.configureAttributes(attributes: List<Attribute>) {
    val ops = ArrayList<ContentProviderOperation>()
    attributes.forEach {
        ops.add(
            ContentProviderOperation.newInsert(TransactionProvider.ATTRIBUTES_URI)
                .withValue(DatabaseConstants.KEY_ATTRIBUTE_NAME, it.name)
                .withValue(DatabaseConstants.KEY_CONTEXT, it.context)
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
                .withValue(DatabaseConstants.KEY_TRANSACTIONID, transactionId)
                .withValue(DatabaseConstants.KEY_ATTRIBUTE_NAME, it.key.name)
                .withValue(DatabaseConstants.KEY_CONTEXT, it.key.context)
                .withValue(DatabaseConstants.KEY_VALUE, it.value)
                .build()
        )
    }
    contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)

}