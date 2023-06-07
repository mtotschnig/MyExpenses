package org.totschnig.myexpenses.activity

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID

class TemplateSaver: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Transaction.getInstanceFromTemplateWithTags(intent.getLongExtra(KEY_TEMPLATEID, 0))?.let {
            if (it.first.save(true) != null && it.first.saveTags(it.second)) {
                Toast.makeText(this,
                    resources.getQuantityString(R.plurals.save_transaction_from_template_success, 1, 1),
                    Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}