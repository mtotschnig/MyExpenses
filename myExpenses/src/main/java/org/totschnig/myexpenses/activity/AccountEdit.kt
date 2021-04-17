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

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.color.SimpleColorDialog
import org.apache.commons.lang3.ArrayUtils
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.CurrencyAdapter
import org.totschnig.myexpenses.databinding.OneAccountBinding
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccountNames
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.ExchangeRateEdit
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.util.Result
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.Currency.Companion.create
import java.io.Serializable
import java.math.BigDecimal

/**
 * Activity for editing an account
 *
 * @author Michael Totschnig
 */
class AccountEdit : AmountActivity(), ExchangeRateEdit.Host, AdapterView.OnItemSelectedListener, ContribIFace, OnDialogResultListener {
    lateinit var binding: OneAccountBinding
    private lateinit var currencySpinner: SpinnerHelper
    private lateinit var accountTypeSpinner: SpinnerHelper
    private lateinit var syncSpinner: SpinnerHelper
    private lateinit var currencyAdapter: CurrencyAdapter
    private lateinit var currencyViewModel: CurrencyViewModel
    private var _account: Account? = null
    private val account: Account
        get() {
            if (_account == null) {
                val extras = intent.extras
                val rowId = extras?.getLong(DatabaseConstants.KEY_ROWID) ?: 0
                _account = if (rowId != 0L) {
                    Account.getInstanceFromDb(rowId)
                } else {
                    Account().apply {
                        setCurrency(currencyContext[extras?.getString(DatabaseConstants.KEY_CURRENCY)
                                ?: currencyViewModel.default.code])
                    }
                }
            }
            return _account!!
        }

    public override fun getDiscardNewMessage(): Int {
        return R.string.dialog_confirm_discard_new_account
    }

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OneAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        currencyViewModel = ViewModelProvider(this).get(CurrencyViewModel::class.java)
        val extras = intent.extras
        val rowId = extras?.getLong(DatabaseConstants.KEY_ROWID) ?: 0
        if (rowId != 0L) {
            mNewInstance = false
            setTitle(R.string.menu_edit_account)
            binding.Label.setText(account.label)
            binding.Description.setText(account.description)
        } else {
            setTitle(R.string.menu_create_account)
        }
        configureForCurrency(account.currencyUnit)
        currencySpinner = SpinnerHelper(findViewById(R.id.Currency))
        currencyAdapter = CurrencyAdapter(this, android.R.layout.simple_spinner_item)
        currencySpinner.adapter = currencyAdapter
        val spinner = findViewById<Spinner>(R.id.AccountType)
        DialogUtils.configureTypeSpinner(spinner)
        accountTypeSpinner = SpinnerHelper(spinner)
        syncSpinner = SpinnerHelper(findViewById(R.id.Sync))
        configureSyncBackendAdapter()
        populateFields()
        currencyViewModel.getCurrencies().observe(this, { currencies: List<Currency?> ->
            currencyAdapter.addAll(currencies)
            if (savedInstanceState == null) {
                currencySpinner.setSelection(currencyAdapter.getPosition(create(account.currencyUnit.code, this)))
            }
        })
        linkInputsWithLabels()
    }

    override fun afterTextChanged(s: Editable) {
        super.afterTextChanged(s)
        updateCriterionLabel()
    }

    private fun updateCriterionLabel() {
        val criterionLabel: Int = when (binding.Criterion.typedValue.compareTo(BigDecimal.ZERO)) {
            1 -> R.string.saving_goal
            -1 -> R.string.credit_limit
            else -> R.string.goal_or_limit
        }
        binding.CriterionLabel.setText(criterionLabel)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PREFERENCES_REQUEST) {
            if (resultCode == RESULT_FIRST_USER) {
                finish()
            } else {
                configureSyncBackendAdapter()
            }
        }
    }

    private fun configureSyncBackendAdapter() {
        val syncBackendAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
                ArrayUtils.insert(0, getAccountNames(this), getString(R.string.synchronization_none)))
        syncBackendAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        syncSpinner.adapter = syncBackendAdapter
        if (account.syncAccountName != null) {
            val position = syncBackendAdapter.getPosition(account.syncAccountName)
            if (position > -1) {
                syncSpinner.setSelection(position)
                syncSpinner.isEnabled = false
                binding.SyncUnlink.visibility = View.VISIBLE
            }
        } else {
            binding.SyncHelp.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        setupListeners()
    }

    /**
     * populates the input field either from the database or with default value for currency (from Locale)
     */
    private fun populateFields() {
        binding.Amount.setAmount(account.openingBalance.amountMajor)
        accountTypeSpinner.setSelection(account.type.ordinal)
        UiUtils.setBackgroundOnButton(binding.colorInput.ColorIndicator, account.color)
        val criterion = account.criterion
        if (criterion != null) {
            binding.Criterion.setAmount(criterion.amountMajor)
            updateCriterionLabel()
        }
    }

    private fun setExchangeRateVisibility(currencyUnit: CurrencyUnit) {
        val homeCurrencyPref = prefHandler.getString(PrefKey.HOME_CURRENCY, currencyUnit.code)
        val isHomeAccount = currencyUnit.code == homeCurrencyPref
        binding.ERR.root.visibility = if (isHomeAccount) View.GONE else View.VISIBLE
        if (!isHomeAccount) {
            binding.ERR.ExchangeRate.setCurrencies(currencyUnit, currencyContext[homeCurrencyPref!!])
            binding.ERR.ExchangeRate.setRate(BigDecimal(if (account.currencyUnit == currencyUnit) account.exchangeRate else 1.0), true)
        }
    }

    /**
     * validates currency (must be code from ISO 4217) and opening balance
     * (a valid float according to the format from the locale)
     */
    override fun saveState() {
        val openingBalance = validateAmountInput(true) ?: return
        val currency = (currencySpinner.selectedItem as Currency).code
        account.setCurrency(currencyContext[currency])
        val label = binding.Label.text.toString()
        if (label == "") {
            binding.Label.error = getString(R.string.no_title_given)
            return
        }
        account.label = label
        account.description = binding.Description.text.toString()
        account.openingBalance = Money(account.currencyUnit, openingBalance)
        account.type = accountTypeSpinner.selectedItem as AccountType
        if (syncSpinner.selectedItemPosition > 0) {
            account.syncAccountName = syncSpinner.selectedItem as String
        }
        if (prefHandler.getString(PrefKey.HOME_CURRENCY, currency) != currency) {
            val rate = binding.ERR.ExchangeRate.getRate(false)
            if (rate != null) {
                account.exchangeRate = rate.toDouble()
            }
        }
        account.setCriterion(binding.Criterion.typedValue)
        //EditActivity.saveState calls DbWriteFragment
        super.saveState()
    }

    override fun getObject(): Model {
        return account
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int,
                                id: Long) {
        setDirty()
        val parentId = parent.id
        if (parentId == R.id.Currency) {
            try {
                val currency = (currencySpinner.selectedItem as Currency).code
                configureForCurrency(currencyContext[currency])
            } catch (e: IllegalArgumentException) {
                //will be reported to user when he tries so safe
            }
        } else if (parentId == R.id.Sync) {
            contribFeatureRequested(ContribFeature.SYNCHRONIZATION, null)
        }
    }

    private fun configureForCurrency(currencyUnit: CurrencyUnit) {
        binding.Amount.setFractionDigits(currencyUnit.fractionDigits)
        binding.Criterion.setFractionDigits(currencyUnit.fractionDigits)
        setExchangeRateVisibility(currencyUnit)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // TODO Auto-generated method stub
    }

    /*
   * callback of DbWriteFragment
   */
    override fun onPostExecute(result: Uri?) {
        if (result == null) {
            complain()
            super.onPostExecute(result)
        } else {
            val intent = Intent()
            val id = ContentUris.parseId(result)
            account.requestSync()
            intent.putExtra(DatabaseConstants.KEY_ROWID, id)
            setResult(RESULT_OK, intent)
            currencyContext.ensureFractionDigitsAreCached(account.currencyUnit)
            finish()
        }
        //no need to call super after finish
    }

    override fun onPostExecute(taskId: Int, o: Any?) {
        super.onPostExecute(taskId, o)
        val r = o as Result<*>?
        when (taskId) {
            TaskExecutionFragment.TASK_SYNC_UNLINK -> if (r!!.isSuccess) {
                syncSpinner.setSelection(0)
                syncSpinner.isEnabled = true
                binding.SyncUnlink.visibility = View.GONE
            }
            TaskExecutionFragment.TASK_SYNC_CHECK -> if (!r!!.isSuccess) {
                syncSpinner.setSelection(0)
                showHelp(r.print(this))
            }
            TaskExecutionFragment.TASK_SET_EXCLUDE_FROM_TOTALS -> if (r!!.isSuccess) {
                account.excludeFromTotals = !account.excludeFromTotals
                invalidateOptionsMenu()
            } else {
                complain()
            }
        }
    }

    private fun complain() {
        showSnackbar("Unknown error while saving account", Snackbar.LENGTH_SHORT)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(Menu.NONE, R.id.EXCLUDE_FROM_TOTALS_COMMAND, 0, R.string.menu_exclude_from_totals)
                .setCheckable(true)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.EXCLUDE_FROM_TOTALS_COMMAND)
        if (item == null) {
            CrashHandler.report(NullPointerException("EXCLUDE_FROM_TOTALS_COMMAND menu item not found"))
        } else {
            item.isChecked = account.excludeFromTotals
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        }
        when (command) {
            R.id.EXCLUDE_FROM_TOTALS_COMMAND -> {
                if (account.id != 0L) {
                    startTaskExecution(
                            TaskExecutionFragment.TASK_SET_EXCLUDE_FROM_TOTALS, arrayOf(account.id),
                            !account.excludeFromTotals, 0)
                }
                return true
            }
            R.id.SYNC_UNLINK_COMMAND -> {
                account.syncAccountName = null
                startTaskExecution(
                        TaskExecutionFragment.TASK_SYNC_UNLINK, arrayOf(account.uuid), null, 0)
                return true
            }
            R.id.SYNC_SETTINGS_COMMAND -> {
                val i = Intent(this, ManageSyncBackends::class.java).apply {
                    putExtra(KEY_UUID, account.uuid)
                }
                startActivityForResult(i, PREFERENCES_REQUEST)
                return true
            }
            else -> return false
        }
    }

    override fun setupListeners() {
        super.setupListeners()
        binding.Label.addTextChangedListener(this)
        binding.Description.addTextChangedListener(this)
        accountTypeSpinner.setOnItemSelectedListener(this)
        currencySpinner.setOnItemSelectedListener(this)
        syncSpinner.setOnItemSelectedListener(this)
        binding.Criterion.setTypeChangedListener {
            setDirty()
            updateCriterionLabel()
        }
        binding.Criterion.addTextChangedListener(this)
    }

    fun syncUnlink(@Suppress("UNUSED_PARAMETER") view: View?) {
        DialogUtils.showSyncUnlinkConfirmationDialog(this, account)
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        if (!mNewInstance) {
            startTaskExecution(
                    TaskExecutionFragment.TASK_SYNC_CHECK, arrayOf(account.uuid),
                    syncSpinner.selectedItem as String,
                    R.string.progress_dialog_checking_sync_backend)
        }
    }

    override fun contribFeatureNotCalled(feature: ContribFeature) {
        if (feature === ContribFeature.SYNCHRONIZATION) {
            syncSpinner.setSelection(0)
        }
    }

    fun syncHelp(@Suppress("UNUSED_PARAMETER") view: View?) {
        showHelp(getString(R.string.form_synchronization_help_text_add))
    }

    private fun showHelp(message: String) {
        MessageDialogFragment.newInstance(
                null,
                message,
                MessageDialogFragment.Button(R.string.pref_category_title_manage, R.id.SYNC_SETTINGS_COMMAND, null),
                MessageDialogFragment.okButton(),
                null)
                .show(supportFragmentManager, "SYNC_HELP")
    }

    fun editAccountColor(@Suppress("UNUSED_PARAMETER") view: View?) {
        SimpleColorDialog.build()
                .allowCustom(true)
                .cancelable(false)
                .neut()
                .colorPreset(account.color)
                .show(this, EDIT_COLOR_DIALOG)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (EDIT_COLOR_DIALOG == dialogTag && which == OnDialogResultListener.BUTTON_POSITIVE) {
            account.color = extras.getInt(SimpleColorDialog.COLOR)
            UiUtils.setBackgroundOnButton(binding.colorInput.ColorIndicator, account.color)
            return true
        }
        return false
    }

    override fun getDate(): LocalDate {
        return LocalDate.now()
    }

    override val amountLabel: TextView
        get() = binding.AmountLabel
    override val amountRow: ViewGroup
        get() = binding.AmountRow
    override val exchangeRateRow: ViewGroup
        get() = binding.ERR.root
    override val amountInput: AmountInput
        get() = binding.Amount
    override val exchangeRateEdit: ExchangeRateEdit
        get() = binding.ERR.ExchangeRate
}