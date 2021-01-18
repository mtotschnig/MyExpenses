/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Based on Financisto (c) 2010 Denis Solonenko, made available
 *   under the terms of the GNU Public License v2.0
 */
package org.totschnig.myexpenses.provider.filter

import android.os.Parcel
import android.os.Parcelable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants

const val TRANSFER_COLUMN = DatabaseConstants.KEY_TRANSFER_ACCOUNT

class TransferCriteria : IdCriteria {
    constructor(label: String?, vararg ids: Long) : super(label, *ids)
    @Suppress("unused") constructor(label: String?, vararg ids: String?) : super(label, *ids)
    constructor(`in`: Parcel?) : super(`in`)

    override fun getSelection(): String {
        val selection = operation.getOp(values.size)
        return DatabaseConstants.KEY_TRANSFER_PEER + " IS NOT NULL AND (" + column + " " + selection + " OR " + DatabaseConstants.KEY_ACCOUNTID + " " + selection + ")"
    }

    override fun getSelectionArgs() = arrayOf(*values, *values)

    override fun getID(): Int {
        return R.id.FILTER_TRANSFER_COMMAND
    }

    public override fun getColumn(): String {
        return TRANSFER_COLUMN
    }

    companion object CREATOR : Parcelable.Creator<TransferCriteria> {
        override fun createFromParcel(`in`: Parcel) = TransferCriteria(`in`)

        override fun newArray(size: Int): Array<TransferCriteria?> = arrayOfNulls(size)

        fun fromStringExtra(extra: String?) = fromStringExtra(extra, TransferCriteria::class.java)
    }
}