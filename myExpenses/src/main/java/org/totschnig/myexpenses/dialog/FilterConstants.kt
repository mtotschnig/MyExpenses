package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import org.totschnig.myexpenses.provider.filter.SimpleCriterion

const val KEY_REQUEST_KEY = "requestKey"
const val KEY_RESULT_FILTER = "result"

fun FragmentManager.confirmFilter(requestKey: String, criterion: SimpleCriterion<*>?) {
    setFragmentResult(requestKey, Bundle().apply {
        putParcelable(KEY_RESULT_FILTER, criterion)
    }
    )
}