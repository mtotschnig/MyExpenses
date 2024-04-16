package org.totschnig.myexpenses.activity

import android.os.Bundle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.AccountWidgetConfigureBinding
import org.totschnig.myexpenses.dialog.SortUtilityDialogFragment
import org.totschnig.myexpenses.fragment.AccountWidgetConfigurationFragment
import org.totschnig.myexpenses.widget.AccountWidget


class AccountWidgetConfigure : BaseWidgetConfigure(), SortUtilityDialogFragment.OnConfirmListener {
    private lateinit var binding: AccountWidgetConfigureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AccountWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setResult(RESULT_CANCELED)
        binding.btnApply.setOnClickListener { apply(AccountWidget::class.java) }
    }

    override fun onSortOrderConfirmed(sortedIds: LongArray) {
        (supportFragmentManager.findFragmentById(R.id.widget_configuration) as AccountWidgetConfigurationFragment)
            .onSortOrderConfirmed(sortedIds)
    }
}