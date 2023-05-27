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
    }

    override val fabDescription = R.string.menu_create_budget

    override fun onFabClicked() {
        startActivity(Intent(this, BudgetEdit::class.java))
    }
}
