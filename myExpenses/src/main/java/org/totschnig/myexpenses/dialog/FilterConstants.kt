package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import org.totschnig.myexpenses.provider.filter.SimpleCriterion

const val RC_CONFIRM_FILTER = "confirmFilter"
const val KEY_RESULT_FILTER = "result"

fun FragmentManager.confirmFilter(criterion: SimpleCriterion<*>?) {
    setFragmentResult(RC_CONFIRM_FILTER, Bundle().apply {
        putParcelable(KEY_RESULT_FILTER, criterion)
    }
    )
}