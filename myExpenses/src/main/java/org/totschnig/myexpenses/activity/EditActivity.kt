/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.totschnig.myexpenses.activity

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import com.evernote.android.state.State
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.newInstance
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.ButtonWithDialog
import org.totschnig.myexpenses.util.linkInputsWithLabels
import java.math.BigDecimal

abstract class EditActivity : ProtectedFragmentActivity(), TextWatcher, ButtonWithDialog.Host {
    protected var mIsSaving = false

    @State
    var isDirty = false

    @JvmField
    @State
    var mNewInstance = true
    protected fun validateAmountInput(input: AmountInput, showToUser: Boolean): BigDecimal {
        return input.getTypedValue(true, showToUser)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable) {
        setDirty()
    }

    protected fun setupToolbarWithClose() {
        setupToolbar(true, R.drawable.ic_menu_close_clear_cancel)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) if (isDirty) {
            showDiscardDialog()
            return true
        } else {
            hideKeyboard()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDiscardDialog() {
        val b = Bundle()
        b.putString(
            ConfirmationDialogFragment.KEY_MESSAGE,
            if (mNewInstance) getString(R.string.discard) + "?" else getString(R.string.dialog_confirm_discard_changes)
        )
        b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, android.R.id.home)
        b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.response_yes)
        b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, R.string.response_no)
        newInstance(b)
            .show(supportFragmentManager, "DISCARD")
    }

    override val fabIcon: Int = R.drawable.ic_menu_done
    override val fabDescription = R.string.menu_save_help_text

    override fun onFabClicked() {
        doSave(false)
    }

    protected open fun doSave(andNew: Boolean) {
        if (!mIsSaving) {
            saveState()
        }
    }

    protected open fun saveState() {
        mIsSaving = true
        startDbWriteTask()
    }

    override fun onPostExecute(result: Uri?) {
        mIsSaving = false
        super.onPostExecute(result)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isDirty) {
            showDiscardDialog()
        } else {
            dispatchOnBackPressed()
        }
    }

    override fun onCurrencySelectionChanged(currencyUnit: CurrencyUnit) {
        setDirty()
    }

    override fun onValueSet(view: View) {
        setDirty()
    }

    protected open fun dispatchOnBackPressed() {
        super.onBackPressed()
    }

    protected fun linkInputsWithLabels() {
        linkInputsWithLabels(findViewById(R.id.Table))
    }

    fun setDirty() {
        isDirty = true
    }

    protected fun clearDirty() {
        isDirty = false
    }

    override val snackBarContainerId = R.id.edit_container
}