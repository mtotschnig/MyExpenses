package org.totschnig.myexpenses.model

interface ITransfer: ITransaction {
    var transferPeer: Long?
    val transferAccountId: Long?
}