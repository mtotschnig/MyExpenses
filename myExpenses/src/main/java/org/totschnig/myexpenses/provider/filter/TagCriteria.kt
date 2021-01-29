package org.totschnig.myexpenses.provider.filter

import android.os.Parcel
import android.os.Parcelable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS_TAGS

const val TAG_COLUMN = KEY_TAGID

class TagCriteria(label: String, vararg ids: String) : IdCriteria(label, *ids) {
    constructor(label: String, vararg ids: Long) : this(label, *longArrayToStringArray(ids))
    constructor(parcel: Parcel) : this(parcel.readString()!!, *parcel.createStringArray()!!)

    override fun getSelection() = "%s IN (SELECT %s FROM %s WHERE %s)".format(KEY_ROWID, KEY_TRANSACTIONID, TABLE_TRANSACTIONS_TAGS, super.getSelection())

    override fun getID() = R.id.FILTER_TAG_COMMAND

    override fun getColumn() = TAG_COLUMN

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(label)
        parcel.writeStringArray(values)
    }

    override fun shouldApplyToParts() = false

    companion object CREATOR : Parcelable.Creator<TagCriteria> {
        override fun createFromParcel(parcel: Parcel) = TagCriteria(parcel)

        override fun newArray(size: Int): Array<TagCriteria?> = arrayOfNulls(size)

        fun fromStringExtra(extra: String) = fromStringExtra(extra, TagCriteria::class.java)
    }
}