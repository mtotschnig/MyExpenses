package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import org.totschnig.myexpenses.compose.MainScreenPrototype

class MyExpensesV2: LaunchActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreenPrototype()
        }
    }
}