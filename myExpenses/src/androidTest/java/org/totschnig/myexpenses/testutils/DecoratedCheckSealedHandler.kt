package org.totschnig.myexpenses.testutils

import android.content.ContentResolver
import android.database.Cursor
import androidx.test.espresso.idling.CountingIdlingResource
import org.totschnig.myexpenses.provider.CheckSealedHandler

class DecoratedCheckSealedHandler(cr: ContentResolver, private val countingIdlingResource: CountingIdlingResource) : CheckSealedHandler(cr) {
    override fun check(itemIds: LongArray, listener: ResultListener) {
        countingIdlingResource.increment()
        super.check(itemIds, listener)
    }

    override fun onQueryComplete(token: Int, cookie: Any, cursor: Cursor?) {
        countingIdlingResource.decrement()
        super.onQueryComplete(token, cookie, cursor)
    }
}