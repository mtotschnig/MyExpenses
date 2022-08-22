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
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.color.SimpleColorDialog
import icepick.State
import org.apache.commons.lang3.ArrayUtils
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.CurrencyAdapter
import org.totschnig.myexpenses.databinding.OneAccountBinding
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccountNames
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.ExchangeRateEdit
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.addChipsBulk
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.AccountEditViewModel
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.SyncBackendViewModel
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.Currency.Companion.create
import org.totschnig.myexpenses.viewmodel.data.Tag
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Activity for editing an account
 *
 * @author Michael Totschnig
 */
class AccountEdit : AmountActivity<AccountEditViewModel>(), ExchangeRateEdit.Host, AdapterView.OnItemSelectedListener, ContribIFace, OnDialogResultListener {
    lateinit var binding: OneAccountBinding
    private lateinit var currencySpinner: SpinnerHelper
    private lateinit var accountTypeSpinner: SpinnerHelper
    private lateinit var syncSpinner: SpinnerHelper
    private lateinit var currencyAdapter: CurrencyAdapter
    private lateinit var currencyViewModel: CurrencyViewModel
    private lateinit var syncViewModel: SyncBackendViewModel

    @State
    @JvmField
    var dataLoaded: Boolean = false

    @State
    @JvmField
    var syncAccountName: String? = null

    @State
    @JvmField
    var _currencyUnit: CurrencyUnit? = null

    val currencyUnit: CurrencyUnit
        get() = if (dataLoaded) _currencyUnit!! else throw IllegalStateException()

    @State
    @JvmField
    var color: Int = 0

    @State
    @JvmField
    var excludeFromTotals = false

    @State
    @JvmField
    var uuid: String? = null

    val rowId: Long
        get() = intent.getLongExtra(DatabaseConstants.KEY_ROWID, 0)

    public override fun getDiscardNewMessage(): Int {
        return R.string.dialog_confirm_discard_new_account
    }

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OneAccountBinding.inflate(layoutInflater)
        binding.TagRow.TagLabel.setText(R.string.active_tags)
        setContentView(binding.root)
        setupToolbar()
        val viewModelProvider = ViewModelProvider(this)
        currencyViewModel = viewModelProvider[CurrencyViewModel::class.java]
        viewModel = viewModelProvider[AccountEditViewModel::class.java]
        syncViewModel = viewModelProvider[SyncBackendViewModel::class.java]
        with((applicationContext as MyApplication).appComponent) {
            inject(viewModel)
            inject(currencyViewModel)
            inject(syncViewModel)
        }
        val extras = intent.extras
        currencySpinner = SpinnerHelper(findViewById(R.id.Currency))
        currencyAdapter = CurrencyAdapter(this, android.R.layout.simple_spinner_item)
        currencySpinner.adapter = currencyAdapter
        val spinner = findViewById<Spinner>(R.id.AccountType)
        DialogUtils.configureTypeSpinner(spinner)
        accountTypeSpinner = SpinnerHelper(spinner)
        syncSpinner = SpinnerHelper(findViewById(R.id.Sync))
        mNewInstance = rowId == 0L
        setTitle(if (rowId != 0L) R.string.menu_edit_account else R.string.menu_create_account)
        if (savedInstanceState == null || !dataLoaded) {
            if (rowId != 0L) {
                viewModel.accountWithTags(rowId).observe(this) {
                    if (it != null) {
                        populateFields(it)
                    } else {
                        Toast.makeText(this, "Error loading account", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            } else {
                populateFields(Account().apply {
                    setCurrency(currencyContext[extras?.getString(DatabaseConstants.KEY_CURRENCY)
                            ?: currencyViewModel.default.code])
                })
            }
        } else {
            configureForCurrency(currencyUnit)
            setup()
        }
        linkInputsWithLabels()
        viewModel.getTags().observe(this) {
            showTags(it) { tag ->
                viewModel.removeTag(tag)
                setDirty()
            }
        }
    }

    private fun setup() {
        configureSyncBackendAdapter()
        currencyViewModel.getCurrencies().observe(this) { currencies: List<Currency?> ->
            currencyAdapter.addAll(currencies)
            currencySpinner.setSelection(
                currencyAdapter.getPosition(
                    create(
                        currencyUnit.code,
                        this
                    )
                )
            )
        }
        UiUtils.setBackgroundOnButton(binding.colorInput.ColorIndicator, color)
        setupListeners()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        when (requestCode) {
            PREFERENCES_REQUEST -> if (resultCode == RESULT_FIRST_USER) {
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
        if (syncAccountName != null) {
            val position = syncBackendAdapter.getPosition(syncAccountName)
            if (position > -1) {
                syncSpinner.setSelection(position)
                syncSpinner.isEnabled = false
                binding.SyncUnlink.visibility = View.VISIBLE
            }
        } else {
            binding.SyncHelp.visibility = View.VISIBLE
        }
    }

    /**
     * populates the input field either from the database or with default value for currency (from Locale)
     */
    private fun populateFields(account: Account) {
        dataLoaded = true
        binding.Label.setText(account.label)
        binding.Description.setText(account.description)
        syncAccountName = account.syncAccountName
        _currencyUnit = account.currencyUnit
        binding.ERR.ExchangeRate.setRate(BigDecimal(account.exchangeRate), true)
        color = account.color
        excludeFromTotals = account.excludeFromTotals
        uuid = account.uuid
        configureForCurrency(currencyUnit)
        binding.Amount.setAmount(account.openingBalance.amountMajor)
        accountTypeSpinner.setSelection(account.type.ordinal)
        val criterion = account.criterion
        if (criterion != null) {
            binding.Criterion.setAmount(criterion.amountMajor)
            updateCriterionLabel()
        }
        setup()
    }

    private fun showTags(tags: Iterable<Tag>?, closeFunction: (Tag) -> Unit) {
        with(binding.TagRow.TagGroup) {
            removeAllViews()
            tags?.let { addChipsBulk(it, closeFunction) }
        }
    }

    private fun setExchangeRateVisibility(currencyUnit: CurrencyUnit) {
        val homeCurrencyPref = prefHandler.getString(PrefKey.HOME_CURRENCY, currencyUnit.code)
        val isHomeAccount = currencyUnit.code == homeCurrencyPref
        binding.ERR.root.visibility = if (isHomeAccount) View.GONE else View.VISIBLE
        if (!isHomeAccount) {
            binding.ERR.ExchangeRate.setCurrencies(currencyUnit, currencyContext[homeCurrencyPref!!])
        }
    }

    /**
     * validates currency (must be code from ISO 4217) and opening balance
     * (a valid float according to the format from the locale)
     */
    override fun saveState() {
        val label = binding.Label.text.toString()
        if (label == "") {
            binding.Label.error = getString(R.string.required)
            return
        }
        val openingBalance: BigDecimal = validateAmountInput(true) ?: return
        val currency = (currencySpinner.selectedItem as Currency).code
        val currencyUnit = currencyContext[currency]
        val account = Account(label, currencyUnit, Money(currencyUnit, openingBalance), binding.Description.text.toString(), accountTypeSpinner.selectedItem as AccountType, color).apply {
            id = rowId
            uuid = this@AccountEdit.uuid
            if (syncSpinner.selectedItemPosition > 0) {
                syncAccountName = syncSpinner.selectedItem as String
            }
            if (prefHandler.getString(PrefKey.HOME_CURRENCY, currency) != currency) {
                val rate = binding.ERR.ExchangeRate.getRate(false)
                if (rate != null) {
                    exchangeRate = rate.toDouble()
                }
            }
            setCriterion(binding.Criterion.typedValue)
            excludeFromTotals = this@AccountEdit.excludeFromTotals
        }
        viewModel.save(account).observe(this) {
            if (it < 0) {
                showSnackBar("ERROR")
            } else {
                account.requestSync()
                intent.putExtra(DatabaseConstants.KEY_ROWID, it)
                setResult(RESULT_OK, intent)
                currencyContext.ensureFractionDigitsAreCached(account.currencyUnit)
                finish()
            }
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int,
                                id: Long) {
        setDirty()
        val parentId = parent.id
        if (parentId == R.id.Currency) {
            try {
                (currencySpinner.selectedItem as? Currency)?.code?.let {
                    configureForCurrency(currencyContext[it])
                }
            } catch (e: IllegalArgumentException) {
                //will be reported to user when he tries so safe
            }
        } else if (parentId == R.id.Sync && position > 0) {
            contribFeatureRequested(ContribFeature.SYNCHRONIZATION, null)
        }
    }

    private fun configureForCurrency(currencyUnit: CurrencyUnit) {
        binding.Amount.setFractionDigits(currencyUnit.fractionDigits)
        binding.Criterion.setFractionDigits(currencyUnit.fractionDigits)
        setExchangeRateVisibility(currencyUnit)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(Menu.NONE, R.id.EXCLUDE_FROM_TOTALS_COMMAND, 0, R.string.menu_exclude_from_totals).isCheckable =
            true
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.EXCLUDE_FROM_TOTALS_COMMAND)
        if (item == null) {
            CrashHandler.report(NullPointerException("EXCLUDE_FROM_TOTALS_COMMAND menu item not found"))
        } else {
            item.isChecked = excludeFromTotals
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        }
        when (command) {
            R.id.EXCLUDE_FROM_TOTALS_COMMAND -> {
                excludeFromTotals = !excludeFromTotals
                return true
            }
            R.id.SYNC_UNLINK_COMMAND -> {
                uuid?.let { uuid ->
                    syncViewModel.syncUnlink(uuid).observe(this) { result ->
                        result.onSuccess {
                            syncSpinner.setSelection(0)
                            syncSpinner.isEnabled = true
                            binding.SyncUnlink.visibility = View.GONE
                        }.onFailure {
                            showSnackBar(it.safeMessage)
                        }
                    }
                }
                return true
            }
            R.id.SYNC_SETTINGS_COMMAND -> {
                val i = Intent(this, ManageSyncBackends::class.java).apply {
                    putExtra(KEY_UUID, uuid)
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
        DialogUtils.showSyncUnlinkConfirmationDialog(this, syncAccountName, uuid)
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        if (!mNewInstance && syncSpinner.selectedItemPosition > 0) {
            showSnackBar(R.string.progress_dialog_checking_sync_backend)
            uuid?.let { uuid ->
                syncViewModel.syncCheck(uuid, syncSpinner.selectedItem as String).observe(this) { result ->
                    result.onFailure {
                    syncSpinner.setSelection(0)
                    showHelp(it.safeMessage)
                }
            } }
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
                .colorPreset(color)
                .show(this, EDIT_COLOR_DIALOG)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (EDIT_COLOR_DIALOG == dialogTag && which == OnDialogResultListener.BUTTON_POSITIVE) {
            color = extras.getInt(SimpleColorDialog.COLOR)
            UiUtils.setBackgroundOnButton(binding.colorInput.ColorIndicator, color)
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