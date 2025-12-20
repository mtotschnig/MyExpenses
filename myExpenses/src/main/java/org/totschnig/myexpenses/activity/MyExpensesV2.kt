package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import org.totschnig.myexpenses.compose.MainScreen
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel

class MyExpensesV2: LaunchActivity() {

    val viewModel: MyExpensesV2ViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(injector) {
            inject(viewModel)
        }
        setContent {

            MainScreen(viewModel)
        }
    }
}