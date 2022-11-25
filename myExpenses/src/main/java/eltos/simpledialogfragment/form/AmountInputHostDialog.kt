package eltos.simpledialogfragment.form

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import org.totschnig.myexpenses.activity.CALCULATOR_REQUEST
import org.totschnig.myexpenses.activity.CalculatorInput
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.ui.AmountInput
import java.math.BigDecimal

class AmountInputHostDialog: SimpleFormDialog() {
    companion object {
        fun build() = AmountInputHostDialog()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == CALCULATOR_REQUEST && intent != null) {
            dialog?.findViewById<AmountInput>(intent.getIntExtra(CalculatorInput.EXTRA_KEY_INPUT_ID, 0))?.also {
                it.setAmount(BigDecimal(intent.getStringExtra(DatabaseConstants.KEY_AMOUNT)), false)
            } ?: run {
                Toast.makeText(context, "CALCULATOR_REQUEST launched with incorrect EXTRA_KEY_INPUT_ID", Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent)
        }
    }
}