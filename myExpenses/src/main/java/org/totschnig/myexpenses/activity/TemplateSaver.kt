package org.totschnig.myexpenses.activity

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.RepositoryTransaction
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.provider.KEY_TEMPLATEID
import org.totschnig.myexpenses.viewmodel.AccountSealedException
import org.totschnig.myexpenses.viewmodel.PlanInstanceInfo
import org.totschnig.myexpenses.viewmodel.TemplatesListViewModel

class TemplateSaver : ComponentActivity() {
    val viewModel: TemplatesListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)

        viewModel.instantiateTemplateForResult(intent.getLongExtra(KEY_TEMPLATEID, 0))
            .observe(this) {
                showTemplateInstantiationResult(it)
                finish()
            }
    }
}

fun Context.showTemplateInstantiationResult(result: Result<RepositoryTransaction>) {
    Toast.makeText(
        this,
        result.fold(
            onSuccess = {
                resources.getQuantityString(
                    R.plurals.save_transaction_from_template_success,
                    1,
                    1
                )
            },
            onFailure = {
                when(it) {
                    is AccountSealedException -> getString(R.string.object_sealed)
                    else -> getString(R.string.save_transaction_template_deleted)
                }
            }
        ),
        Toast.LENGTH_LONG
    ).show()
}