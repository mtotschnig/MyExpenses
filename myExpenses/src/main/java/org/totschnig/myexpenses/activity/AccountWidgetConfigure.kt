package org.totschnig.myexpenses.activity

import android.os.Bundle
import org.totschnig.myexpenses.databinding.AccountWidgetConfigureBinding
import org.totschnig.myexpenses.widget.AccountWidget


class AccountWidgetConfigure : BaseWidgetConfigure() {
    private lateinit var binding: AccountWidgetConfigureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AccountWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setResult(RESULT_CANCELED)
        binding.btnApply.setOnClickListener { apply(AccountWidget::class.java) }
    }
}