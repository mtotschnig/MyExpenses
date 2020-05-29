package org.totschnig.myexpenses.sync

import org.totschnig.myexpenses.sync.json.TransactionChange

fun mergeChanges(input: List<TransactionChange>): List<TransactionChange> =
        input.groupBy(TransactionChange::uuid).map { entry -> mergeUpdates(entry.value) }

fun mergeUpdates(changeList: List<TransactionChange>): TransactionChange {
    check(changeList.size == 0) { "nothing to merge" }
    return changeList
            .sortedBy { obj: TransactionChange -> obj.timeStamp() }
            .reduce { initial: TransactionChange, change: TransactionChange -> mergeUpdate(initial, change) }
}

fun mergeUpdate(initial: TransactionChange, change: TransactionChange): TransactionChange {
    check(change.isCreateOrUpdate && initial.isCreateOrUpdate) { "Can only merge creates and updates" }
    check(initial.uuid() == change.uuid()) { "Can only merge changes with same uuid" }
    val builder = initial.toBuilder()
    if (change.parentUuid() != null) {
        builder.setParentUuid(change.parentUuid())
    }
    if (change.comment() != null) {
        builder.setComment(change.comment())
    }
    if (change.date() != null) {
        builder.setDate(change.date())
    }
    if (change.amount() != null) {
        builder.setAmount(change.amount())
    }
    if (change.label() != null) {
        builder.setLabel(change.label())
    }
    if (change.payeeName() != null) {
        builder.setPayeeName(change.payeeName())
    }
    if (change.transferAccount() != null) {
        builder.setTransferAccount(change.transferAccount())
    }
    if (change.methodLabel() != null) {
        builder.setMethodLabel(change.methodLabel())
    }
    if (change.crStatus() != null) {
        builder.setCrStatus(change.crStatus())
    }
    if (change.referenceNumber() != null) {
        builder.setReferenceNumber(change.referenceNumber())
    }
    if (change.pictureUri() != null) {
        builder.setPictureUri(change.pictureUri())
    }
    if (change.splitParts() != null) {
        builder.setSplitParts(change.splitParts())
    }
    return builder.setCurrentTimeStamp().build()
}