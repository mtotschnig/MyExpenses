package org.totschnig.myexpenses.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyExpenses.Companion.MANAGE_HIDDEN_FRAGMENT_TAG
import org.totschnig.myexpenses.compose.filter.FilterCard
import org.totschnig.myexpenses.compose.filter.FilterDialog
import org.totschnig.myexpenses.compose.filter.FilterHandler
import org.totschnig.myexpenses.compose.filter.TYPE_COMPLEX
import org.totschnig.myexpenses.compose.transactions.CompactTransactionRenderer
import org.totschnig.myexpenses.compose.transactions.DateTimeFormatInfo
import org.totschnig.myexpenses.compose.transactions.FutureCriterion
import org.totschnig.myexpenses.compose.transactions.ItemRenderer
import org.totschnig.myexpenses.compose.transactions.NewTransactionRenderer
import org.totschnig.myexpenses.compose.transactions.RenderType
import org.totschnig.myexpenses.compose.transactions.TransactionEvent
import org.totschnig.myexpenses.compose.transactions.TransactionEventHandler
import org.totschnig.myexpenses.compose.transactions.TransactionList
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.db2.countAccounts
import org.totschnig.myexpenses.dialog.ArchiveDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_CHECKBOX_LABEL
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_COMMAND_POSITIVE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_MESSAGE
import org.totschnig.myexpenses.dialog.ExportDialogFragment
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.dialog.ProgressDialogFragment
import org.totschnig.myexpenses.dialog.progress.NewProgressDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectTransformToTransferTargetDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectTransformToTransferTargetDialogFragment.Companion.KEY_IS_INCOME
import org.totschnig.myexpenses.dialog.select.SelectTransformToTransferTargetDialogFragment.Companion.TRANSFORM_TO_TRANSFER_REQUEST
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod.Companion.translateIfPredefined
import org.totschnig.myexpenses.preference.ColorSource
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.CheckSealedHandler
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_COLOR
import org.totschnig.myexpenses.provider.KEY_GROUPING
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_SECOND_GROUP
import org.totschnig.myexpenses.provider.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.KEY_YEAR
import org.totschnig.myexpenses.provider.filter.AmountCriterion
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.CommentCriterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.provider.filter.MethodCriterion
import org.totschnig.myexpenses.provider.filter.Operation
import org.totschnig.myexpenses.provider.filter.PayeeCriterion
import org.totschnig.myexpenses.provider.filter.SimpleCriterion
import org.totschnig.myexpenses.provider.filter.TagCriterion
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.ContribUtils
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.util.ui.asDateTimeFormatter
import org.totschnig.myexpenses.util.ui.dateTimeFormatter
import org.totschnig.myexpenses.util.ui.dateTimeFormatterLegacy
import org.totschnig.myexpenses.viewmodel.AccountSealedException
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel.DeleteState.DeleteComplete
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel.DeleteState.DeleteProgress
import org.totschnig.myexpenses.viewmodel.ExportViewModel
import org.totschnig.myexpenses.viewmodel.KEY_ROW_IDS
import org.totschnig.myexpenses.viewmodel.ModalProgressViewModel
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel.SelectionInfo
import org.totschnig.myexpenses.viewmodel.OpenAction
import org.totschnig.myexpenses.viewmodel.ShareAction
import org.totschnig.myexpenses.viewmodel.SumInfo
import org.totschnig.myexpenses.viewmodel.UpgradeHandlerViewModel
import org.totschnig.myexpenses.viewmodel.data.BaseAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import timber.log.Timber
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

typealias RenderFactory = (
    renderType: RenderType,
    account: PageAccount,
    withCategoryIcon: Boolean,
    colorSource: ColorSource,
    onToggleCrStatus: ((Long) -> Unit)?,
) -> ItemRenderer

abstract class BaseMyExpenses<T : MyExpensesViewModel> : LaunchActivity() {

    lateinit var viewModel: T
    lateinit var remapHandler: RemapHandler
    lateinit var tagHandler: TagHandler

    val upgradeHandlerViewModel: UpgradeHandlerViewModel by viewModels()
    private val exportViewModel: ExportViewModel by viewModels()
    private val progressViewModel: ModalProgressViewModel by viewModels()

    abstract val currentAccount: BaseAccount?

    val currentFilter: FilterPersistence
        get() = viewModel.filterPersistence.getValue(selectedAccountId)

    abstract fun finishActionMode()

    @get:Composable
    abstract val transactionListWindowInsets: WindowInsets

    abstract val accountCount: Int

    var selectionState
        get() = viewModel.selectionState.value
        set(value) {
            viewModel.selectionState.value = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        remapHandler = RemapHandler(this)
        tagHandler = TagHandler(this)
        with(injector) {
            inject(upgradeHandlerViewModel)
            inject(exportViewModel)
        }
        if (savedInstanceState == null) {
            newVersionCheck()
            //voteReminderCheck();

            showTransactionFromIntent(intent)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                exportViewModel.publishProgress.collect { progress ->
                    progress?.let {
                        progressViewModel.appendToMessage(it)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                exportViewModel.result.collect { result ->
                    result?.let { (exportFormat, documentList) ->
                        val legacyShare =
                            prefHandler.getBoolean(
                                PrefKey.PERFORM_SHARE,
                                false
                            ) && shareTarget.isNotEmpty()
                        val uriList = documentList.map { it.uri }
                        progressViewModel.onTaskCompleted(
                            buildList {
                                if (!legacyShare && documentList.isNotEmpty()) {
                                    if (exportFormat == ExportFormat.CSV) {
                                        add(
                                            OpenAction(
                                                mimeType = exportFormat.mimeType,
                                                targets = uriList
                                            )
                                        )
                                    }
                                    add(
                                        ShareAction(
                                            mimeType = exportFormat.mimeType,
                                            targets = uriList
                                        )
                                    )
                                }
                            }
                        )
                        if (legacyShare && documentList.isNotEmpty()) {
                            shareExport(exportFormat, uriList)
                        }
                        exportViewModel.resultProcessed()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bulkDeleteState.filterNotNull().collect { result ->
                    when (result) {
                        is DeleteProgress -> {
                            showProgressSnackBar(
                                getString(R.string.progress_dialog_deleting),
                                result.total,
                                result.count
                            )
                        }

                        is DeleteComplete -> {
                            showSnackBar(
                                buildList {
                                    if (result.success > 0) {
                                        add(
                                            resources.getQuantityString(
                                                R.plurals.delete_success,
                                                result.success,
                                                result.success
                                            )
                                        )
                                    }
                                    if (result.failure > 0) {
                                        add(deleteFailureMessage(null))
                                    }
                                }.joinToString(" ")
                            )
                            viewModel.bulkDeleteCompleteShown()
                        }
                    }
                }
            }
        }

        supportFragmentManager.setFragmentResultListener(
            TRANSFORM_TO_TRANSFER_REQUEST,
            this
        ) { _, bundle ->
            val isIncome = bundle.getBoolean(KEY_IS_INCOME)
            val target = bundle.getString(KEY_LABEL)
            val from = if (isIncome) target else currentAccount!!.label
            val to = if (isIncome) currentAccount!!.label else target
            showConfirmationDialog(
                "TRANSFORM_TRANSFER",
                getString(R.string.warning_transform_to_transfer, from, to),
                R.id.TRANSFORM_TO_TRANSFER_COMMAND
            ) {
                putAll(bundle)
            }
        }
    }

    fun startExport(args: Bundle) {
        args.addFilter()
        supportFragmentManager.beginTransaction()
            .add(
                NewProgressDialogFragment.newInstance(
                    getString(R.string.pref_category_title_export)
                ),
                PROGRESS_TAG
            )
            .commitNow()
        exportViewModel.startExport(args)
    }

    private fun Bundle.addFilter() {
        putParcelable(
            KEY_FILTER,
            currentFilter.whereFilter.value
        )
    }

    private fun shareExport(format: ExportFormat, uriList: List<Uri>) {
        baseViewModel.share(
            this, uriList,
            shareTarget,
            "text/" + format.name.lowercase(Locale.US)
        )
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        intent.extras?.let {
            val fromExtra = it.getLong(KEY_ROWID, 0)
            if (fromExtra != 0L) {
                selectedAccountId = fromExtra
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pdfResult.collectPrintResult()
            }
        }
        viewModel.cloneAndRemapProgress.observe(
            this
        ) { (first, second): Pair<Int, Int> ->
            val progressDialog =
                supportFragmentManager.findFragmentByTag(PROGRESS_TAG) as? ProgressDialogFragment
            val totalProcessed = first + second
            if (progressDialog != null) {
                if (totalProcessed < progressDialog.max) {
                    progressDialog.setProgress(totalProcessed)
                } else {
                    if (second == 0) {
                        showSnackBar(R.string.clone_and_remap_result)
                    } else {
                        showSnackBar(
                            String.format(
                                Locale.ROOT,
                                "%d out of %d failed",
                                second,
                                totalProcessed
                            )
                        )
                    }
                    supportFragmentManager.beginTransaction().remove(progressDialog).commit()
                }
            }
        }
        if (savedInstanceState == null) {
            selectedAccountId = prefHandler.getLong(PrefKey.CURRENT_ACCOUNT, 0L)
        }
    }

    protected fun editAccount(account: FullAccount) {
        startActivity(Intent(this, AccountEdit::class.java).apply {
            putExtra(KEY_ROWID, account.id)
            putExtra(KEY_COLOR, account.color)
        })
    }

    protected fun confirmAccountDelete(account: FullAccount) {
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
                longArrayOf(account.id)
            ),
            null,
            MessageDialogFragment.noButton(), 0
        )
            .show(supportFragmentManager, "DELETE_ACCOUNT")
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        } else when (command) {


            R.id.CREATE_ACCOUNT_FOR_TRANSFER_COMMAND -> {
                createAccountForTransfer.launch(Unit)
            }

            R.id.ARCHIVE_COMMAND -> {
                (currentAccount as? FullAccount)?.let {
                    ArchiveDialogFragment.newInstance(it).show(supportFragmentManager, "ARCHIVE")
                }
            }

            R.id.PRINT_COMMAND -> AppDirHelper.checkAppDir(this)
                .onSuccess {
                    contribFeatureRequested(
                        ContribFeature.PRINT,
                        ExportViewModel.PRINT_TRANSACTION_LIST
                    )
                }.onFailure {
                    showDismissibleSnackBar(it.safeMessage)
                }

            R.id.RESET_COMMAND -> checkReset()

            R.id.HISTORY_COMMAND -> contribFeatureRequested(ContribFeature.HISTORY)

            R.id.DISTRIBUTION_COMMAND -> contribFeatureRequested(ContribFeature.DISTRIBUTION)

            R.id.BUDGET_COMMAND -> contribFeatureRequested(ContribFeature.BUDGET, null)

            R.id.MANAGE_TEMPLATES_COMMAND -> startActivity(
                Intent(this, ManageTemplates::class.java)
            )

            R.id.MANAGE_PARTIES_COMMAND -> startActivity(
                Intent(this, ManageParties::class.java).apply {
                    action = Action.MANAGE.name
                }
            )

            R.id.DELETE_ACCOUNT_COMMAND_DO -> {
                val accountIds = tag as LongArray
                val manageHiddenFragment =
                    supportFragmentManager.findFragmentByTag(MANAGE_HIDDEN_FRAGMENT_TAG)
                if (manageHiddenFragment != null) {
                    supportFragmentManager.beginTransaction().remove(manageHiddenFragment).commit()
                }
                showSnackBarIndefinite(R.string.progress_dialog_deleting)
                viewModel.deleteAccounts(accountIds).observe(this) { result ->
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
            }

            R.id.CLEAR_FILTER_COMMAND -> {
                lifecycleScope.launch {
                    currentFilter.persist(null)
                    invalidateOptionsMenu()
                }
            }

            else -> return false
        }
        return true
    }

    private val createAccountForTransfer =
        registerForActivityResult(AccountEdit.Companion.CreateContract()) {
            if (it != null) {
                createRow(TYPE_TRANSFER, transferEnabled = accountCount > 1)
            }
        }

    private fun checkReset() {
        exportViewModel.checkAppDir().observe(this) { result ->
            result.onSuccess {
                currentAccount?.let { account ->
                    if (account.isAggregate) {
                        //for aggregate account sealed is checked for each account during export
                        showExportDialog(emptyList())
                    } else if ((account as FullAccount).sealed) {
                        showExportDialog(listOf(R.string.account_closed))
                    } else {
                        checkSealedHandler.checkAccount(account.id) { result ->
                            result.onSuccess {
                                showExportDialog(
                                    listOfNotNull(
                                        if (!it.first) R.string.object_sealed else null,
                                        if (!it.second) R.string.object_sealed_debt else null
                                    )
                                )
                            }
                                .onFailure {
                                    showSnackBar(it.safeMessage)
                                }
                        }
                    }
                }
            }.onFailure {
                showDismissibleSnackBar(it.safeMessage)
            }
        }
    }

    private fun showExportDialog(cannotResetConditions: List<Int>) {
        currentAccount?.let {
            with(it) {
                exportViewModel.hasExported(this)
                    .observe(this@BaseMyExpenses) { hasExported ->
                        ExportDialogFragment.newInstance(
                            ExportDialogFragment.AccountInfo(
                                id,
                                label,
                                currency,
                                cannotResetConditions,
                                hasExported,
                                currentFilter.whereFilter.value != null
                            )
                        ).show(supportFragmentManager, "EXPORT")
                    }
            }
        }
    }

    protected fun toggleAccountSealed(account: FullAccount, snackBarContainer: View? = null) {
        if (account.sealed) {
            viewModel.setSealed(account.id, false)
        } else {
            if (account.syncAccountName == null) {
                viewModel.setSealed(account.id, true)
            } else {
                showSnackBar(
                    getString(R.string.warning_synced_account_cannot_be_closed),
                    Snackbar.LENGTH_LONG, null, null, snackBarContainer
                )
            }
        }
    }

    protected fun toggleExcludeFromTotals(account: FullAccount) {
        viewModel.setExcludeFromTotals(account.id, !account.excludeFromTotals)
    }

    protected fun toggleDynamicExchangeRate(account: FullAccount) {
        viewModel.setDynamicExchangeRate(account.id, !account.dynamic)
    }

    var selectedAccountId: Long
        get() = viewModel.selectedAccountId.value
        set(value) {
            viewModel.selectAccount(value)
        }

    protected val createAccount =
        registerForActivityResult(AccountEdit.Companion.CreateContract()) {
            if (it != null) {
                selectedAccountId = it
            }
        }

    fun createRow(
        @Transactions.TransactionType type: Int,
        transferEnabled: Boolean,
        isIncome: Boolean = false,
    ) {
        when (type) {
            TYPE_SPLIT -> contribFeatureRequested(ContribFeature.SPLIT_TRANSACTION)
            TYPE_TRANSFER if !transferEnabled -> showTransferAccountMissingMessage()
            else -> createRowDo(type, isIncome)
        }
    }

    fun createRowDo(type: Int, isIncome: Boolean) {
        lifecycleScope.launch {
            createRowIntent(type, isIncome)?.let { startEdit(it) }
        }
    }

    /**
     * start ExpenseEdit Activity for a new transaction/transfer/split
     * Originally the form for transaction is rendered, user can change from spinner in toolbar
     */
    suspend fun createRowIntent(type: Int, isIncome: Boolean) = getEditIntent()?.apply {
        putExtra(Transactions.OPERATION_TYPE, type)
        putExtra(ExpenseEdit.KEY_INCOME, isIncome)
    }

    protected fun toggleCrStatus(transactionId: Long) {
        checkSealed(listOf(transactionId), withTransfer = false) {
            viewModel.toggleCrStatus(transactionId)
        }
    }

    open val checkSealedHandler by lazy { CheckSealedHandler(contentResolver) }

    fun checkSealed(itemIds: List<Long>, withTransfer: Boolean = true, onChecked: Runnable) {
        checkSealedHandler.check(itemIds, withTransfer) { result ->
            lifecycleScope.launchWhenResumed {
                result.onSuccess {
                    if (it.first && it.second) {
                        onChecked.run()
                    } else {
                        warnSealedAccount(!it.first, !it.second, itemIds.size > 1)
                    }
                }.onFailure {
                    showSnackBar(it.safeMessage)
                }
            }
        }
    }

    private fun warnSealedAccount(sealedAccount: Boolean, sealedDebt: Boolean, multiple: Boolean) {
        val resIds = mutableListOf<Int>()
        if (multiple) {
            resIds.add(R.string.warning_account_for_transaction_is_closed)
        }
        if (sealedAccount) {
            resIds.add(R.string.object_sealed)
        }
        if (sealedDebt) {
            resIds.add(R.string.object_sealed_debt)
        }
        showSnackBar(TextUtils.concatResStrings(this, *resIds.toIntArray()))
    }

    fun printBalanceSheet() {
        AppDirHelper.checkAppDir(this)
            .onSuccess {
                contribFeatureRequested(
                    ContribFeature.PRINT,
                    ExportViewModel.PRINT_BALANCE_SHEET
                )
            }.onFailure {
                showDismissibleSnackBar(it.safeMessage)
            }
    }

    private fun Intent.fillIntentForGroupingFromTag(tag: Int) {
        val year = (tag / 1000)
        val groupingSecond = (tag % 1000)
        putExtra(KEY_YEAR, year)
        putExtra(KEY_SECOND_GROUP, groupingSecond)
    }

    private fun Intent.forwardCurrentConfiguration(currentAccount: BaseAccount) {
        putExtra(KEY_ACCOUNTID, currentAccount.id)
        putExtra(KEY_GROUPING, currentAccount.grouping)
        if (currentFilter.whereFilter.value != null) {
            putExtra(KEY_FILTER, currentFilter.whereFilter.value)
        }
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        currentAccount?.also { currentAccount ->
            when (feature) {
                ContribFeature.DISTRIBUTION -> {
                    recordUsage(feature)
                    startActivity(Intent(this, DistributionActivity::class.java).apply {
                        forwardCurrentConfiguration(currentAccount)
                    })
                }

                ContribFeature.HISTORY -> {
                    recordUsage(feature)
                    startActivity(Intent(this, HistoryActivity::class.java).apply {
                        forwardCurrentConfiguration(currentAccount)
                    })
                }

                ContribFeature.SPLIT_TRANSACTION -> {
                    if (tag != null) {
                        showConfirmationDialog(
                            tag = "SPLIT_TRANSACTION",
                            message = getString(R.string.warning_split_transactions),
                            commandPositive = R.id.SPLIT_TRANSACTION_COMMAND,
                            commandPositiveLabel = R.string.menu_split_transaction
                        ) {
                            putLongArray(KEY_ROW_IDS, tag as LongArray?)
                        }
                    } else {
                        createRowDo(TYPE_SPLIT, false)
                    }
                }

                ContribFeature.PRINT -> {
                    showProgressSnackBar(
                        getString(R.string.progress_dialog_printing, "PDF")
                    )
                    if (tag == ExportViewModel.PRINT_TRANSACTION_LIST) {
                        viewModel.print(
                            currentAccount.toPageAccount(this),
                            currentFilter.whereFilter.value
                        )
                    } else if (tag == ExportViewModel.PRINT_BALANCE_SHEET) {
                        viewModel.printBalanceSheet()
                    }
                }

                ContribFeature.BUDGET -> {
                    if (tag != null) {
                        val (budgetId, headerId) = tag as Pair<Long, Int>
                        startActivity(Intent(this, BudgetActivity::class.java).apply {
                            putExtra(KEY_ROWID, budgetId)
                            fillIntentForGroupingFromTag(headerId)
                        })
                    } else {
                        recordUsage(feature)
                        val i = Intent(this, ManageBudgets::class.java)
                        startActivity(i)
                    }
                }

                ContribFeature.BANKING -> {
                    val (bankId, accountId, accountTypeId) = tag as Triple<Long, Long, Long>
                    bankingFeature.startSyncFragment(
                        bankId,
                        accountId,
                        accountTypeId,
                        supportFragmentManager
                    )
                }

                ContribFeature.OCR -> if (featureViewModel.isFeatureAvailable(this, Feature.OCR)) {
                    //ocrViewModel.startOcrFeature(Uri.parse("file:///android_asset/OCR.jpg"), supportFragmentManager);
                    startMediaChooserDo("SCAN")
                } else {
                    featureViewModel.requestFeature(this, Feature.OCR)
                }

                else -> super.contribFeatureCalled(feature, tag)
            }
        } ?: run {
            showSnackBar(R.string.no_accounts)
        }
    }

    protected fun unarchive(transactionId: Long) {
        showConfirmationDialog(
            tag = "UNARCHIVE",
            message = getString(R.string.warning_unarchive),
            commandPositive = R.id.UNARCHIVE_COMMAND,
            commandPositiveLabel = R.string.menu_unpack
        ) {
            putLong(KEY_ROWID, transactionId)
        }
    }

    protected suspend fun deleteArchive(transaction: Transaction2) {
        val count = withContext(Dispatchers.IO) {
            viewModel.childCount(transaction.id)
        }
        checkSealed(listOf(transaction.id)) {
            val message = buildString {
                append(getString(R.string.warning_delete_archive, count))
                if (transaction.crStatus == CrStatus.RECONCILED) {
                    append(" ")
                    append(getString(R.string.warning_delete_reconciled))
                }
            }

            showConfirmationDialog(
                tag = "DELETE_TRANSACTION",
                message = message,
                commandPositive = R.id.DELETE_COMMAND_DO,
                commandPositiveLabel = R.string.menu_delete
            ) {
                putInt(
                    ConfirmationDialogFragment.KEY_TITLE,
                    R.string.dialog_title_warning_delete_archive
                )
                putLongArray(KEY_ROW_IDS, longArrayOf(transaction.id))
            }
        }
    }

    protected fun edit(transaction: Transaction2, clone: Boolean = false) {
        checkSealed(listOf(transaction.id)) {
            if (transaction.transferPeerIsPart == true) {
                showSnackBar(
                    if (transaction.transferPeerIsArchived == true) R.string.warning_archived_transfer_cannot_be_edited else R.string.warning_splitpartcategory_context
                )
            } else {
                startActivityForResult(
                    Intent(this, ExpenseEdit::class.java).apply {
                        putExtra(KEY_ROWID, transaction.id)
                        putExtra(KEY_COLOR, transaction.color ?: currentAccount?.color(resources))
                        if (clone) {
                            putExtra(ExpenseEdit.KEY_CLONE, true)
                        }

                    }, EDIT_REQUEST
                )
            }
        }
    }

    protected fun createTemplate(transaction: Transaction2) {
        checkSealed(listOf(transaction.id)) {
            if (transaction.isSplit && !prefHandler.getBoolean(
                    PrefKey.NEW_SPLIT_TEMPLATE_ENABLED,
                    true
                )
            ) {
                showContribDialog(ContribFeature.SPLIT_TEMPLATE, null)
            } else {
                startActivity(Intent(this, ExpenseEdit::class.java).apply {
                    action = ExpenseEdit.ACTION_CREATE_TEMPLATE_FROM_TRANSACTION
                    putExtra(KEY_ROWID, transaction.id)
                })
            }
        }
    }

    protected fun delete(transactions: List<Pair<Long, CrStatus>>) {
        val hasReconciled = transactions.any { it.second == CrStatus.RECONCILED }
        val hasNotVoid = transactions.any { it.second != CrStatus.VOID }
        val itemIds = transactions.map { it.first }
        checkSealed(itemIds) {
            var message = resources.getQuantityString(
                R.plurals.warning_delete_transaction,
                transactions.size,
                transactions.size
            )
            if (hasReconciled) {
                message += " " + getString(R.string.warning_delete_reconciled)
            }
            showConfirmationDialog(
                tag = "DELETE_TRANSACTION",
                message = message,
                commandPositive = R.id.DELETE_COMMAND_DO,
                commandPositiveLabel = R.string.menu_delete
            ) {
                putInt(
                    ConfirmationDialogFragment.KEY_TITLE,
                    R.string.dialog_title_warning_delete_transaction
                )
                if (hasNotVoid) {
                    putString(
                        KEY_CHECKBOX_LABEL,
                        getString(R.string.mark_void_instead_of_delete)
                    )
                }
                putLongArray(KEY_ROW_IDS, itemIds.toLongArray())
            }
        }
    }

    protected fun undelete(itemIds: List<Long>) {
        checkSealed(itemIds) {
            viewModel.undeleteTransactions(itemIds).observe(this) { result: Int ->
                finishActionMode()
                showSnackBar("${getString(R.string.menu_undelete_transaction)}: $result")
            }
        }
    }

    protected fun ungroupSplit(transaction: Transaction2) {
        showConfirmationDialog(
            tag = "UNSPLIT_TRANSACTION",
            message = getString(R.string.warning_ungroup_split_transactions),
            commandPositive = R.id.UNGROUP_SPLIT_COMMAND,
            commandPositiveLabel = R.string.menu_ungroup_split_transaction
        ) {
            putLong(KEY_ROWID, transaction.id)
        }
    }

    protected fun unlinkTransfer(transaction: Transaction2) {
        showConfirmationDialog(
            tag = "UNLINK_TRANSFER",
            message = getString(R.string.warning_unlink_transfer),
            commandPositive = R.id.UNLINK_TRANSFER_COMMAND,
            commandPositiveLabel = R.string.menu_unlink_transfer
        ) {
            putLong(KEY_ROWID, transaction.id)
        }
    }

    protected fun transformToTransfer(transaction: Transaction2) {
        SelectTransformToTransferTargetDialogFragment.newInstance(transaction)
            .show(supportFragmentManager, "SELECT_ACCOUNT")
    }

    fun addFilterCriterion(c: SimpleCriterion<*>) {
        lifecycleScope.launch {
            currentFilter.addCriterion(c)
            invalidateOptionsMenu()
        }
    }

    fun isContextMenuItemVisible(itemId: Int): Boolean {
        val hasTransfer = selectionState.any { it.isTransfer }
        val hasSplit = selectionState.any { it.isSplit }
        val hasVoid = selectionState.any { it.crStatus == CrStatus.VOID }
        return when (itemId) {
            R.id.REMAP_ACCOUNT_COMMAND -> accountCount > 1
            R.id.REMAP_PAYEE_COMMAND -> !hasTransfer
            R.id.REMAP_CATEGORY_COMMAND -> !hasSplit
            R.id.REMAP_METHOD_COMMAND -> !hasTransfer
            R.id.SPLIT_TRANSACTION_COMMAND -> !hasSplit && !hasVoid
            R.id.LINK_TRANSFER_COMMAND ->
                selectionState.count() == 2 &&
                        !hasSplit && !hasTransfer && !hasVoid &&
                        viewModel.canLinkSelection()

            R.id.UNDELETE_COMMAND -> hasVoid
            else -> true
        }
    }

    fun BaseAccount?.isMenuItemVisible(itemId: Int): Boolean {
        return when (itemId) {
            R.id.SYNC_COMMAND -> (this as? FullAccount)?.syncAccountName != null
            R.id.HISTORY_COMMAND, R.id.RESET_COMMAND, R.id.PRINT_COMMAND -> hasItems
            R.id.DISTRIBUTION_COMMAND -> sumInfo.value.mappedCategories
            R.id.BALANCE_COMMAND -> this is FullAccount && !isAggregate && type.supportsReconciliation && !sealed
            R.id.FINTS_SYNC_COMMAND -> (this as? FullAccount)?.bankId != null
            R.id.ARCHIVE_COMMAND -> this is FullAccount && !isAggregate && !sealed && hasItems
            else -> true
        }
    }

    fun onContextItemClicked(@IdRes itemId: Int): Boolean {
        if (remapHandler.handleActionItemClick(itemId)) return true
        when (itemId) {
            R.id.DELETE_COMMAND -> delete(selectionState.map { it.id to it.crStatus })
            R.id.MAP_TAG_COMMAND -> tagHandler.tag()
            R.id.SPLIT_TRANSACTION_COMMAND -> split(selectionState.map { it.id })
            R.id.LINK_TRANSFER_COMMAND -> linkTransfer()
            R.id.SELECT_ALL_COMMAND -> selectAll()
            R.id.UNDELETE_COMMAND -> undelete(selectionState.map { it.id })
            else -> return false
        }
        return true
    }

    private fun split(itemIds: List<Long>) {
        checkSealed(itemIds) {
            contribFeatureRequested(
                ContribFeature.SPLIT_TRANSACTION,
                itemIds.toLongArray()
            )
        }

    }

    private fun selectAll() {
        viewModel.selectAllState.value = true
    }

    fun selectAllListTooLarge() {
        showSnackBar(
            getString(
                R.string.select_all_list_too_large,
                getString(android.R.string.selectAll)
            )
        )
    }

    private fun linkTransfer() {
        val itemIds = selectionState.map { it.id }
        checkSealed(itemIds) {
            showConfirmationDialog(
                tag = "LINK_TRANSFER",
                message = getString(R.string.warning_link_transfer) + " " + getString(R.string.continue_confirmation),
                commandPositive = R.id.LINK_TRANSFER_COMMAND,
                commandPositiveLabel = R.string.menu_create_transfer
            ) {
                putLongArray(KEY_ROW_IDS, itemIds.toLongArray())
            }
        }
    }

    override fun onPositive(args: Bundle, checked: Boolean) {
        super.onPositive(args, checked)
        when (args.getInt(KEY_COMMAND_POSITIVE)) {
            R.id.DELETE_COMMAND_DO -> {
                finishActionMode()
                viewModel.deleteTransactions(args.getLongArray(KEY_ROW_IDS)!!, checked)
            }

            R.id.BALANCE_COMMAND_DO -> {
                balance(args.getLong(KEY_ROWID), checked)
            }

            R.id.REMAP_COMMAND -> {
                remapHandler.remap(args, checked)
                finishActionMode()
            }

            R.id.SPLIT_TRANSACTION_COMMAND -> {
                finishActionMode()
                val ids = args.getLongArray(KEY_ROW_IDS)!!
                viewModel.split(ids).observe(this) { result ->
                    showSnackBar(
                        result.fold(
                            onSuccess = {
                                if (it) {
                                    recordUsage(ContribFeature.SPLIT_TRANSACTION)
                                    if (ids.size > 1)
                                        getString(R.string.split_transaction_one_success)
                                    else
                                        getString(
                                            R.string.split_transaction_group_success,
                                            ids.size
                                        )
                                } else getString(R.string.split_transaction_not_possible)
                            },
                            onFailure = {
                                report(it)
                                it.safeMessage
                            }
                        ))
                }
            }

            R.id.UNGROUP_SPLIT_COMMAND -> {
                viewModel.revokeSplit(args.getLong(KEY_ROWID)).observe(this) { result ->
                    result.onSuccess {
                        showSnackBar(getString(R.string.ungroup_split_transaction_success))
                    }.onFailure {
                        report(it)
                        showSnackBar(it.safeMessage)
                    }
                }
            }

            R.id.LINK_TRANSFER_COMMAND -> {
                finishActionMode()
                viewModel.linkTransfer(args.getLongArray(KEY_ROW_IDS)!!).observeAndReportFailure()
            }

            R.id.UNLINK_TRANSFER_COMMAND -> {
                viewModel.unlinkTransfer(args.getLong(KEY_ROWID)).observeAndReportFailure()
            }

            R.id.TRANSFORM_TO_TRANSFER_COMMAND -> {
                viewModel.transformToTransfer(
                    args.getLong(KEY_TRANSACTIONID),
                    args.getLong(KEY_ROWID)
                ).observeAndReportFailure()
            }

            R.id.UNARCHIVE_COMMAND -> {
                viewModel.unarchive(args.getLong(KEY_ROWID))
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

    private fun LiveData<Result<Unit>>.observeAndReportFailure() {
        observe(this@BaseMyExpenses) { result ->
            result.onFailure {
                report(it)
                showSnackBar(it.safeMessage)
            }
        }
    }

    val rendererFactory: RenderFactory =
        { renderType, account, withCategoryIcon, colorSource, onToggleCrStatus ->
            when (renderType) {

                RenderType.New -> {
                    NewTransactionRenderer(
                        dateTimeFormatter(account, prefHandler, this),
                        withCategoryIcon,
                        colorSource,
                        onToggleCrStatus
                    )
                }

                RenderType.Legacy -> {
                    CompactTransactionRenderer(
                        dateTimeFormatterLegacy(
                            account,
                            prefHandler,
                            this
                        )?.let {
                            DateTimeFormatInfo(
                                (it.first as SimpleDateFormat).asDateTimeFormatter,
                                it.second
                            )
                        },
                        withCategoryIcon,
                        prefHandler.getBoolean(
                            PrefKey.UI_ITEM_RENDERER_ORIGINAL_AMOUNT,
                            false
                        ),
                        colorSource,
                        onToggleCrStatus
                    )
                }
            }
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
            upgradeHandlerViewModel.upgrade(this, prevVersion, currentVersion)

            showVersionDialog(prevVersion)
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                if ((!licenceHandler.hasTrialAccessTo(ContribFeature.SYNCHRONIZATION)
                            && viewModel.repository.countAccounts(
                        "$KEY_SYNC_ACCOUNT_NAME IS NOT NULL",
                        null
                    ) > 0
                            && !prefHandler.getBoolean(
                        PrefKey.SYNC_UPSELL_NOTIFICATION_SHOWN,
                        false
                    )
                            )
                ) {
                    prefHandler.putBoolean(PrefKey.SYNC_UPSELL_NOTIFICATION_SHOWN, true)
                    ContribUtils.showContribNotification(
                        this@BaseMyExpenses,
                        ContribFeature.SYNCHRONIZATION
                    )
                }
            }
        }
        checkCalendarPermission()
    }

    private fun checkCalendarPermission() {
        if ("-1" != prefHandler.getString(PrefKey.PLANNER_CALENDAR_ID, "-1")) {
            checkPermissionsForPlaner()
        }
    }

    val sumInfo: MutableState<SumInfo> = mutableStateOf(SumInfo.EMPTY)

    val hasItems
        get() = sumInfo.value.hasItems

    @Composable
    fun Page(account: PageAccount, accountCount: Int, v2: Boolean = false) {
        val coroutineScope = rememberCoroutineScope()
        val preferredSearchType =
            viewModel.preferredSearchType.flow.collectAsState(TYPE_COMPLEX).value
        if (showFilterDialog) {
            FilterDialog(
                account = account,
                sumInfo = sumInfo.value,
                //we are only interested in the current value, since as soon as we persist new value,
                //the dialog is dismissed
                //noinspection StateFlowValueCalledInComposition
                criterion = currentFilter.whereFilter.value,
                initialPreferredSearchType = preferredSearchType,
                onDismissRequest = {
                    showFilterDialog = false
                }, onConfirmRequest = { preferredSearchType, criterion ->
                    coroutineScope.launch {
                        viewModel.preferredSearchType.set(preferredSearchType)
                        currentFilter.persist(criterion)
                        showFilterDialog = false
                        invalidateOptionsMenu()
                    }
                }
            )
        }

        LaunchedEffect(key1 = account.sealed) {
            if (account.sealed) finishActionMode()
        }

        val showStatusHandle =
            if (account.isAggregate || account.type?.supportsReconciliation == false)
                false
            else
                viewModel.showStatusHandle.flow.collectAsState(initial = true).value

        val onToggleCrStatus: ((Long) -> Unit)? = if (showStatusHandle) ::toggleCrStatus else null

        val headerData = remember(account) { viewModel.headerData(account, v2) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            val filter = viewModel.filterPersistence.getValue(account.id)
                .whereFilter
                .collectAsState(null)
            filter.value?.let { filter ->
                FilterHandler(account, "confirmFilterDirect_${account.id}", { oldValue, newValue ->
                    if (newValue != null && oldValue != null) {
                        lifecycleScope.launch {
                            currentFilter.replaceCriterion(oldValue, newValue)
                        }
                    }
                }) {
                    FilterCard(
                        filter,
                        editFilter = { handleEdit(it) },
                        clearAllFilter = { confirmClearFilter() },
                        clearFilter = {
                            lifecycleScope.launch {
                                currentFilter.removeCriterion(it)
                                invalidateOptionsMenu()
                            }
                        }
                    )
                }
            }

            headerData.collectAsState().value.let { headerData ->
                val lazyPagingItems =
                    viewModel.items.getValue(account).collectAsLazyPagingItems()
                if (!account.sealed) {
                    LaunchedEffect(viewModel.selectAllState.value) {
                        if (viewModel.selectAllState.value) {
                            if (lazyPagingItems.loadState.prepend.endOfPaginationReached &&
                                lazyPagingItems.loadState.append.endOfPaginationReached
                            ) {
                                var jndex = 0
                                while (jndex < lazyPagingItems.itemCount) {
                                    lazyPagingItems.peek(jndex)?.let {
                                        viewModel.selectionHandler.selectConditional(it)
                                    }
                                    jndex++
                                }
                            } else {
                                showSnackBar(
                                    getString(
                                        R.string.select_all_list_too_large,
                                        getString(android.R.string.selectAll)
                                    )
                                )
                            }
                            viewModel.selectAllState.value = false
                        }
                    }
                }
                val bulkDeleteState = viewModel.bulkDeleteState.collectAsState(initial = null)
                val modificationAllowed =
                    !account.sealed && bulkDeleteState.value !is DeleteProgress
                val colorSource =
                    viewModel.colorSource.collectAsState(initial = ColorSource.TYPE)
                val withCategoryIcon =
                    viewModel.withCategoryIcon.collectAsState(initial = true)
                val renderType = viewModel.renderer.collectAsState(initial = RenderType.New)
                val renderer = remember {
                    derivedStateOf {
                        Timber.d("init renderer ${renderType.value}")
                        rendererFactory(
                            renderType.value,
                            account,
                            withCategoryIcon.value,
                            colorSource.value,
                            onToggleCrStatus
                        )
                    }
                }
                TransactionList(
                    modifier = Modifier.weight(1f),
                    lazyPagingItems = lazyPagingItems,
                    headerData = headerData,
                    budgetData = remember(account.grouping) { viewModel.budgetData(account) }
                        .collectAsState(null),
                    selectionHandler = if (modificationAllowed) viewModel.selectionHandler else null,
                    selectAllState = viewModel.selectAllState,
                    onSelectAllListTooLarge = { selectAllListTooLarge() },
                    onEvent = object : TransactionEventHandler {
                        override fun invoke(event: TransactionEvent, transaction: Transaction2) {
                            when (event) {
                                TransactionEvent.ShowDetails -> {
                                    showDetails(
                                        transaction.id,
                                        transaction.isArchive,
                                        currentFilter.takeIf { transaction.isArchive },
                                        currentAccount?.sortOrder.takeIf { transaction.isArchive }
                                    )
                                }

                                TransactionEvent.UnArchive -> unarchive(transaction.id)
                                TransactionEvent.Delete -> lifecycleScope.launch {
                                    if (transaction.isArchive) {
                                        deleteArchive(transaction)
                                    } else {
                                        delete(listOf(transaction.id to transaction.crStatus))
                                    }
                                }

                                TransactionEvent.Edit -> edit(transaction, false)
                                TransactionEvent.Clone -> edit(transaction, true)
                                TransactionEvent.CreateTemplate -> createTemplate(transaction)
                                TransactionEvent.UnDelete -> undelete(listOf(transaction.id))
                                TransactionEvent.Select -> viewModel.selectionState.value =
                                    listOf(SelectionInfo(transaction))

                                TransactionEvent.Ungroup -> ungroupSplit(transaction)
                                TransactionEvent.Unlink -> unlinkTransfer(transaction)
                                TransactionEvent.TransformToTransfer -> transformToTransfer(
                                    transaction
                                )

                                TransactionEvent.AddFilterCategory -> addFilterCriterion(
                                    CategoryCriterion(
                                        transaction.categoryPath!!,
                                        transaction.catId!!
                                    )
                                )

                                TransactionEvent.AddFilterPayee -> addFilterCriterion(
                                    PayeeCriterion(
                                        transaction.party!!.name,
                                        transaction.party.id!!
                                    )
                                )

                                TransactionEvent.AddFilterAmount -> addFilterCriterion(
                                    AmountCriterion(
                                        operation = Operation.EQ,
                                        values = listOf(transaction.displayAmount.amountMinor),
                                        currency = transaction.displayAmount.currencyUnit.code,
                                        sign = transaction.displayAmount.amountMinor > 0
                                    )
                                )

                                TransactionEvent.AddFilterMethod -> addFilterCriterion(
                                    MethodCriterion(
                                        transaction.methodLabel!!.translateIfPredefined(this@BaseMyExpenses),
                                        transaction.methodId!!
                                    )
                                )

                                TransactionEvent.AddFilterTag -> addFilterCriterion(
                                    TagCriterion(
                                        transaction.tagList.joinToString { it.second },
                                        transaction.tagList.map { it.first }
                                    )
                                )

                                TransactionEvent.AddFilterComment -> addFilterCriterion(
                                    CommentCriterion(transaction.comment)
                                )
                            }
                        }

                    },
                    futureCriterion = viewModel.futureCriterion.collectAsState(initial = FutureCriterion.EndOfDay).value,
                    expansionHandler = viewModel.expansionHandlerForTransactionGroups(account),
                    onBudgetClick = { budgetId, headerId ->
                        contribFeatureRequested(ContribFeature.BUDGET, budgetId to headerId)
                    },
                    showSumDetails = viewModel.showSumDetails.collectAsState(initial = true).value,
                    scrollToCurrentDate = viewModel.scrollToCurrentDate.getValue(account.id),
                    renderer = renderer.value,
                    isFiltered = filter.value != null,
                    splitInfoResolver = {
                        viewModel.splitInfo(it)
                    },
                    windowInsets = transactionListWindowInsets,
                    modificationsAllowed = modificationAllowed,
                    accountCount = accountCount
                )
            }
        }
    }


    fun confirmClearFilter() {
        ConfirmationDialogFragment.newInstance(Bundle().apply {
            putString(KEY_MESSAGE, getString(R.string.clear_all_filters))
            putInt(KEY_COMMAND_POSITIVE, R.id.CLEAR_FILTER_COMMAND)
        }).show(supportFragmentManager, "CLEAR_FILTER")
    }

    protected var showFilterDialog
        get() = viewModel.showFilterDialog
        set(value) {
            viewModel.showFilterDialog = value
        }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.extras?.let {
            selectedAccountId = it.getLong(KEY_ROWID)
        }
        showTransactionFromIntent(intent)
    }

    fun showTransactionFromIntent(intent: Intent) {
        val transactionId = intent.getLongExtra(KEY_TRANSACTIONID, -1L)
        if (transactionId != -1L) {
            showDetails(transactionId)
            intent.removeExtra(KEY_TRANSACTIONID)
        }
    }

    override fun onPdfResultProcessed() {
        viewModel.pdfResultProcessed()
    }
}
