package org.totschnig.myexpenses.activity

import android.content.Intent
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.ManageBudgetsBinding

class ManageBudgets : ProtectedFragmentActivity() {

    private lateinit var binding: ManageBudgetsBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ManageBudgetsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(true)
        setTitle(R.string.menu_budget)
    }

    override val fabDescription = R.string.menu_create_budget

    override val fabActionName = "CREATE_BUDGET"

    override val _floatingActionButton: FloatingActionButton
        get() = binding.fab.CREATECOMMAND

    override fun onFabClicked() {
        super.onFabClicked()
        startActivity(Intent(this, BudgetEdit::class.java))
    }
}
