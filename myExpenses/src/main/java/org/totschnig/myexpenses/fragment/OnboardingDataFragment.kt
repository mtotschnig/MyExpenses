package org.totschnig.myexpenses.fragment

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.color.SimpleColorDialog
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BackupRestoreActivity
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.adapter.SpinnerItem
import org.totschnig.myexpenses.databinding.OnboardingWizzardDataBinding
import org.totschnig.myexpenses.dialog.addAllAccountTypes
import org.totschnig.myexpenses.dialog.buildColorDialog
import org.totschnig.myexpenses.dialog.configureCurrencySpinner
import org.totschnig.myexpenses.dialog.configureTypeSpinner
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccountNames
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.ui.bindListener
import org.totschnig.myexpenses.ui.setColor
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.OnBoardingDataViewModel
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.Currency.Companion.create
import java.math.BigDecimal
import javax.inject.Inject

class OnboardingDataFragment : OnboardingFragment(), AdapterView.OnItemSelectedListener,
    OnDialogResultListener {
    private var _binding: OnboardingWizzardDataBinding? = null
    private val binding get() = _binding!!
    private lateinit var accountTypeSpinner: SpinnerHelper

    @Inject
    lateinit var currencyContext: CurrencyContext

    private val currencyViewModel: CurrencyViewModel by viewModels()
    val viewModel: OnBoardingDataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with((requireActivity().application as MyApplication).appComponent) {
            inject(this@OnboardingDataFragment)
            inject(currencyViewModel)
            inject(viewModel)
        }
        viewModel.accountSave.observe(this) { result ->
            result.onSuccess {
                hostActivity.start()
            }.onFailure {
                CrashHandler.report(it)
                hostActivity.showSnackBar(it.safeMessage)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val selectedItem = binding.Currency.selectedItem as? Currency
        if (selectedItem != null) {
            outState.putString(DatabaseConstants.KEY_CURRENCY, selectedItem.code)
        }
        val label = binding.Label.text.toString()
        outState.putBoolean(
            KEY_LABEL_UNCHANGED_OR_EMPTY,
            TextUtils.isEmpty(label) || label == getString(R.string.default_account_name)
        )
    }

    override val navigationButtonId = R.id.suw_navbar_done

    override fun onNextButtonClicked() {
        hostActivity.doWithEncryptionCheck {
            buildAccount()?.let {
                prefHandler.putString(PrefKey.HOME_CURRENCY, selectedCurrency.code)
                viewModel.saveAccount(it)
            }
        }
    }

    override val menuResId = R.menu.onboarding_data

    override fun setupMenu() {
        toolbar.menu.findItem(R.id.SetupFromRemote).subMenu?.let {
            it.clear()
            hostActivity.addSyncProviderMenuEntries(it)
            for (account in getAccountNames(requireActivity())) {
                it.add(Menu.NONE, Menu.NONE, Menu.NONE, account)
            }
        }
        toolbar.setOnMenuItemClickListener { item: MenuItem -> onRestoreMenuItemSelected(item) }
    }

    private fun onRestoreMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.SetupFromLocal -> {
                hostActivity.withResultCallbackLauncher.launch(
                    Intent(activity, BackupRestoreActivity::class.java).apply {
                        action = BackupRestoreActivity.ACTION_RESTORE
                    }
                )
            }
            Menu.NONE -> {
                hostActivity.fetchAccountData(item.title.toString())
            }
            R.id.Banking -> {
                hostActivity.contribFeatureCalled(ContribFeature.BANKING, null)
            }
            !in arrayOf(R.id.SetupMain, R.id.SetupFromRemote) -> {
                hostActivity.startSetup(item.itemId)
            }
        }
        return true
    }

    override val layoutResId = R.layout.onboarding_wizzard_data

    override fun bindView(view: View) {
        _binding = OnboardingWizzardDataBinding.bind(view)
        binding.MoreOptionsButton.setOnClickListener {
            viewModel.moreOptionsShown = true
            showMoreOptions()
        }
        accountTypeSpinner = SpinnerHelper(binding.AccountType).also {
            it.setOnItemSelectedListener(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun configureView(savedInstanceState: Bundle?) {
        //label
        setDefaultLabel()

        //amount
        binding.Amount.setFractionDigits(2)
        binding.Amount.setAmount(BigDecimal.ZERO)
        binding.Amount.findViewById<View>(R.id.Calculator).visibility = View.GONE

        //currency
        val currencyAdapter = binding.Currency.configureCurrencySpinner(this)
        val code = savedInstanceState?.getString(DatabaseConstants.KEY_CURRENCY)
        val currency =
            if (code != null) create(code, requireActivity()) else currencyViewModel.default
        currencyAdapter.clear()
        currencyAdapter.addAll(currencyViewModel.currenciesFromEnum)
        binding.Currency.setSelection(currencyAdapter.getPosition(currency))
        nextButton.visibility = View.VISIBLE

        //type
        val typeAdapter = binding.AccountType.configureTypeSpinner()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.accountTypes.collect { accountTypes ->
                    typeAdapter.addAllAccountTypes(accountTypes)
                    accountTypes.find { it.isCashAccount }?.let {
                        binding.AccountType.setSelection(typeAdapter.getPosition(it.id))
                    }
                }
            }
        }

        //color
        binding.colorInput.setColor(viewModel.accountColor)

        if (viewModel.moreOptionsShown) {
            showMoreOptions()
        }

        binding.colorInput.bindListener {
            buildColorDialog(requireContext(), viewModel.accountColor)
                .show(this, ProtectedFragmentActivity.EDIT_COLOR_DIALOG)
        }
    }

    override val title: CharSequence
        get() = getString(R.string.onboarding_data_title)

    private fun setDefaultLabel() {
        binding.Label.setText(R.string.default_account_name)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(KEY_LABEL_UNCHANGED_OR_EMPTY)) {
                setDefaultLabel()
            }
        }
    }

    private fun showMoreOptions() {
        binding.MoreOptionsButton.visibility = View.GONE
        binding.MoreOptionsContainer.visibility = View.VISIBLE
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (parent.id == R.id.Currency) {
            binding.Amount.setFractionDigits(selectedCurrency.fractionDigits)
        }
    }

    private val selectedCurrency
        get() = currencyContext[(binding.Currency.selectedItem as Currency).code]

    override fun onNothingSelected(parent: AdapterView<*>?) {}
    private fun buildAccount(): Account? {
        val currency = selectedCurrency
        return binding.Amount.getAmount(currency, showToUser = true).getOrNull()?.let { money ->
            @Suppress("UNCHECKED_CAST")
            Account(
                label = binding.Label.text.toString().takeIf { it.isNotEmpty() }
                    ?: getString(R.string.default_account_name),
                currency = currency.code,
                openingBalance = money.amountMinor,
                description = binding.Description.text.toString(),
                type = (accountTypeSpinner.selectedItem as SpinnerItem.Item<AccountType>).data,
                color = viewModel.accountColor
            )
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (ProtectedFragmentActivity.EDIT_COLOR_DIALOG == dialogTag && which == OnDialogResultListener.BUTTON_POSITIVE) {
            with(extras.getInt(SimpleColorDialog.COLOR)) {
                viewModel.accountColor = this
                binding.colorInput.setColor(this)
            }
            return true
        }
        return false
    }

    companion object {
        private const val KEY_LABEL_UNCHANGED_OR_EMPTY = "label_unchanged_or_empty"
        fun newInstance(): OnboardingDataFragment {
            return OnboardingDataFragment()
        }
    }
}