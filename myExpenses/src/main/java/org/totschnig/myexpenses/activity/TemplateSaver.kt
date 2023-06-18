package org.totschnig.myexpenses.activity

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID

class TemplateSaver : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val instance =
            Transaction.getInstanceFromTemplateWithTags(intent.getLongExtra(KEY_TEMPLATEID, 0))
        Toast.makeText(
            this,
            if (instance == null) getString(R.string.save_transaction_template_deleted) else
                if (instance.first.save(true) != null && instance.first.saveTags(instance.second)) {
                    resources.getQuantityString(
                        R.plurals.save_transaction_from_template_success,
                        1,
                        1
                    )
                } else "Error while saving",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }
}