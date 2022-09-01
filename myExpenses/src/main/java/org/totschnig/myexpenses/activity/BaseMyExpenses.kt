package org.totschnig.myexpenses.activity

import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager.widget.ViewPager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.AmountEdit
import eltos.simpledialogfragment.form.Hint
import eltos.simpledialogfragment.form.SimpleFormDialog
import eltos.simpledialogfragment.form.Spinner
import icepick.State
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit.Companion.KEY_OCR_RESULT
import org.totschnig.myexpenses.adapter.MyViewPagerAdapter
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.databinding.ActivityMainBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ExportDialogFragment
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.dialog.ProgressDialogFragment
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.OcrHost
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.feature.OcrResultFlat
import org.totschnig.myexpenses.feature.Payee
import org.totschnig.myexpenses.fragment.BaseTransactionList.KEY_FILTER
import org.totschnig.myexpenses.fragment.TransactionList
import org.totschnig.myexpenses.model.AggregateAccount
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.enableAutoFill
import org.totschnig.myexpenses.preference.requireString
import org.totschnig.myexpenses.provider.CheckSealedHandler
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.Criteria
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.ui.DiscoveryHelper
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.AppDirHelper.ensureContentUri
import org.totschnig.myexpenses.util.ContribUtils
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.util.distrib.ReviewManager
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.AccountSealedException
import org.totschnig.myexpenses.viewmodel.ExportViewModel
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel
import org.totschnig.myexpenses.viewmodel.UpgradeHandlerViewModel
import se.emilsjolander.stickylistheaders.ExpandableStickyListHeadersListView
import timber.log.Timber
import java.io.File
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import javax.inject.Inject


const val DIALOG_TAG_OCR_DISAMBIGUATE = "DISAMBIGUATE"
const val DIALOG_TAG_NEW_BALANCE = "NEW_BALANCE"

abstract class BaseMyExpenses : LaunchActivity(), OcrHost, OnDialogResultListener, ContribIFace {
    @JvmField
    @State
    var scanFile: File? = null

    @JvmField
    @State
    var accountId: Long = 0

    var currentCurrency: String? = null

    val currentCurrencyUnit: CurrencyUnit?
        get() = currentCurrency?.let { currencyContext.get(it) }

    @Inject
    lateinit var discoveryHelper: IDiscoveryHelper

    @Inject
    lateinit var reviewManager: ReviewManager

    var accountsCursor: Cursor? = null
    fun requireAccountsCursor() = accountsCursor!!
    lateinit var toolbar: Toolbar

    var columnIndexType = 0

    private var currentBalance: String? = null
    var currentPosition = -1

    val viewModel: MyExpensesViewModel by viewModels()
    private val upgradeHandlerViewModel: UpgradeHandlerViewModel by viewModels()
    private val exportViewModel: ExportViewModel by viewModels()

    lateinit var binding: ActivityMainBinding
    protected var pagerAdapter: MyViewPagerAdapter? = null

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (savedInstanceState == null) {
            floatingActionButton?.let {
                discoveryHelper.discover(
                    this,
                    it,
                    3,
                    DiscoveryHelper.Feature.fab_long_press
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with((applicationContext as MyApplication).appComponent) {
            inject(viewModel)
            inject(upgradeHandlerViewModel)
            inject(exportViewModel)
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                upgradeHandlerViewModel.upgradeInfo.collect { info ->
                    info?.let {
                        showDismissibleSnackBar(it, object : Snackbar.Callback() {
                            override fun onDismissed(
                                transientBottomBar: Snackbar,
                                event: Int
                            ) {
                                if (event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_ACTION)
                                    upgradeHandlerViewModel.messageShown()
                            }
                        })
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                exportViewModel.publishProgress.collect { progress ->
                    progress?.let {
                        progressDialogFragment?.appendToMessage(progress)
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                exportViewModel.result.collect { result ->
                    result?.let {
                        progressDialogFragment?.onTaskCompleted()
                        if (result.second.isNotEmpty()) {
                            shareExport(result.first, result.second)
                        }
                        exportViewModel.resultProcessed()
                    }
                }
            }
        }
    }

    override fun injectDependencies() {
        (applicationContext as MyApplication).appComponent.inject(this)
    }

    override fun onFeatureAvailable(feature: Feature) {
        if (feature == Feature.OCR) {
            activateOcrMode()
        }
    }

    private fun displayDateCandidate(pair: Pair<LocalDate, LocalTime?>) =
        (pair.second?.let { pair.first.atTime(pair.second) } ?: pair.first).toString()

    override fun processOcrResult(result: Result<OcrResult>) {
        result.onSuccess {
            if (it.needsDisambiguation()) {
                SimpleFormDialog.build()
                    .cancelable(false)
                    .autofocus(false)
                    .neg(android.R.string.cancel)
                    .extra(Bundle().apply {
                        putParcelable(KEY_OCR_RESULT, it)
                    })
                    .title(getString(R.string.scan_result_multiple_candidates_dialog_title))
                    .fields(
                        when (it.amountCandidates.size) {
                            0 -> Hint.plain(getString(R.string.scan_result_no_amount))
                            1 -> Hint.plain(
                                "%s: %s".format(
                                    getString(R.string.amount),
                                    it.amountCandidates[0]
                                )
                            )
                            else -> Spinner.plain(KEY_AMOUNT)
                                .placeholder(R.string.amount)
                                .items(*it.amountCandidates.toTypedArray())
                                .preset(0)
                        },
                        when (it.dateCandidates.size) {
                            0 -> Hint.plain(getString(R.string.scan_result_no_date))
                            1 -> Hint.plain(
                                "%s: %s".format(
                                    getString(R.string.date),
                                    displayDateCandidate(it.dateCandidates[0])
                                )
                            )
                            else -> Spinner.plain(KEY_DATE)
                                .placeholder(R.string.date)
                                .items(
                                    *it.dateCandidates.map(this::displayDateCandidate)
                                        .toTypedArray()
                                )
                                .preset(0)
                        },
                        when (it.payeeCandidates.size) {
                            0 -> Hint.plain(getString(R.string.scan_result_no_payee))
                            1 -> Hint.plain(
                                "%s: %s".format(
                                    getString(R.string.payee),
                                    it.payeeCandidates[0].name
                                )
                            )
                            else -> Spinner.plain(KEY_PAYEE_NAME)
                                .placeholder(R.string.payee)
                                .items(*it.payeeCandidates.map(Payee::name).toTypedArray())
                                .preset(0)
                        }
                    )
                    .show(this, DIALOG_TAG_OCR_DISAMBIGUATE)
            } else {
                startEditFromOcrResult(
                    if (it.isEmpty()) {
                        Toast.makeText(
                            this,
                            getString(R.string.scan_result_no_data),
                            Toast.LENGTH_LONG
                        ).show()
                        null
                    } else {
                        it.selectCandidates()
                    }
                )
            }
        }.onFailure {
            Timber.e(it)
            Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * start ExpenseEdit Activity for a new transaction/transfer/split
     * Originally the form for transaction is rendered, user can change from spinner in toolbar
     */
    open fun createRowIntent(type: Int, isIncome: Boolean) =
        Intent(this, ExpenseEdit::class.java).apply {
            putExtra(Transactions.OPERATION_TYPE, type)
            putExtra(ExpenseEdit.KEY_INCOME, isIncome)
            //if we are called from an aggregate cursor, we also hand over the currency
            if (accountId < 0) {
                putExtra(KEY_CURRENCY, currentCurrency)
                putExtra(ExpenseEdit.KEY_AUTOFILL_MAY_SET_ACCOUNT, true)
            } else {
                //if accountId is 0 ExpenseEdit will retrieve the first entry from the accounts table
                putExtra(KEY_ACCOUNTID, accountId)
            }
        }

    private fun createRow(type: Int, isIncome: Boolean) {
        if (type == Transactions.TYPE_SPLIT) {
            contribFeatureRequested(ContribFeature.SPLIT_TRANSACTION, null)
        } else {
            createRowDo(type, isIncome)
        }
    }

    fun createRowDo(type: Int, isIncome: Boolean) {
        startEdit(createRowIntent(type, isIncome))
    }

    private fun startEdit(intent: Intent?) {
        floatingActionButton?.hide()
        startActivityForResult(intent, EDIT_REQUEST)
    }

    private fun startEditFromOcrResult(result: OcrResultFlat?) {
        recordUsage(ContribFeature.OCR)
        startEdit(
            createRowIntent(Transactions.TYPE_TRANSACTION, false).apply {
                putExtra(KEY_OCR_RESULT, result)
                putExtra(KEY_PICTURE_URI, Uri.fromFile(scanFile))
            }
        )
    }

    fun ensureAccountCursorAtCurrentPosition() =
        accountsCursor?.takeIf { it.moveToPosition(currentPosition) }


    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == OnDialogResultListener.BUTTON_POSITIVE) {
            when (dialogTag) {
                DIALOG_TAG_OCR_DISAMBIGUATE -> {
                    startEditFromOcrResult(
                        extras.getParcelable<OcrResult>(KEY_OCR_RESULT)!!.selectCandidates(
                            extras.getInt(KEY_AMOUNT),
                            extras.getInt(KEY_DATE),
                            extras.getInt(KEY_PAYEE_NAME)
                        )
                    )
                    return true
                }
                DIALOG_TAG_NEW_BALANCE -> {
                    if (currentPosition > -1) {
                        accountsCursor?.let { cursor ->
                            currentCurrencyUnit?.let { currencyUnit ->
                                cursor.moveToPosition(currentPosition)
                                startEdit(
                                    createRowIntent(Transactions.TYPE_TRANSACTION, false).apply {
                                        putExtra(
                                            KEY_AMOUNT,
                                            (extras.getSerializable(KEY_AMOUNT) as BigDecimal) -
                                                    Money(
                                                        currencyUnit,
                                                        cursor.getLong(
                                                            cursor.getColumnIndexOrThrow(
                                                                KEY_CURRENT_BALANCE
                                                            )
                                                        )
                                                    ).amountMajor
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        return false
    }

    private val shareTarget: String
        get() = prefHandler.requireString(PrefKey.SHARE_TARGET, "").trim { it <= ' ' }

    private fun shareExport(format: ExportFormat, uriList: List<Uri>) {
        shareViewModel.share(
            this, uriList,
            shareTarget,
            "text/" + format.name.lowercase(Locale.US)
        )
    }

    override fun dispatchCommand(command: Int, tag: Any?) =
        if (super.dispatchCommand(command, tag)) {
            true
        } else when (command) {
            R.id.SHARE_PDF_COMMAND -> {
                shareViewModel.share(
                    this, listOf(ensureContentUri(Uri.parse(tag as String?), this)),
                    shareTarget,
                    "application/pdf"
                )
                true
            }
            R.id.OCR_DOWNLOAD_COMMAND -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=org.totschnig.ocr.tesseract")
                }
                packageManager.queryIntentActivities(intent, 0)
                    .find { it.activityInfo.packageName == "org.fdroid.fdroid" }
                    ?.activityInfo?.let {
                        intent.component = ComponentName(it.applicationInfo.packageName, it.name)
                        startActivity(intent)
                    }
                    ?: run {
                        Toast.makeText(this, "F-Droid not installed", Toast.LENGTH_LONG).show()
                    }
                true
            }
            R.id.DELETE_ACCOUNT_COMMAND_DO -> {
                //reset mAccountId will prevent the now defunct account being used in an immediately following "new transaction"
                val accountIds = tag as Array<Long>
                if (accountIds.any { it == accountId }) {
                    accountId = 0
                }
                val manageHiddenFragment =
                    supportFragmentManager.findFragmentByTag(MANAGE_HIDDEN_FRAGMENT_TAG)
                if (manageHiddenFragment != null) {
                    supportFragmentManager.beginTransaction().remove(manageHiddenFragment).commit()
                }
                showSnackBarIndefinite(R.string.progress_dialog_deleting)
                viewModel.deleteAccounts(accountIds).observe(
                    this
                ) { result ->
                    result.onSuccess {
                        showSnackBar(
                            resources.getQuantityString(
                                R.plurals.delete_success,
                                accountIds.size,
                                accountIds.size
                            )
                        )
                    }.onFailure {
                        if (it is AccountSealedException) {
                            showSnackBar(R.string.object_sealed_debt)
                        } else {
                            showDeleteFailureFeedback(null)
                        }
                    }
                }
                true
            }
            else -> false
        }

    fun setupFabSubMenu() {
        floatingActionButton?.setOnLongClickListener { fab ->
            discoveryHelper.markDiscovered(DiscoveryHelper.Feature.fab_long_press)
            val popup = PopupMenu(this, fab)
            val popupMenu = popup.menu
            popup.setOnMenuItemClickListener { item ->
                createRow(
                    when (item.itemId) {
                        R.string.split_transaction -> Transactions.TYPE_SPLIT
                        R.string.transfer -> Transactions.TYPE_TRANSFER
                        else -> Transactions.TYPE_TRANSACTION
                    }, item.itemId == R.string.income
                )
                true
            }
            popupMenu.add(Menu.NONE, R.string.expense, Menu.NONE, R.string.expense)
                .setIcon(R.drawable.ic_expense)
            popupMenu.add(Menu.NONE, R.string.income, Menu.NONE, R.string.income).icon =
                AppCompatResources.getDrawable(this, R.drawable.ic_menu_add)?.also {
                    DrawableCompat.setTint(
                        it,
                        ResourcesCompat.getColor(resources, R.color.colorIncome, null)
                    )
                }
            popupMenu.add(Menu.NONE, R.string.transfer, Menu.NONE, R.string.transfer)
                .setIcon(R.drawable.ic_menu_forward)
            popupMenu.add(
                Menu.NONE,
                R.string.split_transaction,
                Menu.NONE,
                R.string.split_transaction
            ).setIcon(R.drawable.ic_menu_split)
            //noinspection RestrictedApi
            (popup.menu as? MenuBuilder)?.setOptionalIconsVisible(true)
            popup.show()
            true
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.SCAN_MODE_COMMAND)?.let {
            it.isChecked = prefHandler.getBoolean(PrefKey.OCR, false)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    fun setupToolbarPopupMenu() {
        toolbar.setOnClickListener {
            if (currentPosition > -1) {
                val popup = PopupMenu(this, toolbar)
                val popupMenu = popup.menu
                popupMenu.add(
                    Menu.NONE,
                    R.id.COPY_TO_CLIPBOARD_COMMAND,
                    Menu.NONE,
                    R.string.copy_text
                )
                popupMenu.add(
                    Menu.NONE,
                    R.id.NEW_BALANCE_COMMAND,
                    Menu.NONE,
                    getString(R.string.new_balance)
                )
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.COPY_TO_CLIPBOARD_COMMAND -> copyToClipBoard()
                        R.id.NEW_BALANCE_COMMAND -> if (accountId > 0) {
                            SimpleFormDialog.build().fields(
                                AmountEdit.plain(KEY_AMOUNT).label(R.string.new_balance)
                                    .fractionDigits(currentCurrencyUnit!!.fractionDigits)
                            ).show(this, DIALOG_TAG_NEW_BALANCE)
                        }
                    }
                    true
                }
                popup.show()
            }
        }
    }

    fun setBalance() {
        accountsCursor?.let { cursor ->
            currentCurrencyUnit?.let { currencyUnit ->
                val balance = cursor.getLong(KEY_CURRENT_BALANCE)
                val label = cursor.getString(KEY_LABEL)
                val isHome = cursor.getInt(KEY_IS_AGGREGATE) == AggregateAccount.AGGREGATE_HOME
                currentBalance = String.format(
                    Locale.getDefault(), "%s%s", if (isHome) " ≈ " else "",
                    currencyFormatter.formatMoney(Money(currencyUnit, balance))
                )
                title = if (isHome) getString(R.string.grand_total) else label
                toolbar.subtitle = currentBalance
                toolbar.setSubtitleTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        if (balance < 0) R.color.colorExpense else R.color.colorIncome,
                        null
                    )
                )
            }
        }

    }

    private fun copyToClipBoard() {
        currentBalance?.let { copyToClipboard(it) }
    }

    fun updateFab() {
        val scanMode = isScanMode()
        requireFloatingActionButtonWithContentDescription(
            if (scanMode)
                getString(R.string.contrib_feature_ocr_label)
            else
                TextUtils.concatResStrings(
                    this,
                    ". ",
                    R.string.menu_create_transaction,
                    R.string.menu_create_transfer,
                    R.string.menu_create_split
                )
        )
        floatingActionButton!!.setImageResource(if (scanMode) R.drawable.ic_scan else R.drawable.ic_menu_add_fab)
    }

    fun isScanMode(): Boolean = prefHandler.getBoolean(PrefKey.OCR, false)

    private fun activateOcrMode() {
        prefHandler.putBoolean(PrefKey.OCR, true)
        updateFab()
        invalidateOptionsMenu()
    }

    private fun Intent.fillIntentForGroupingFromTag(tag: Int) {
        val year = (tag / 1000).toInt()
        val groupingSecond = (tag % 1000).toInt()
        putExtra(KEY_YEAR, year)
        putExtra(KEY_SECOND_GROUP, groupingSecond)
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (feature) {
            ContribFeature.DISTRIBUTION -> {
                ensureAccountCursorAtCurrentPosition()?.let {
                    recordUsage(feature)
                    startActivity(Intent(this, DistributionActivity::class.java).apply {
                        putExtra(KEY_ACCOUNTID, accountId)
                        putExtra(KEY_GROUPING, it.getString(KEY_GROUPING))
                        (tag as? Int)?.let { tag -> fillIntentForGroupingFromTag(tag) }
                    })
                }
            }
            ContribFeature.HISTORY -> {
                ensureAccountCursorAtCurrentPosition()?.let {
                    recordUsage(feature)
                    val i = Intent(this, HistoryActivity::class.java)
                    i.putExtra(KEY_ACCOUNTID, accountId)
                    i.putExtra(KEY_GROUPING, it.getString(KEY_GROUPING))
                    startActivity(i)
                }
            }
            ContribFeature.SPLIT_TRANSACTION -> {
                if (tag != null) {
                    val b = Bundle()
                    b.putString(
                        ConfirmationDialogFragment.KEY_MESSAGE,
                        getString(R.string.warning_split_transactions)
                    )
                    b.putInt(
                        ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                        R.id.SPLIT_TRANSACTION_COMMAND
                    )
                    b.putInt(
                        ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE,
                        R.id.CANCEL_CALLBACK_COMMAND
                    )
                    b.putInt(
                        ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                        R.string.menu_split_transaction
                    )
                    b.putLongArray(TaskExecutionFragment.KEY_LONG_IDS, tag as LongArray?)
                    ConfirmationDialogFragment.newInstance(b)
                        .show(supportFragmentManager, "SPLIT_TRANSACTION")
                } else {
                    createRowDo(Transactions.TYPE_SPLIT, false)
                }
            }
            ContribFeature.PRINT -> {
                ensureAccountCursorAtCurrentPosition()?.let { cursor ->
                    currentFragment?.let {
                        val args = Bundle()
                        args.putParcelableArrayList(
                            KEY_FILTER,
                            it.filterCriteria
                        )
                        args.putLong(KEY_ROWID, accountId)
                        args.putLong(KEY_CURRENT_BALANCE, cursor.getLong(KEY_CURRENT_BALANCE))
                        if (!supportFragmentManager.isStateSaved) {
                            supportFragmentManager.beginTransaction()
                                .add(
                                    TaskExecutionFragment.newInstanceWithBundle(
                                        args,
                                        TaskExecutionFragment.TASK_PRINT
                                    ), ProtectedFragmentActivity.ASYNC_TAG
                                )
                                .add(
                                    ProgressDialogFragment.newInstance(
                                        getString(
                                            R.string.progress_dialog_printing,
                                            "PDF"
                                        )
                                    ),
                                    ProtectedFragmentActivity.PROGRESS_TAG
                                )
                                .commit()
                        }
                    }
                }
            }
            ContribFeature.BUDGET -> {
                if (tag != null) {
                    val (budgetId, headerId) = tag as Pair<Long, Int>
                    startActivity(Intent(this, BudgetActivity::class.java).apply {
                        putExtra(KEY_ROWID, budgetId)
                        fillIntentForGroupingFromTag(headerId)
                    })
                } else if (accountId != 0L && currentCurrency != null) {
                    recordUsage(feature)
                    val i = Intent(this, ManageBudgets::class.java)
                    startActivity(i)
                }
            }
            ContribFeature.OCR -> {
                if (featureViewModel.isFeatureAvailable(this, Feature.OCR)) {
                    if ((tag as Boolean)) {
                        /*scanFile = File("/sdcard/OCR_bg.jpg")
                        ocrViewModel.startOcrFeature(scanFile!!, supportFragmentManager);*/
                        ocrViewModel.getScanFiles { pair ->
                            scanFile = pair.second
                            CropImage.activity()
                                .setCameraOnly(true)
                                .setAllowFlipping(false)
                                .setOutputUri(Uri.fromFile(scanFile))
                                .setCaptureImageOutputUri(ocrViewModel.getScanUri(pair.first))
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .start(this)
                        }
                    } else {
                        activateOcrMode()
                    }
                } else {
                    featureViewModel.requestFeature(this, Feature.OCR)
                }
            }
            else -> {}
        }
    }

    fun confirmAccountDelete(accountId: Long) {
        viewModel.account(accountId, once = true).observe(this) { account ->
            MessageDialogFragment.newInstance(
                resources.getQuantityString(
                    R.plurals.dialog_title_warning_delete_account,
                    1,
                    1
                ),
                getString(
                    R.string.warning_delete_account,
                    account.label
                ) + " " + getString(R.string.continue_confirmation),
                MessageDialogFragment.Button(
                    R.string.menu_delete,
                    R.id.DELETE_ACCOUNT_COMMAND_DO,
                    arrayOf(accountId)
                ),
                null,
                MessageDialogFragment.noButton(), 0
            )
                .show(supportFragmentManager, "DELETE_ACCOUNT")
        }
    }

    fun setAccountSealed(accountId: Long, isSealed: Boolean) {
        if (isSealed) {
            viewModel.account(accountId, once = true).observe(this) { account ->
                if (account.syncAccountName == null) {
                    viewModel.setSealed(accountId, true)
                } else {
                    showSnackBar(
                        getString(R.string.warning_synced_account_cannot_be_closed),
                        Snackbar.LENGTH_LONG, null, null, accountList()
                    )
                }
            }
        } else {
            viewModel.setSealed(accountId, false)
        }
    }

    fun accountList(): ExpandableStickyListHeadersListView {
        return binding.accountPanel.accountList
    }

    fun viewPager(): ViewPager {
        return binding.viewPagerMain.viewPager
    }

    fun navigationView(): NavigationView {
        return binding.accountPanel.expansionContent
    }

    open fun buildCheckSealedHandler() = CheckSealedHandler(contentResolver)

    fun doReset() {
        currentFragment?.takeIf { it.hasItems() }?.also { fragment ->
            exportViewModel.checkAppDir().observe(this) { result ->
                result.onSuccess {
                    ensureAccountCursorAtCurrentPosition()?.let { cursor ->
                        val isSealed = cursor.getInt(DatabaseConstants.KEY_SEALED) == 1
                        val label = cursor.getString(KEY_LABEL)
                        val currency = cursor.getString(KEY_CURRENCY)
                        exportViewModel.hasExported(accountId).observe(this) {
                            ExportDialogFragment.newInstance(
                                ExportDialogFragment.AccountInfo(
                                    accountId,
                                    label,
                                    currency,
                                    isSealed,
                                    it,
                                    fragment.isFiltered
                                )
                            ).show(this.supportFragmentManager, "EXPORT")
                        }
                    }
                }.onFailure {
                    showDismissibleSnackBar(it.safeMessage)
                }
            }
        } ?: run {
            showExportDisabledCommand()
        }
    }

    override fun getCurrentFragment() = pagerAdapter?.let {
        supportFragmentManager.findFragmentByTag(
            it.getFragmentName(currentPosition)
        ) as TransactionList?
    }

    fun showExportDisabledCommand() {
        showMessage(R.string.dialog_command_disabled_reset_account)
    }

    /**
     * check if this is the first invocation of a new version
     * in which case help dialog is presented
     * also is used for hooking version specific upgrade procedures
     * and display information to be presented upon app launch
     */
    fun newVersionCheck() {
        val prevVersion = prefHandler.getInt(PrefKey.CURRENT_VERSION, -1)
        val currentVersion = DistributionHelper.versionNumber
        if (prevVersion < currentVersion) {
            if (prevVersion == -1) {
                return
            }
            upgradeHandlerViewModel.upgrade(prevVersion, currentVersion)
            val showImportantUpgradeInfo = ArrayList<Int>()
            prefHandler.putInt(PrefKey.CURRENT_VERSION, currentVersion)
            if (prevVersion < 19) {
                prefHandler.putString(PrefKey.SHARE_TARGET, prefHandler.getString("ftp_target", ""))
                prefHandler.remove("ftp_target")
            }
            if (prevVersion < 28) {
                Timber.i(
                    "Upgrading to version 28: Purging %d transactions from database",
                    contentResolver.delete(
                        TransactionProvider.TRANSACTIONS_URI,
                        "$KEY_ACCOUNTID not in (SELECT _id FROM accounts)", null
                    )
                )
            }
            if (prevVersion < 30) {
                if ("" != prefHandler.getString(PrefKey.SHARE_TARGET, "")) {
                    prefHandler.putBoolean(PrefKey.SHARE_TARGET, true)
                }
            }
            if (prevVersion < 40) {
                //this no longer works since we migrated time to utc format
                //  DbUtils.fixDateValues(getContentResolver());
                //we do not want to show both reminder dialogs too quickly one after the other for upgrading users
                //if they are already above both thresholds, so we set some delay
                prefHandler.putLong("nextReminderContrib", Transaction.getSequenceCount() + 23)
            }
            if (prevVersion < 163) {
                prefHandler.remove("qif_export_file_encoding")
            }
            if (prevVersion < 199) {
                //filter serialization format has changed
                val edit = settings.edit()
                for (entry in settings.all.entries) {
                    val key = entry.key
                    val keyParts =
                        key.split(("_").toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (keyParts[0] == "filter") {
                        val `val` = settings.getString(key, "")!!
                        when (keyParts[1]) {
                            "method", "payee", "cat" -> {
                                val sepIndex = `val`.indexOf(";")
                                edit.putString(
                                    key,
                                    `val`.substring(sepIndex + 1) + ";" + Criteria.escapeSeparator(
                                        `val`.substring(0, sepIndex)
                                    )
                                )
                            }
                            "cr" -> edit.putString(
                                key,
                                CrStatus.values()[Integer.parseInt(`val`)].name
                            )
                        }
                    }
                }
                edit.apply()
            }
            if (prevVersion < 202) {
                val appDir = prefHandler.getString(PrefKey.APP_DIR, null)
                if (appDir != null) {
                    prefHandler.putString(PrefKey.APP_DIR, Uri.fromFile(File(appDir)).toString())
                }
            }
            if (prevVersion < 221) {
                prefHandler.putString(
                    PrefKey.SORT_ORDER_LEGACY,
                    if (prefHandler.getBoolean(PrefKey.CATEGORIES_SORT_BY_USAGES_LEGACY, true))
                        "USAGES"
                    else
                        "ALPHABETIC"
                )
            }
            if (prevVersion < 303) {
                if (prefHandler.getBoolean(PrefKey.AUTO_FILL_LEGACY, false)) {
                    enableAutoFill(prefHandler)
                }
                prefHandler.remove(PrefKey.AUTO_FILL_LEGACY)
            }
            if (prevVersion < 316) {
                prefHandler.putString(PrefKey.HOME_CURRENCY, Utils.getHomeCurrency().code)
                invalidateHomeCurrency()
            }
            if (prevVersion < 354 && GenericAccountService.getAccounts(this).isNotEmpty()) {
                showImportantUpgradeInfo.add(R.string.upgrade_information_cloud_sync_storage_format)
            }

            showVersionDialog(prevVersion, showImportantUpgradeInfo)
        } else {
            if ((!licenceHandler.hasTrialAccessTo(ContribFeature.SYNCHRONIZATION) && !prefHandler.getBoolean(
                    PrefKey.SYNC_UPSELL_NOTIFICATION_SHOWN,
                    false
                ))
            ) {
                prefHandler.putBoolean(PrefKey.SYNC_UPSELL_NOTIFICATION_SHOWN, true)
                ContribUtils.showContribNotification(this, ContribFeature.SYNCHRONIZATION)
            }
        }
        checkCalendarPermission()
    }

    private fun checkCalendarPermission() {
        if ("-1" != prefHandler.getString(PrefKey.PLANNER_CALENDAR_ID, "-1")) {
            if (!PermissionHelper.PermissionGroup.CALENDAR.hasPermission(this)) {
                requestPermission(PermissionHelper.PermissionGroup.CALENDAR)
            }
        }
    }

    fun balance(accountId: Long, reset: Boolean) {
        viewModel.balanceAccount(accountId, reset).observe(
            this
        ) { result ->
            result.onFailure {
                showSnackBar(it.safeMessage)
            }
        }
    }

    fun startExport(args: Bundle) {
        args.putParcelableArrayList(
            KEY_FILTER,
            currentFragment!!.filterCriteria
        )
        supportFragmentManager.beginTransaction()
            .add(
                ProgressDialogFragment.newInstance(
                    getString(R.string.pref_category_title_export),
                    null,
                    ProgressDialog.STYLE_SPINNER,
                    true
                ), ProtectedFragmentActivity.PROGRESS_TAG
            )
            .commitNow()
        exportViewModel.startExport(args)
    }

    companion object {
        const val MANAGE_HIDDEN_FRAGMENT_TAG = "MANAGE_HIDDEN"
    }
}