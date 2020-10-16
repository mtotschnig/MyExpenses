package org.totschnig.myexpenses.activity

import android.content.Intent
import android.os.Bundle
import org.totschnig.myexpenses.R

class ManageBudgets : ProtectedFragmentActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_budgets)
        setupToolbar(true)
        setTitle(R.string.menu_budget)
        configureFloatingActionButton(R.string.menu_create_budget)
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (command == R.id.CREATE_COMMAND) {
            val i = Intent(this, BudgetEdit::class.java)
            startActivity(i)
            return true
        }
        return super.dispatchCommand(command, tag)
    }
}
