package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.viewmodel.PlanInstanceInfo
import org.totschnig.myexpenses.viewmodel.TemplatesListViewModel

class TemplateSaver : ComponentActivity() {
    val viewModel: TemplatesListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        val info = PlanInstanceInfo(intent.getLongExtra(KEY_TEMPLATEID, 0))

        viewModel.newFromTemplate(info).observe(this) { successCount: Int ->
            Toast.makeText(
                this,
                if (successCount == 0) getString(R.string.save_transaction_template_deleted) else resources.getQuantityString(
                    R.plurals.save_transaction_from_template_success,
                    successCount,
                    successCount
                ),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }
}