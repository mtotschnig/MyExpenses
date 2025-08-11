package org.totschnig.myexpenses.testutils

import android.content.ContentResolver
import android.database.Cursor
import androidx.test.espresso.idling.CountingIdlingResource
import org.totschnig.myexpenses.provider.CheckSealedHandler

class DecoratedCheckSealedHandler(cr: ContentResolver, private val countingIdlingResource: CountingIdlingResource) : CheckSealedHandler(cr) {
    override fun checkAccount(accountId: Long, listener: ResultListener) {
        countingIdlingResource.increment()
        super.checkAccount(accountId, listener)
    }

    override fun check(itemIds: List<Long>, withTransfer: Boolean, listener: ResultListener) {
        countingIdlingResource.increment()
        super.check(itemIds, withTransfer, listener)
    }

    override fun onQueryComplete(token: Int, cookie: Any, cursor: Cursor?) {
        countingIdlingResource.decrement()
        super.onQueryComplete(token, cookie, cursor)
    }
}