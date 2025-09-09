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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.evernote.android.state.State
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.color.SimpleColorDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.apache.commons.lang3.ArrayUtils
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.CurrencyAdapter
import org.totschnig.myexpenses.adapter.GroupedSpinnerAdapter
import org.totschnig.myexpenses.adapter.SpinnerItem
import org.totschnig.myexpenses.databinding.OneAccountBinding
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.dialog.addAllAccountTypes
import org.totschnig.myexpenses.dialog.buildColorDialog
import org.totschnig.myexpenses.dialog.configureCurrencySpinner
import org.totschnig.myexpenses.dialog.configureTypeSpinner
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccountNames
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.ExchangeRateEdit
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.ui.bindListener
import org.totschnig.myexpenses.ui.setColor
import org.totschnig.myexpenses.util.calculateRawExchangeRate
import org.totschnig.myexpenses.util.calculateRealExchangeRate
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.util.setEnabledAndVisible
import org.totschnig.myexpenses.util.ui.addChipsBulk
import org.totschnig.myexpenses.viewmodel.AccountEditViewModel
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.SyncBackendViewModel
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.Currency.Companion.create
import org.totschnig.myexpenses.viewmodel.data.Tag
import java.io.Serializable
import java.math.BigDecimal

/**
 * Activity for editing an account
 *
 * @author Michael Totschnig
 */
class AccountEdit : AmountActivity<AccountEditViewModel>(), ExchangeRateEdit.Host,
    AdapterView.OnItemSelectedListener, ContribIFace, OnDialogResultListener {
    lateinit var binding: OneAccountBinding
    private lateinit var currencySpinner: SpinnerHelper
    private lateinit var accountTypeSpinner: SpinnerHelper
    private lateinit var syncSpinner: SpinnerHelper
    private lateinit var currencyAdapter: CurrencyAdapter
    private lateinit var accountTypeAdapter: GroupedSpinnerAdapter<Boolean, AccountType>
    private lateinit var currencyViewModel: CurrencyViewModel
    private lateinit var syncViewModel: SyncBackendViewModel

    @State
    var dataLoaded: Boolean = false

    @State
    var syncAccountName: String? = null

    @State
    lateinit var currencyUnit: CurrencyUnit

    @State
    var accountType = 0L

    @State
    var excludeFromTotals = false

    @State
    var dynamicExchangeRates = false

    @State
    var uuid: String? = null

    val rowId: Long
        get() = intent.getLongExtra(KEY_ROWID, 0)

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OneAccountBinding.inflate(layoutInflater)
        binding.TagRow.TagLabel.setText(R.string.active_tags)
        setContentView(binding.root)
        floatingActionButton = binding.fab.CREATECOMMAND

        setupToolbarWithClose()
        val viewModelProvider = ViewModelProvider(this)
        currencyViewModel = viewModelProvider[CurrencyViewModel::class.java]
        viewModel = viewModelProvider[AccountEditViewModel::class.java]
        syncViewModel = viewModelProvider[SyncBackendViewModel::class.java]
        with(injector) {
            inject(viewModel)
            inject(currencyViewModel)
            inject(syncViewModel)
        }

        currencySpinner = SpinnerHelper(binding.Currency)
        currencyAdapter = binding.Currency.configureCurrencySpinner()
        currencySpinner.adapter = currencyAdapter

        accountTypeSpinner = SpinnerHelper(binding.AccountType)
        accountTypeAdapter = binding.AccountType.configureTypeSpinner()

        syncSpinner = SpinnerHelper(binding.Sync)

        newInstance = rowId == 0L
        setTitle(if (rowId != 0L) R.string.menu_edit_account else R.string.menu_create_account)
        if (savedInstanceState == null || !dataLoaded) {
            if (rowId != 0L) {
                viewModel.loadAccount(rowId).observe(this) {
                    if (it != null) {
                        populateFields(it)
                    } else {
                        Toast.makeText(this, "Error loading account", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
                viewModel.loadTags(rowId)
            } else {
                populateFields(
                    Account(currency = currencyViewModel.default.code, type = AccountType.CASH)
                )
            }
        } else {
            configureForCurrency(currencyUnit)
        }
        linkInputsWithLabels()
        viewModel.tagsLiveData.observe(this) {
            showTags(it) { tag ->
                viewModel.removeTag(tag)
                setDirty()
            }
        }
        binding.colorInput.bindListener {
            buildColorDialog(this, color).show(this, EDIT_COLOR_DIALOG)
        }
        binding.SyncUnlink.setOnClickListener {
            DialogUtils.showSyncUnlinkConfirmationDialog(this, syncAccountName, uuid)
        }
        with(binding.SyncHelp) {
            contentDescription =
                getString(R.string.synchronization) + ": " + getString(R.string.menu_help)
            setOnClickListener {
                showHelp(getString(R.string.form_synchronization_help_text_add))
            }
        }
        binding.TagRow.bindListener()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setup(true)
    }

    private fun setup(fromSavedState: Boolean) {
        configureSyncBackendAdapter(fromSavedState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                currencyViewModel.currencies.collect { currencies: List<Currency?> ->
                    currencyAdapter.addAll(currencies)
                    currencySpinner.setSelection(
                        currencyAdapter.getPosition(
                            create(currencyUnit.code, this@AccountEdit)
                        )
                    )
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.accountTypes.collect { accountTypes ->
                    accountTypeAdapter.addAllAccountTypes(accountTypes)
                    (accountType.takeIf { it != 0L } ?: accountTypes.find { it.isCashAccount }?.id)?.let {
                        accountTypeSpinner.setSelection(accountTypeAdapter.getPosition(it))
                    }
                }
            }
        }
        binding.colorInput.setColor(color)
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

    private fun configureSyncBackendAdapter(fromSavedState: Boolean) {
        val syncBackendAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item,
            ArrayUtils.insert(0, getAccountNames(this), getString(R.string.synchronization_none))
        )
        syncBackendAdapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
        syncSpinner.adapter = syncBackendAdapter
        if (syncAccountName != null) {
            val position = syncBackendAdapter.getPosition(syncAccountName)
            if (position > -1) {
                syncSpinner.setSelection(position)
                if (!fromSavedState) {
                    syncSpinner.isEnabled = false
                    binding.SyncUnlink.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * populates the input field either from the database or with default value for currency (from Locale)
     */
    private fun populateFields(account: Account) {
        binding.Label.setText(account.label)
        binding.Description.setText(account.description)
        syncAccountName = account.syncAccountName
        currencyUnit = currencyContext[account.currency]
        accountType = account.type.id
        color = account.color
        excludeFromTotals = account.excludeFromTotals
        dynamicExchangeRates = account.dynamicExchangeRates
        uuid = account.uuid
        dataLoaded = true
        binding.ERR.ExchangeRate.setRate(
            calculateRealExchangeRate(
                account.exchangeRate,
                currencyUnit,
                homeCurrency
            ), true
        )
        configureForCurrency(currencyUnit)
        binding.Amount.setAmount(Money(currencyUnit, account.openingBalance).amountMajor)
        accountTypeSpinner.setSelection(accountTypeAdapter.getPosition(account.type))
        val criterion = account.criterion
        if (criterion != null) {
            binding.Criterion.setAmount(Money(currencyUnit, account.criterion).amountMajor)
            updateCriterionLabel()
        }
        setup(false)
    }

    private fun showTags(tags: Iterable<Tag>?, closeFunction: (Tag) -> Unit) {
        with(binding.TagRow.TagGroup) {
            removeAllViews()
            tags?.let { addChipsBulk(it, closeFunction) }
        }
    }

    private fun setExchangeRateVisibility(currencyUnit: CurrencyUnit) {
        val isHomeAccount = currencyUnit.code == homeCurrency.code
        binding.ERR.root.visibility = if (isHomeAccount) View.GONE else View.VISIBLE
        if (!isHomeAccount) {
            binding.ERR.ExchangeRate.setCurrencies(
                currencyUnit,
                homeCurrency
            )
        }
    }

    /**
     * validates currency (must be code from ISO 4217) and opening balance
     * (a valid float according to the format from the locale)
     */
    override fun saveState() {
        if (!dataLoaded) return
        val label = binding.Label.text.toString()
        if (label == "") {
            binding.Label.error = getString(R.string.required)
            return
        }
        val openingBalance: BigDecimal = validateAmountInput(true) ?: return
        val currency = (currencySpinner.selectedItem as Currency).code
        val currencyUnit = currencyContext[currency]
        val isForeignExchange = currencyContext.homeCurrencyString != currency
        @Suppress("UNCHECKED_CAST") val account = Account(
            id = rowId,
            label = label,
            currency = currency,
            openingBalance = Money(currencyUnit, openingBalance).amountMinor,
            description = binding.Description.text.toString(),
            type = (accountTypeSpinner.selectedItem as SpinnerItem.Item<AccountType>).data,
            color = color,
            uuid = uuid,
            syncAccountName = if (syncSpinner.selectedItemPosition > 0) syncSpinner.selectedItem as String else null,
            criterion = Money(currencyUnit, binding.Criterion.typedValue).amountMinor,
            excludeFromTotals = excludeFromTotals,
            dynamicExchangeRates = dynamicExchangeRates && isForeignExchange,
            exchangeRate = (if (isForeignExchange) {
                binding.ERR.ExchangeRate.getRate(false)?.let {
                    calculateRawExchangeRate(it, currencyUnit, homeCurrency)
                }
            } else null) ?: 1.0
        )
        super.saveState()
        viewModel.save(account).observe(this) { result ->
            result.onFailure {
                CrashHandler.report(it)
                showSnackBar(it.safeMessage)
            }.onSuccess { (id, uuid) ->
                account.syncAccountName?.let {
                    requestSync(
                        accountName = it,
                        uuid = uuid
                    )
                }
                setResult(RESULT_OK, Intent().apply {
                    putExtra(KEY_ROWID, id)
                })
                currencyContext.ensureFractionDigitsAreCached(currencyUnit)
                finish()
            }
            isSaving = false
        }
    }

    override fun onItemSelected(
        parent: AdapterView<*>, view: View?, position: Int,
        id: Long,
    ) {
        setDirty()
        val parentId = parent.id
        when (parentId) {
            R.id.Currency -> {
                try {
                    (currencySpinner.selectedItem as? Currency)?.code?.let {
                        currencyUnit = currencyContext[it]
                        configureForCurrency(currencyUnit)
                    }
                } catch (_: IllegalArgumentException) {
                    //will be reported to user when he tries so safe
                }
            }
            R.id.Sync -> {
                if (position > 0) {
                    contribFeatureRequested(ContribFeature.SYNCHRONIZATION)
                } else {
                    syncAccountName = null
                }
            }
            R.id.AccountType -> {
                if (id > 0L) {
                    accountType = id
                }
            }
        }
    }

    private fun configureForCurrency(currencyUnit: CurrencyUnit) {
        binding.Amount.setFractionDigits(currencyUnit.fractionDigits)
        binding.Criterion.setFractionDigits(currencyUnit.fractionDigits)
        setExchangeRateVisibility(currencyUnit)
        invalidateOptionsMenu()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.account_edit, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.EXCLUDE_FROM_TOTALS_COMMAND).isChecked = excludeFromTotals
        lifecycleScope.launch {
            with(menu.findItem(R.id.DYNAMIC_EXCHANGE_RATE_COMMAND)) {
                if (viewModel.dynamicExchangeRatesPerAccount.first()) {
                    if (::currencyUnit.isInitialized) {
                        val isFX = currencyUnit.code != homeCurrency.code
                        this.setEnabledAndVisible(isFX)
                        isChecked = isFX && dynamicExchangeRates
                    }
                } else {
                    this.setEnabledAndVisible(false)
                }
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override val fabActionName = "SAVE_ACCOUNT"

    override fun dispatchCommand(command: Int, tag: Any?) =
        super.dispatchCommand(command, tag) || when (command) {
            R.id.EXCLUDE_FROM_TOTALS_COMMAND -> {
                excludeFromTotals = !excludeFromTotals
                setDirty()
                true
            }

            R.id.DYNAMIC_EXCHANGE_RATE_COMMAND -> {
                dynamicExchangeRates = !dynamicExchangeRates
                setDirty()
                true
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
                true
            }

            R.id.SYNC_SETTINGS_COMMAND -> {
                syncSettings.launch(
                    Intent(this, ManageSyncBackends::class.java).apply {
                        putExtra(KEY_UUID, uuid)
                    }
                )
                true
            }

            else -> false
        }

    private val syncSettings =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_FIRST_USER) {
                finish()
            } else {
                configureSyncBackendAdapter(true)
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

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        if (syncSpinner.selectedItemPosition > 0) {
            val syncAccountName = syncSpinner.selectedItem as String
            if (!newInstance) {
                showSnackBar(R.string.progress_dialog_checking_sync_backend)
                uuid?.let { uuid ->
                    syncViewModel.syncCheck(uuid, syncAccountName)
                        .observe(this) { result ->
                            result.onFailure {
                                syncSpinner.setSelection(0)
                                showHelp(it.safeMessage)
                            }.onSuccess {
                                this.syncAccountName = syncAccountName
                            }
                        }
                }
            } else {
                this.syncAccountName = syncAccountName
            }
        }
    }

    override fun contribFeatureNotCalled(feature: ContribFeature) {
        if (feature === ContribFeature.SYNCHRONIZATION) {
            syncSpinner.setSelection(0)
        }
    }

    private fun showHelp(message: String) {
        MessageDialogFragment.newInstance(
            null,
            message,
            MessageDialogFragment.Button(
                R.string.pref_category_title_manage,
                R.id.SYNC_SETTINGS_COMMAND,
                null
            ),
            MessageDialogFragment.okButton(),
            null
        )
            .show(supportFragmentManager, "SYNC_HELP")
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (EDIT_COLOR_DIALOG == dialogTag && which == BUTTON_POSITIVE) {
            color = extras.getInt(SimpleColorDialog.COLOR)
            if (!maybeApplyDynamicColor()) {
                binding.colorInput.setColor(color)
            }
            return true
        }
        return false
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