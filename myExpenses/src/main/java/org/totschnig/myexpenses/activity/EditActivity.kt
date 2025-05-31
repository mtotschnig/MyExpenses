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

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import com.evernote.android.state.State
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.retrofit.ExchangeRateApi
import org.totschnig.myexpenses.ui.ButtonWithDialog
import org.totschnig.myexpenses.ui.ExchangeRateEdit
import org.totschnig.myexpenses.util.ExchangeRateHandler
import org.totschnig.myexpenses.util.linkInputsWithLabels
import org.totschnig.myexpenses.util.transformForUser
import java.time.LocalDate
import javax.inject.Inject

abstract class EditActivity : ProtectedFragmentActivity(), TextWatcher, ButtonWithDialog.Host,
    ExchangeRateEdit.Host {
    protected var isSaving = false

    @State
    var isDirty = false

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    @State
    var newInstance = true

    @Inject
    lateinit var exchangeRateHandler: ExchangeRateHandler

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable) {
        setDirty()
    }

    override fun injectDependencies() {
        injector.inject(this)
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
        ConfirmationDialogFragment.newInstance(
            Bundle().apply {
                putString(
                    ConfirmationDialogFragment.KEY_MESSAGE,
                    if (newInstance) getString(R.string.discard) + "?" else getString(R.string.dialog_confirm_discard_changes)
                )
                putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, android.R.id.home)
                putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.response_yes)
                putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, R.string.response_no)
            })
            .show(supportFragmentManager, "DISCARD")
    }

    override val fabIcon: Int = R.drawable.ic_menu_done
    override val fabDescription = R.string.menu_save_help_text

    override fun onFabClicked() {
        super.onFabClicked()
        doSave(false)
    }

    protected open fun doSave(andNew: Boolean) {
        if (!isSaving) {
            saveState()
        }
    }

    protected open fun saveState() {
        isSaving = true
    }

    open val onBackPressedCallbackEnabled: Boolean
        get() = isDirty

    fun updateOnBackPressedCallbackEnabled() {
        onBackPressedCallback.isEnabled = onBackPressedCallbackEnabled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedCallback = object : OnBackPressedCallback(isDirty) {
            override fun handleOnBackPressed() {
                if (isDirty) {
                    showDiscardDialog()
                } else {
                    dispatchOnBackPressed()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCurrencySelectionChanged(currencyUnit: CurrencyUnit) {
        setDirty()
    }

    override fun onValueSet(view: View) {
        setDirty()
    }

    protected open fun dispatchOnBackPressed() {
        doHome()
    }

    protected fun linkInputsWithLabels() {
        linkInputsWithLabels(findViewById(R.id.Table))
    }

    fun setDirty() {
        isDirty = true
        updateOnBackPressedCallbackEnabled()
    }

    protected fun clearDirty() {
        isDirty = false
        updateOnBackPressedCallbackEnabled()
    }

    override val snackBarContainerId = R.id.edit_container

    open val date: LocalDate = LocalDate.now()

    override suspend fun loadExchangeRate(
        other: CurrencyUnit,
        base: CurrencyUnit,
        source: ExchangeRateApi?,
    ) = runCatching {
        exchangeRateHandler.loadExchangeRate(other, base, date, source)
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = {
            Result.failure(it.transformForUser(this, other.code, base.code))
        }
    )
}