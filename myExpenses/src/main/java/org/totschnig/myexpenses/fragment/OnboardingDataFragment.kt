package org.totschnig.myexpenses.fragment

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.viewModels
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.color.SimpleColorDialog
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BackupRestoreActivity
import org.totschnig.myexpenses.activity.OnboardingActivity
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.activity.RESTORE_REQUEST
import org.totschnig.myexpenses.activity.SyncBackendSetupActivity
import org.totschnig.myexpenses.adapter.CurrencyAdapter
import org.totschnig.myexpenses.databinding.OnboardingWizzardDataBinding
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccountNames
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.OnBoardingViewModel
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.Currency.Companion.create
import java.math.BigDecimal
import javax.inject.Inject

class OnboardingDataFragment : OnboardingFragment(), AdapterView.OnItemSelectedListener,
    OnDialogResultListener {
    private var _binding: OnboardingWizzardDataBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var currencyContext: CurrencyContext
    
    @Inject
    lateinit var prefHandler: PrefHandler
    
    val currencyViewModel: CurrencyViewModel by viewModels()
    val viewModel: OnBoardingViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with((requireActivity().application as MyApplication).appComponent) {
            inject(this@OnboardingDataFragment)
            inject(currencyViewModel)
            inject(viewModel)
        }
        viewModel.accountSave.observe(this) {
            if (it) {
                (requireActivity() as OnboardingActivity).start()
            }  else {
                val message = "Unknown error while setting up account"
                CrashHandler.report(Exception(message))
                (requireActivity() as OnboardingActivity).showSnackBar(message)
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

    override fun getNavigationButtonId(): Int {
        return R.id.suw_navbar_done
    }

    override fun onNextButtonClicked() {
        prefHandler.putString(PrefKey.HOME_CURRENCY, validateSelectedCurrency().code)
        viewModel.saveAccount(buildAccount())
    }

    override fun getMenuResId(): Int {
        return R.menu.onboarding_data
    }

    public override fun setupMenu() {
        toolbar.menu.findItem(R.id.SetupFromRemote).subMenu?.let {
            it.clear()
            (requireActivity() as SyncBackendSetupActivity).addSyncProviderMenuEntries(it)
            for (account in getAccountNames(requireActivity())) {
                it.add(Menu.NONE, Menu.NONE, Menu.NONE, account)
            }
        }
        toolbar.setOnMenuItemClickListener { item: MenuItem -> onRestoreMenuItemSelected(item) }
    }

    private fun onRestoreMenuItemSelected(item: MenuItem): Boolean {
        val hostActivity = requireActivity() as SyncBackendSetupActivity
        if (item.itemId == R.id.SetupFromLocal) {
            val intent = Intent(activity, BackupRestoreActivity::class.java)
            intent.action = BackupRestoreActivity.ACTION_RESTORE
            hostActivity.startActivityForResult(intent, RESTORE_REQUEST)
        } else {
            if (item.itemId == Menu.NONE) {
                hostActivity.fetchAccountData(item.title.toString())
            } else {
                hostActivity.startSetup(item.itemId)
            }
        }
        return true
    }

    override fun getLayoutResId(): Int {
        return R.layout.onboarding_wizzard_data
    }

    public override fun bindView(view: View) {
        _binding = OnboardingWizzardDataBinding.bind(view)
        binding.MoreOptionsButton.setOnClickListener {
            viewModel.moreOptionsShown = true
            showMoreOptions()
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
        DialogUtils.configureCurrencySpinner(binding.Currency, this)
        val code =
            if (savedInstanceState != null) savedInstanceState[DatabaseConstants.KEY_CURRENCY] as String? else null
        val currency =
            if (code != null) create(code, requireActivity()) else currencyViewModel.default
        currencyViewModel.getCurrencies().observe(this) { currencies: List<Currency?> ->
            val adapter = binding.Currency.adapter as CurrencyAdapter
            adapter.clear()
            adapter.addAll(currencies)
            binding.Currency.setSelection(adapter.getPosition(currency))
            nextButton.visibility = View.VISIBLE
        }

        //type
        DialogUtils.configureTypeSpinner(binding.AccountType)

        //color
        UiUtils.setBackgroundOnButton(binding.colorInput.ColorIndicator, viewModel.accountColor)
        if (viewModel.moreOptionsShown) {
            showMoreOptions()
        }
    }

    override fun getTitle(): CharSequence {
        return getString(R.string.onboarding_data_title)
    }

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
            binding.Amount.setFractionDigits(validateSelectedCurrency().fractionDigits)
        }
    }

    private fun validateSelectedCurrency(): CurrencyUnit {
        val currency = (binding.Currency.selectedItem as Currency).code
        return currencyContext[currency]
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}
    fun buildAccount(): Account {
        var label = binding.Label.text.toString()
        if (TextUtils.isEmpty(label)) {
            label = getString(R.string.default_account_name)
        }
        val openingBalance = binding.Amount.typedValue
        val currency = validateSelectedCurrency()
        return Account(
            label, currency, Money(currency, openingBalance),
            binding.Description.text.toString(),
            binding.AccountType.selectedItem as AccountType, viewModel.accountColor
        )
    }

    fun editAccountColor() {
        SimpleColorDialog.build()
            .allowCustom(true)
            .cancelable(false)
            .neut()
            .colorPreset(viewModel.accountColor)
            .show(this, ProtectedFragmentActivity.EDIT_COLOR_DIALOG)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (ProtectedFragmentActivity.EDIT_COLOR_DIALOG == dialogTag && which == OnDialogResultListener.BUTTON_POSITIVE) {
            with(extras.getInt(SimpleColorDialog.COLOR)) {
                viewModel.accountColor = this
                UiUtils.setBackgroundOnButton(binding.colorInput.ColorIndicator, this)
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