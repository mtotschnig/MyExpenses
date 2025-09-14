package org.totschnig.myexpenses.activity

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Loupe
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.Insets
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.AmountInput
import eltos.simpledialogfragment.form.AmountInputHostDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AccountList
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.CompactTransactionRenderer
import org.totschnig.myexpenses.compose.DateTimeFormatInfo
import org.totschnig.myexpenses.compose.FutureCriterion
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.MenuEntry.Companion.delete
import org.totschnig.myexpenses.compose.MenuEntry.Companion.edit
import org.totschnig.myexpenses.compose.MenuEntry.Companion.select
import org.totschnig.myexpenses.compose.NewTransactionRenderer
import org.totschnig.myexpenses.compose.RenderType
import org.totschnig.myexpenses.compose.SubMenuEntry
import org.totschnig.myexpenses.compose.TEST_TAG_PAGER
import org.totschnig.myexpenses.compose.TransactionList
import org.totschnig.myexpenses.compose.UiText
import org.totschnig.myexpenses.compose.filter.FilterCard
import org.totschnig.myexpenses.compose.filter.FilterDialog
import org.totschnig.myexpenses.compose.filter.FilterHandler
import org.totschnig.myexpenses.compose.filter.TYPE_COMPLEX
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.databinding.ActivityMainBinding
import org.totschnig.myexpenses.db2.countAccounts
import org.totschnig.myexpenses.dialog.ArchiveDialogFragment
import org.totschnig.myexpenses.dialog.BalanceDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_CHECKBOX_LABEL
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_COMMAND_NEGATIVE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_COMMAND_POSITIVE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_MESSAGE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_POSITIVE_BUTTON_LABEL
import org.totschnig.myexpenses.dialog.CriterionInfo
import org.totschnig.myexpenses.dialog.CriterionReachedDialogFragment
import org.totschnig.myexpenses.dialog.ExportDialogFragment
import org.totschnig.myexpenses.dialog.HelpDialogFragment
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.dialog.ProgressDialogFragment
import org.totschnig.myexpenses.dialog.TransactionListComposeDialogFragment
import org.totschnig.myexpenses.dialog.progress.NewProgressDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectTransformToTransferTargetDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectTransformToTransferTargetDialogFragment.Companion.KEY_IS_INCOME
import org.totschnig.myexpenses.dialog.select.SelectTransformToTransferTargetDialogFragment.Companion.TRANSFORM_TO_TRANSFER_REQUEST
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod.Companion.translateIfPredefined
import org.totschnig.myexpenses.preference.ColorSource
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.CheckSealedHandler
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.isAggregate
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CLEARED_TOTAL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_RECONCILED_TOTAL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR
import org.totschnig.myexpenses.provider.TransactionDatabase.SQLiteDowngradeFailedException
import org.totschnig.myexpenses.provider.TransactionDatabase.SQLiteUpgradeFailedException
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
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
import org.totschnig.myexpenses.retrofit.Vote
import org.totschnig.myexpenses.ui.DiscoveryHelper
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.ui.SnackbarAction
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.ContribUtils
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.TextUtils.withAmountColor
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.checkMenuIcon
import org.totschnig.myexpenses.util.configureSortDirectionMenu
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.util.distrib.DistributionHelper.isGithub
import org.totschnig.myexpenses.util.distrib.ReviewManager
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.getSortDirectionFromMenuItemId
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.util.setEnabledAndVisible
import org.totschnig.myexpenses.util.ui.DisplayProgress
import org.totschnig.myexpenses.util.ui.asDateTimeFormatter
import org.totschnig.myexpenses.util.ui.dateTimeFormatter
import org.totschnig.myexpenses.util.ui.dateTimeFormatterLegacy
import org.totschnig.myexpenses.util.ui.displayProgress
import org.totschnig.myexpenses.util.ui.getAmountColor
import org.totschnig.myexpenses.viewmodel.AccountSealedException
import org.totschnig.myexpenses.viewmodel.CompletedAction
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel.DeleteState.DeleteComplete
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel.DeleteState.DeleteProgress
import org.totschnig.myexpenses.viewmodel.ExportViewModel
import org.totschnig.myexpenses.viewmodel.KEY_ROW_IDS
import org.totschnig.myexpenses.viewmodel.ModalProgressViewModel
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel
import org.totschnig.myexpenses.viewmodel.OpenAction
import org.totschnig.myexpenses.viewmodel.PriceCalculationViewModel
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel
import org.totschnig.myexpenses.viewmodel.ShareAction
import org.totschnig.myexpenses.viewmodel.SumInfo
import org.totschnig.myexpenses.viewmodel.TransactionListViewModel
import org.totschnig.myexpenses.viewmodel.UpgradeHandlerViewModel
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import org.totschnig.myexpenses.viewmodel.repository.RoadmapRepository
import timber.log.Timber
import java.io.Serializable
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlin.math.sign

const val DIALOG_TAG_OCR_DISAMBIGUATE = "DISAMBIGUATE"
const val DIALOG_TAG_NEW_BALANCE = "NEW_BALANCE"

abstract class BaseMyExpenses : LaunchActivity(), OnDialogResultListener, ContribIFace,
    NewProgressDialogFragment.Host {
    override val fabActionName = "CREATE_TRANSACTION"

    private val accountData: List<FullAccount>
        get() = viewModel.accountData.value?.getOrNull() ?: emptyList()

    var selectedAccountId: Long
        get() = viewModel.selectedAccountId
        set(value) {
            viewModel.selectAccount(value)
        }

    private val accountForNewTransaction: FullAccount?
        get() = currentAccount?.let { current ->
            current.takeIf { !it.isAggregate } ?: viewModel.accountData.value?.getOrNull()
                ?.filter { !it.isAggregate && (current.isHomeAggregate || it.currency == current.currency) }
                ?.maxByOrNull { it.lastUsed }
        }

    val currentFilter: FilterPersistence
        get() = viewModel.filterPersistence.getValue(selectedAccountId)

    val currentAccount: FullAccount?
        get() = accountData.find { it.id == selectedAccountId }

    private val currentPage: Int
        get() = accountData.indexOfFirst { it.id == selectedAccountId }.coerceAtLeast(0)

    @Inject
    lateinit var discoveryHelper: IDiscoveryHelper

    @Inject
    lateinit var reviewManager: ReviewManager

    @Inject
    lateinit var modelClass: Class<out MyExpensesViewModel>

    private var drawerToggle: ActionBarDrawerToggle? = null

    private var currentBalance: String = ""

    lateinit var viewModel: MyExpensesViewModel
    private val upgradeHandlerViewModel: UpgradeHandlerViewModel by viewModels()
    private val exportViewModel: ExportViewModel by viewModels()
    private val roadmapViewModel: RoadmapViewModel by viewModels()
    private val progressViewModel: ModalProgressViewModel by viewModels()
    private val pricesViewModel: PriceCalculationViewModel by viewModels()

    lateinit var binding: ActivityMainBinding

    val accountCount
        get() = accountData.count { it.id > 0 }

    suspend fun getHiddenAccountCount() = withContext(Dispatchers.IO) {
        viewModel.accountFlagsRaw.first().filter { !it.isVisible }.sumOf { it.count ?: 0 }
    }

    private var actionMode: ActionMode? = null

    var selectionState
        get() = viewModel.selectionState.value
        set(value) {
            viewModel.selectionState.value = value
        }

    private val selectAllState: MutableState<Boolean> = mutableStateOf(false)

    var sumInfo: MutableState<SumInfo> = mutableStateOf(SumInfo.EMPTY)

    private var showFilterDialog
        get() = viewModel.showFilterDialog
        set(value) {
            viewModel.showFilterDialog = value
        }

    fun finishActionMode() {
        actionMode?.finish()
    }

    override fun onWebUiActivated() {
        invalidateOptionsMenu()
    }

    private val formattedSelectedTransactionSum
        get() = with(viewModel.selectedTransactionSum) {
            currencyFormatter.convAmount(this, currentAccount!!.currencyUnit)
                .withAmountColor(resources, sign)
        }

    private fun updateActionModeTitle() {
        actionMode?.title = if (selectionState.size > 1) {
            android.text.TextUtils.concat(
                selectionState.size.toString(),
                " (Σ: ",
                formattedSelectedTransactionSum,
                ")"
            )
        } else selectionState.size.toString()
    }

    private fun startMyActionMode() {
        if (actionMode == null) {
            actionMode = startSupportActionMode(object : ActionMode.Callback {
                override fun onCreateActionMode(
                    mode: ActionMode,
                    menu: Menu,
                ): Boolean {
                    return currentAccount?.let {
                        if (!it.sealed) {
                            menuInflater.inflate(R.menu.transactionlist_context, menu)
                            if (resources.getBoolean(R.bool.showTransactionBulkActions)) {
                                listOf(
                                    R.id.DELETE_COMMAND,
                                    R.id.MAP_TAG_COMMAND,
                                    R.id.SPLIT_TRANSACTION_COMMAND,
                                    R.id.REMAP_PARENT,
                                    R.id.LINK_TRANSFER_COMMAND,
                                    R.id.SELECT_ALL_COMMAND,
                                    R.id.UNDELETE_COMMAND
                                ).forEach {
                                    menu.findItem(it)
                                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                                }
                            }
                        }
                    } != null
                }

                override fun onPrepareActionMode(
                    mode: ActionMode,
                    menu: Menu,
                ) = with(menu) {
                    findItem(R.id.REMAP_ACCOUNT_COMMAND).isVisible = accountCount > 1
                    val hasTransfer = selectionState.any { it.isTransfer }
                    val hasSplit = selectionState.any { it.isSplit }
                    val hasVoid = selectionState.any { it.crStatus == CrStatus.VOID }
                    findItem(R.id.REMAP_PAYEE_COMMAND).isVisible = !hasTransfer
                    findItem(R.id.REMAP_CATEGORY_COMMAND).isVisible = !hasSplit
                    findItem(R.id.REMAP_METHOD_COMMAND).isVisible = !hasTransfer
                    findItem(R.id.SPLIT_TRANSACTION_COMMAND).isVisible = !hasSplit && !hasVoid
                    findItem(R.id.LINK_TRANSFER_COMMAND).isVisible =
                        selectionState.count() == 2 &&
                                !hasSplit && !hasTransfer && !hasVoid &&
                                viewModel.canLinkSelection()
                    findItem(R.id.UNDELETE_COMMAND).isVisible = hasVoid
                    true
                }

                override fun onActionItemClicked(
                    mode: ActionMode,
                    item: MenuItem,
                ): Boolean {
                    if (remapHandler.handleActionItemClick(item.itemId)) return true
                    when (item.itemId) {
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

                override fun onDestroyActionMode(mode: ActionMode) {
                    actionMode = null
                    selectionState = emptyList()
                }

            })
        } else actionMode?.invalidate()
        updateActionModeTitle()
    }

    private fun selectAll() {
        selectAllState.value = true
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

    lateinit var remapHandler: RemapHandler
    lateinit var tagHandler: TagHandler

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (savedInstanceState == null) {
            floatingActionButton.let {
                discoveryHelper.discover(
                    this,
                    it,
                    3,
                    DiscoveryHelper.Feature.FabLongPress
                )
            }
        }
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle?.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle?.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return (drawerToggle?.onOptionsItemSelected(item) == true) || when (item.itemId) {
            R.id.SCAN_MODE_COMMAND -> {
                toggleScanMode()
                true
            }

            R.id.SHOW_STATUS_HANDLE_COMMAND -> {
                currentAccount?.let {
                    viewModel.persistShowStatusHandle(!item.isChecked)
                    invalidateOptionsMenu()
                }
                true
            }

            R.id.WEB_UI_COMMAND -> {
                toggleWebUI()
                true
            }

            R.id.ARCHIVE_COMMAND -> {
                currentAccount?.let {
                    ArchiveDialogFragment.newInstance(it).show(supportFragmentManager, "ARCHIVE")
                }
                true
            }

            R.id.SEARCH_COMMAND -> {
                showFilterDialog = true
                true
            }

            else -> handleGrouping(item) ||
                    handleSortDirection(item) ||
                    super.onOptionsItemSelected(item)
        }
    }

    protected open fun handleSortDirection(item: MenuItem) =
        getSortDirectionFromMenuItemId(item.itemId)?.let { newSortDirection ->
            if (!item.isChecked) {
                viewModel.persistSortDirection(selectedAccountId, newSortDirection)
            }
            true
        } == true

    private fun handleGrouping(item: MenuItem) =
        Utils.getGroupingFromMenuItemId(item.itemId)?.let { newGrouping ->
            if (!item.isChecked) {
                viewModel.persistGrouping(selectedAccountId, newGrouping)
            }
            true
        } == true

    private fun toggleScanMode() {
        if (isScanMode()) {
            prefHandler.putBoolean(PrefKey.OCR, false)
            updateFab()
            invalidateOptionsMenu()
        } else {
            contribFeatureRequested(ContribFeature.OCR, false)
        }
    }

    private fun toggleWebUI() {
        if (prefHandler.getBoolean(PrefKey.UI_WEB, false)) {
            prefHandler.putBoolean(PrefKey.UI_WEB, false)
        } else {
            contribFeatureRequested(ContribFeature.WEB_UI, false)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        if (key != null && prefHandler.matches(key, PrefKey.UI_WEB)) {
            invalidateOptionsMenu()
        }
    }

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLocaleContext()
        viewModel = ViewModelProvider(this)[modelClass]
        with(injector) {
            inject(viewModel)
            inject(upgradeHandlerViewModel)
            inject(exportViewModel)
            inject(roadmapViewModel)
            inject(pricesViewModel)
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(false)
        toolbar.isVisible = false
        if (savedInstanceState == null) {
            newVersionCheck()
            //voteReminderCheck();
            selectedAccountId = prefHandler.getLong(PrefKey.CURRENT_ACCOUNT, 0L)
        }

        binding.viewPagerMain.viewPager.setContent {
            val upgradeInfo = upgradeHandlerViewModel.upgradeInfo.collectAsState().value
            if (upgradeInfo == null || upgradeInfo is UpgradeHandlerViewModel.UpgradeSuccess) {
                MainContent()
            }
        }
        setupToolbarClickHandlers()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                upgradeHandlerViewModel.upgradeInfo.collect { info ->
                    when (info) {
                        is UpgradeHandlerViewModel.UpgradeError -> {
                            showMessage(
                                info.info,
                                null,
                                null,
                                MessageDialogFragment.Button(
                                    R.string.button_label_close,
                                    R.id.QUIT_COMMAND,
                                    null
                                ),
                                false
                            )
                        }

                        is UpgradeHandlerViewModel.UpgradeSuccess -> {
                            showDismissibleSnackBar(
                                message = info.info,
                                actionLabel = getString(R.string.dialog_dismiss) +
                                        if (info.count > 1) " (${info.index} / ${info.count})" else "",
                                callback = object : Snackbar.Callback() {
                                    override fun onDismissed(
                                        transientBottomBar: Snackbar,
                                        event: Int,
                                    ) {
                                        if (event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_ACTION)
                                            upgradeHandlerViewModel.messageShown()
                                    }
                                })
                        }

                        else -> {}
                    }
                }
            }
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
                viewModel.pdfResult.collectPrintResult()
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

        if (resources.getDimensionPixelSize(R.dimen.drawerWidth) > resources.displayMetrics.widthPixels) {
            binding.accountPanel.root.layoutParams.width = resources.displayMetrics.widthPixels
        }

        binding.accountPanel.accountList.setContent {
            AppTheme {
                viewModel.accountData.collectAsStateWithLifecycle().value.let { result ->
                    result?.onSuccess { data ->
                        val banks = viewModel.banks.collectAsState()
                        LaunchedEffect(Unit) {
                            toolbar.isVisible = true
                        }
                        LaunchedEffect(data) {
                            val selectedIndex = data.indexOfFirst { it.id == selectedAccountId }
                            if (selectedIndex == -1) {
                                selectedAccountId = data.firstOrNull()?.id ?: 0L
                            } else {
                                viewModel.scrollToAccountIfNeeded(
                                    selectedIndex,
                                    selectedAccountId,
                                    false
                                )
                            }
                            navigationView.menu.findItem(R.id.EQUIVALENT_WORTH_COMMAND).isVisible =
                                data.any { it.isHomeAggregate }
                        }

                        val accountGrouping = viewModel.accountGrouping.collectAsState(
                            AccountGrouping.TYPE
                        )

                        AccountList(
                            accountData = data,
                            grouping = accountGrouping.value,
                            selectedAccount = selectedAccountId,
                            onSelected = {
                                selectedAccountId = it
                                closeDrawer()
                            },
                            onEdit = {
                                closeDrawer()
                                editAccount(it)
                            },
                            onDelete = {
                                closeDrawer()
                                confirmAccountDelete(it)
                            },
                            onSetFlag = { accountId, flagId ->
                                viewModel.setFlag(
                                    accountId,
                                    flagId
                                )
                            },
                            onToggleSealed = { toggleAccountSealed(it) },
                            onToggleExcludeFromTotals = { toggleExcludeFromTotals(it) },
                            onToggleDynamicExchangeRate = if (viewModel.dynamicExchangeRatesPerAccount.collectAsState(
                                    true
                                ).value
                            ) {
                                { toggleDynamicExchangeRate(it) }
                            } else null,
                            listState = viewModel.listState,
                            showEquivalentWorth = viewModel.showEquivalentWorth()
                                .collectAsState(false).value,
                            expansionHandlerGroups = viewModel.expansionHandler("collapsedHeadersDrawer_${accountGrouping.value}"),
                            expansionHandlerAccounts = viewModel.expansionHandler("expandedAccounts"),
                            bankIcon = { modifier, id ->
                                banks.value.find { it.id == id }
                                    ?.let { bank ->
                                        bankingFeature.bankIconRenderer?.invoke(
                                            modifier,
                                            bank
                                        )
                                    }
                            },
                            flags = viewModel.accountFlags.collectAsState(emptyList()).value
                        )
                    }?.onFailure {
                        val (message, forceQuit) = when (it) {
                            is SQLiteDowngradeFailedException -> "Database cannot be downgraded from a newer version. Please either uninstall MyExpenses, before reinstalling, or upgrade to a new version." to true
                            is SQLiteUpgradeFailedException -> "Database upgrade failed. Please contact ${
                                getString(
                                    R.string.support_email
                                )
                            } !" to true

                            else -> "Data loading failed" to false
                        }
                        showMessage(
                            message,
                            if (!forceQuit) {
                                MessageDialogFragment.Button(
                                    R.string.safe_mode,
                                    R.id.SAFE_MODE_COMMAND,
                                    null
                                )
                            } else null,
                            null,
                            MessageDialogFragment.Button(
                                R.string.button_label_close,
                                R.id.QUIT_COMMAND,
                                null
                            ),
                            false
                        )
                    }
                }

            }
        }
        remapHandler = RemapHandler(this)
        tagHandler = TagHandler(this)

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
        if (resources.getInteger(R.integer.window_size_class) == 1) {
            toolbar.setNavigationIcon(R.drawable.ic_menu)
            binding.accountPanel.root.isVisible =
                prefHandler.getBoolean(PrefKey.ACCOUNT_PANEL_VISIBLE, false) ||
                        viewModel.showBalanceSheet
            toolbar.setNavigationOnClickListener {
                val newState = !binding.accountPanel.root.isVisible
                prefHandler.putBoolean(PrefKey.ACCOUNT_PANEL_VISIBLE, newState)
                binding.accountPanel.root.isVisible = newState
                toolbar.setNavigationContentDescription(
                    if (newState) R.string.drawer_close else R.string.drawer_open
                )
                ViewCompat.requestApplyInsets(binding.mainContent)
            }
        }

        floatingActionButton = binding.fab.CREATECOMMAND
        updateFab()
        setupFabSubMenu()

        ViewCompat.setOnApplyWindowInsetsListener(binding.accountPanel.root) { v, insets ->
            val isLtr = v.layoutDirection == View.LAYOUT_DIRECTION_LTR
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                val left =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars() + WindowInsetsCompat.Type.displayCutout()).left
                val right =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars() + WindowInsetsCompat.Type.displayCutout()).right
                leftMargin = when {
                    isLtr -> left
                    else -> 0
                }
                rightMargin = when {
                    !isLtr -> right
                    else -> 0
                }
            }
            //make account list and balance sheet aware of bottom inset
            ViewCompat.dispatchApplyWindowInsets(v.findViewById(R.id.accountList), insets)
            ViewCompat.dispatchApplyWindowInsets(v.findViewById(R.id.balanceSheet), insets)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContent) { v, insets ->
            val accountPanelIsVisible = binding.accountPanel.root.isVisible
            val isLtr = v.layoutDirection == View.LAYOUT_DIRECTION_LTR
            val originalNavigationBarInsets =
                insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val originalDisplayCutoutInsets =
                insets.getInsets(WindowInsetsCompat.Type.displayCutout())

            val systemBarsInsetsForChildren = Insets.of(
                if (accountPanelIsVisible && isLtr) 0 else originalNavigationBarInsets.left,
                originalNavigationBarInsets.top,
                if (accountPanelIsVisible && !isLtr) 0 else originalNavigationBarInsets.right,
                originalNavigationBarInsets.bottom
            )

            val displayCutoutInsetsForChildren: Insets = Insets.of(
                if (accountPanelIsVisible && isLtr) 0 else originalDisplayCutoutInsets.left,
                originalDisplayCutoutInsets.top,
                if (accountPanelIsVisible && !isLtr) 0 else originalDisplayCutoutInsets.right,
                originalDisplayCutoutInsets.bottom
            )

            val insetsForChildren = WindowInsetsCompat.Builder(insets)
                .setInsets(WindowInsetsCompat.Type.systemBars(), systemBarsInsetsForChildren)
                .setInsets(
                    WindowInsetsCompat.Type.displayCutout(),
                    displayCutoutInsetsForChildren
                )
                .build()

            insetsForChildren
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar.root) { v, insets ->
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars() + WindowInsetsCompat.Type.displayCutout()).left
                rightMargin =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars() + WindowInsetsCompat.Type.displayCutout()).right
            }
            WindowInsetsCompat.CONSUMED
        }
        binding.drawer?.let { drawer ->
            drawerToggle = object : ActionBarDrawerToggle(
                this, drawer,
                toolbar, R.string.drawer_open, R.string.drawer_close
            ) {
                //at the moment we finish action if drawer is opened;
                //and do NOT open it again when drawer is closed
                override fun onDrawerOpened(drawerView: View) {
                    super.onDrawerOpened(drawerView)
                    onBackPressedCallback.isEnabled = true
                    finishActionMode()
                }

                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                    super.onDrawerSlide(drawerView, 0f) // this disables the animation
                }

                override fun onDrawerClosed(drawerView: View) {
                    super.onDrawerClosed(drawerView)
                    onBackPressedCallback.isEnabled = false
                }
            }.also {
                drawer.addDrawerListener(it)
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

        with(navigationView) {
            setNavigationItemSelectedListener(::handleNavigationClick)
            getChildAt(0)?.isVerticalScrollBarEnabled = false
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.showEquivalentWorth().collect {
                        configureEquivalentWorthMenuItemIcon(
                            menu.findItem(R.id.EQUIVALENT_WORTH_COMMAND),
                            it
                        )
                    }
                }
            }
        }

        onBackPressedCallback = object : OnBackPressedCallback(viewModel.showBalanceSheet) {
            override fun handleOnBackPressed() {
                if (!closeBalanceSheet() &&
                    binding.drawer?.isDrawerOpen(GravityCompat.START) == true
                ) {
                    binding.drawer?.closeDrawer(GravityCompat.START)
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        if (viewModel.showBalanceSheet) {
            openBalanceSheet()
        }
    }

    override fun onPdfResultProcessed() {
        viewModel.pdfResultProcessed()
    }

    override val snackBarContainerId = R.id.main_content

    private fun editAccount(account: FullAccount) {
        startActivityForResult(Intent(this, AccountEdit::class.java).apply {
            putExtra(KEY_ROWID, account.id)
            putExtra(KEY_COLOR, account._color)
        }, EDIT_ACCOUNT_REQUEST)

    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun MainContent() {

        LaunchedEffect(currentAccount?.id) {
            with(currentAccount) {
                configureUiWithCurrentAccount(this, false)
                if (this != null) {
                    finishActionMode()
                    sumInfo.value = SumInfo.EMPTY
                    invalidateOptionsMenu()
                    viewModel.sumInfo(toPageAccount).collect {
                        invalidateOptionsMenu()
                        sumInfo.value = it
                    }
                }
            }
        }
        val result = viewModel.accountData.collectAsState()

        if (result.value?.isSuccess == true) {
            val accountData = result.value!!.getOrThrow()
            AppTheme {
                LaunchedEffect(accountData) {
                    if (accountData.isNotEmpty()) {
                        currentAccount?.let {
                            lifecycleScope.launch {
                                viewModel.sumInfo(it.toPageAccount).collect {
                                    invalidateOptionsMenu()
                                    sumInfo.value = it
                                }
                            }
                        }
                        configureUiWithCurrentAccount(currentAccount, true)
                    } else {
                        setTitle(R.string.app_name)
                        toolbar.subtitle = null
                    }
                }

                if (accountData.isNotEmpty()) {
                    val pagerState = rememberPagerState(
                        accountData.indexOfFirst { viewModel.selectedAccountId == it.id }
                            .coerceAtLeast(0)
                    ) { accountData.count() }
                    LaunchedEffect(viewModel.selectedAccountId) {
                        if (pagerState.currentPage != currentPage) {
                            pagerState.scrollToPage(currentPage)
                        }
                    }
                    LaunchedEffect(pagerState.settledPage) {
                        selectedAccountId = accountData[pagerState.settledPage].id
                        viewModel.scrollToAccountIfNeeded(
                            pagerState.currentPage,
                            selectedAccountId,
                            true
                        )
                    }
                    val coroutineScope = rememberCoroutineScope()
                    val preferredSearchType =
                        viewModel.preferredSearchType().collectAsState(TYPE_COMPLEX).value
                    if (showFilterDialog) {
                        currentAccount?.let {
                            FilterDialog(
                                account = it,
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
                                        viewModel.persistPreferredSearchType(preferredSearchType)
                                        currentFilter.persist(criterion)
                                        showFilterDialog = false
                                        invalidateOptionsMenu()
                                    }
                                }
                            )
                        }
                    }
                    HorizontalPager(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.onSurface)
                            .testTag(TEST_TAG_PAGER)
                            .semantics {
                                collectionInfo = CollectionInfo(1, accountData.size)
                            },
                        verticalAlignment = Alignment.Top,
                        state = pagerState,
                        pageSpacing = 10.dp,
                        key = { accountData[it].id }
                    ) {
                        Timber.i("Rendering page $it")
                        Page(account = accountData[it].toPageAccount, preferredSearchType)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(dimensionResource(id = R.dimen.padding_main_screen)),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            modifier = Modifier.wrapContentSize(),
                            textAlign = TextAlign.Center,
                            text = stringResource(id = R.string.no_accounts)
                        )
                        Button(onClick = { createAccountDo() }) {
                            Text(text = stringResource(id = R.string.menu_create_account))
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Page(account: PageAccount, preferredSearchType: Int) {

        LaunchedEffect(key1 = account.sealed) {
            if (account.sealed) finishActionMode()
        }

        val showStatusHandle = if (account.isAggregate || !account.type.supportsReconciliation)
            false
        else
            viewModel.showStatusHandle().collectAsState(initial = true).value

        val onToggleCrStatus: ((Long) -> Unit)? = if (showStatusHandle) {
            {
                checkSealed(listOf(it), withTransfer = false) {
                    viewModel.toggleCrStatus(it)
                }
            }
        } else null

        val headerData = remember(account) { viewModel.headerData(account) }

        LaunchedEffect(selectionState.size) {
            if (selectionState.isNotEmpty()) {
                startMyActionMode()
            } else {
                finishActionMode()
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            val filter = viewModel.filterPersistence.getValue(account.id)
                .whereFilter
                .collectAsState(null)
            filter.value?.let { filter ->
                FilterHandler(account, "confirmFilterDirect_${account.id}", {
                    oldValue, newValue ->
                    if (newValue != null && oldValue != null) {
                        lifecycleScope.launch {
                            currentFilter.replaceCriterion(oldValue, newValue)
                        }
                    }
                }) {
                    FilterCard(filter,
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
                val withCategoryIcon =
                    viewModel.withCategoryIcon.collectAsState(initial = true).value
                val lazyPagingItems =
                    viewModel.items.getValue(account).collectAsLazyPagingItems()
                if (!account.sealed) {
                    LaunchedEffect(selectAllState.value) {
                        if (selectAllState.value) {
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
                            selectAllState.value = false
                        }
                    }
                }
                val bulkDeleteState = viewModel.bulkDeleteState.collectAsState(initial = null)
                val modificationAllowed =
                    !account.sealed && bulkDeleteState.value !is DeleteProgress
                val colorSource =
                    viewModel.colorSource.collectAsState(initial = ColorSource.TYPE).value
                TransactionList(
                    modifier = Modifier.weight(1f),
                    lazyPagingItems = lazyPagingItems,
                    headerData = headerData,
                    budgetData = remember(account.grouping) { viewModel.budgetData(account) }
                        .collectAsState(null),
                    selectionHandler = if (modificationAllowed) viewModel.selectionHandler else null,
                    menuGenerator = remember(modificationAllowed) {
                        { transaction ->
                            org.totschnig.myexpenses.compose.Menu(
                                buildList {
                                    add(
                                        MenuEntry(
                                            icon = Icons.Filled.Loupe,
                                            label = R.string.details,
                                            command = "DETAILS"
                                        ) {
                                            showDetails(
                                                transaction.id,
                                                transaction.isArchive,
                                                currentFilter.takeIf { transaction.isArchive },
                                                currentAccount?.sortOrder.takeIf { transaction.isArchive }
                                            )
                                        })
                                    if (modificationAllowed) {
                                        if (transaction.isArchive) {
                                            add(
                                                MenuEntry(
                                                    icon = Icons.Filled.Unarchive,
                                                    label = R.string.menu_unpack,
                                                    command = "UNPACK_ARCHIVE"
                                                ) {
                                                    unarchive(transaction)
                                                })
                                            add(delete("DELETE_TRANSACTION") {
                                                lifecycleScope.launch {
                                                    deleteArchive(transaction)
                                                }
                                            })
                                        } else {
                                            add(
                                                MenuEntry(
                                                    icon = Icons.Filled.ContentCopy,
                                                    label = R.string.menu_clone_transaction,
                                                    command = "CLONE"
                                                ) {
                                                    edit(transaction, true)
                                                })
                                            add(
                                                MenuEntry(
                                                    icon = myiconpack.IcActionTemplateAdd,
                                                    label = R.string.menu_create_template_from_transaction,
                                                    command = "CREATE_TEMPLATE_FROM_TRANSACTION"
                                                ) { createTemplate(transaction) })
                                            if (transaction.crStatus == CrStatus.VOID) {
                                                add(
                                                    MenuEntry(
                                                        icon = Icons.Filled.RestoreFromTrash,
                                                        label = R.string.menu_undelete_transaction,
                                                        command = "UNDELETE_TRANSACTION"
                                                    ) {
                                                        undelete(listOf(transaction.id))
                                                    })
                                            }
                                            if (transaction.crStatus != CrStatus.VOID) {
                                                add(edit("EDIT_TRANSACTION") {
                                                    edit(transaction)
                                                })
                                            }
                                            add(delete("DELETE_TRANSACTION") {
                                                delete(listOf(transaction.id to transaction.crStatus))
                                            })
                                            add(
                                                select("SELECT_TRANSACTION") {
                                                    viewModel.selectionState.value = listOf(
                                                        MyExpensesViewModel.SelectionInfo(
                                                            transaction
                                                        )
                                                    )
                                                }
                                            )
                                            when {
                                                transaction.isSplit -> {
                                                    add(
                                                        MenuEntry(
                                                            icon = Icons.AutoMirrored.Filled.CallSplit,
                                                            label = R.string.menu_ungroup_split_transaction,
                                                            command = "UNGROUP_SPLIT"
                                                        ) {
                                                            ungroupSplit(transaction)
                                                        })
                                                }

                                                transaction.isTransfer -> {
                                                    add(
                                                        MenuEntry(
                                                            icon = Icons.Filled.LinkOff,
                                                            label = R.string.menu_unlink_transfer,
                                                            command = "UNLINK_TRANSFER"
                                                        ) {
                                                            unlinkTransfer(transaction)
                                                        })
                                                }

                                                else -> {
                                                    if (accountCount >= 2) {
                                                        add(
                                                            MenuEntry(
                                                                icon = Icons.Filled.Link,
                                                                label = R.string.menu_transform_to_transfer,
                                                                command = "TRANSFORM_TRANSFER"
                                                            ) {
                                                                transformToTransfer(transaction)
                                                            })
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    add(
                                        SubMenuEntry(
                                            icon = Icons.Filled.Search,
                                            label = R.string.filter,
                                            subMenu = org.totschnig.myexpenses.compose.Menu(
                                                buildList {
                                                    if (transaction.catId != null && !transaction.isSplit) {
                                                        if (transaction.categoryPath != null) {
                                                            add(
                                                                MenuEntry(
                                                                    label = UiText.StringValue(
                                                                        transaction.categoryPath
                                                                    ),
                                                                    command = "FILTER_FOR_CATEGORY"
                                                                ) {
                                                                    addFilterCriterion(
                                                                        CategoryCriterion(
                                                                            transaction.categoryPath,
                                                                            transaction.catId
                                                                        )
                                                                    )
                                                                })
                                                        } else {
                                                            CrashHandler.report(
                                                                IllegalStateException("Category path is null")
                                                            )
                                                        }
                                                    }
                                                    if (transaction.party?.id != null) {
                                                        add(
                                                            MenuEntry(
                                                                label = UiText.StringValue(
                                                                    transaction.party.name
                                                                ),
                                                                command = "FILTER_FOR_PAYEE"
                                                            ) {
                                                                addFilterCriterion(
                                                                    PayeeCriterion(
                                                                        transaction.party.name,
                                                                        transaction.party.id
                                                                    )
                                                                )
                                                            }
                                                        )
                                                    }
                                                    if (transaction.methodId != null) {
                                                        val label =
                                                            transaction.methodLabel!!.translateIfPredefined(
                                                                this@BaseMyExpenses
                                                            )
                                                        add(
                                                            MenuEntry(
                                                                label = UiText.StringValue(label),
                                                                command = "FILTER_FOR_METHOD"
                                                            ) {
                                                                addFilterCriterion(
                                                                    MethodCriterion(
                                                                        label,
                                                                        transaction.methodId
                                                                    )
                                                                )
                                                            }
                                                        )
                                                    }
                                                    if (transaction.tagList.isNotEmpty()) {
                                                        val label =
                                                            transaction.tagList.joinToString { it.second }
                                                        add(
                                                            MenuEntry(
                                                                label = UiText.StringValue(label),
                                                                command = "FILTER_FOR_METHOD"
                                                            ) {
                                                                addFilterCriterion(
                                                                    TagCriterion(
                                                                        label,
                                                                        transaction.tagList.map { it.first }
                                                                    )
                                                                )
                                                            }
                                                        )
                                                    }
                                                    add(
                                                        MenuEntry(
                                                            label = UiText.StringValue(
                                                                currencyFormatter.formatMoney(
                                                                    transaction.displayAmount
                                                                )
                                                            ),
                                                            command = "FILTER_FOR_AMOUNT"
                                                        ) {
                                                            addFilterCriterion(
                                                                AmountCriterion(
                                                                    operation = Operation.EQ,
                                                                    values = listOf(transaction.displayAmount.amountMinor),
                                                                    currency = transaction.displayAmount.currencyUnit.code,
                                                                    sign = transaction.displayAmount.amountMinor > 0
                                                                )
                                                            )
                                                        }
                                                    )
                                                    if (!transaction.comment.isNullOrEmpty()) {
                                                        add(
                                                            MenuEntry(
                                                                label = UiText.StringValue(
                                                                    transaction.comment
                                                                ),
                                                                command = "FILTER_FOR_AMOUNT"
                                                            ) {
                                                                addFilterCriterion(
                                                                    CommentCriterion(transaction.comment)
                                                                )
                                                            }
                                                        )
                                                    }
                                                }
                                            )
                                        ))
                                }
                            )
                        }
                    },
                    futureCriterion = viewModel.futureCriterion.collectAsState(initial = FutureCriterion.EndOfDay).value,
                    expansionHandler = viewModel.expansionHandlerForTransactionGroups(account),
                    onBudgetClick = { budgetId, headerId ->
                        contribFeatureRequested(ContribFeature.BUDGET, budgetId to headerId)
                    },
                    showSumDetails = viewModel.showSumDetails.collectAsState(initial = true).value,
                    scrollToCurrentDate = viewModel.scrollToCurrentDate.getValue(account.id),
                    renderer = when (viewModel.renderer.collectAsState(initial = RenderType.New).value) {
                        RenderType.New -> {
                            NewTransactionRenderer(
                                dateTimeFormatter(account, prefHandler, this@BaseMyExpenses),
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
                                    this@BaseMyExpenses
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
                    },
                    isFiltered = filter.value != null,
                    splitInfoResolver = {
                        viewModel.splitInfo(it)
                    }
                )
            }
        }
    }

    private fun undelete(itemIds: List<Long>) {
        checkSealed(itemIds) {
            viewModel.undeleteTransactions(itemIds).observe(this) { result: Int ->
                finishActionMode()
                showSnackBar("${getString(R.string.menu_undelete_transaction)}: $result")
            }
        }
    }

    private fun unarchive(transaction: Transaction2) {
        showConfirmationDialog(
            tag = "UNARCHIVE",
            message = getString(R.string.warning_unarchive),
            commandPositive = R.id.UNARCHIVE_COMMAND,
            commandPositiveLabel = R.string.menu_unpack
        ) {
            putLong(KEY_ROWID, transaction.id)
        }
    }

    private fun ungroupSplit(transaction: Transaction2) {
        showConfirmationDialog(
            tag = "UNSPLIT_TRANSACTION",
            message = getString(R.string.warning_ungroup_split_transactions),
            commandPositive = R.id.UNGROUP_SPLIT_COMMAND,
            commandPositiveLabel = R.string.menu_ungroup_split_transaction
        ) {
            putLong(KEY_ROWID, transaction.id)
        }
    }

    private fun unlinkTransfer(transaction: Transaction2) {
        showConfirmationDialog(
            tag = "UNLINK_TRANSFER",
            message = getString(R.string.warning_unlink_transfer),
            commandPositive = R.id.UNLINK_TRANSFER_COMMAND,
            commandPositiveLabel = R.string.menu_unlink_transfer
        ) {
            putLong(KEY_ROWID, transaction.id)
        }
    }

    private fun transformToTransfer(transaction: Transaction2) {
        SelectTransformToTransferTargetDialogFragment.newInstance(transaction)
            .show(supportFragmentManager, "SELECT_ACCOUNT")
    }

    private fun createTemplate(transaction: Transaction2) {
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

    private fun edit(transaction: Transaction2, clone: Boolean = false) {
        checkSealed(listOf(transaction.id)) {
            if (transaction.transferPeerIsPart == true) {
                showSnackBar(
                    if (transaction.transferPeerIsArchived == true) R.string.warning_archived_transfer_cannot_be_edited else R.string.warning_splitpartcategory_context
                )
            } else {
                startActivityForResult(
                    Intent(this, ExpenseEdit::class.java).apply {
                        putExtra(KEY_ROWID, transaction.id)
                        putExtra(KEY_COLOR, transaction.color ?: currentAccount?._color)
                        if (clone) {
                            putExtra(ExpenseEdit.KEY_CLONE, true)
                        }

                    }, EDIT_REQUEST
                )
            }
        }
    }

    private fun split(itemIds: List<Long>) {
        checkSealed(itemIds) {
            contribFeatureRequested(
                ContribFeature.SPLIT_TRANSACTION,
                itemIds.toLongArray()
            )
        }

    }

    private suspend fun deleteArchive(transaction: Transaction2) {
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

    private fun delete(transactions: List<Pair<Long, CrStatus>>) {
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

    private fun closeDrawer() = binding.drawer?.closeDrawers() != null

    override fun injectDependencies() {
        injector.inject(this)
    }

    override fun onFeatureAvailable(feature: Feature) {
        super.onFeatureAvailable(feature)
        if (feature == Feature.OCR) {
            activateOcrMode()
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

    override suspend fun getEditIntent(): Intent? {
        val candidate = accountForNewTransaction
        return if (candidate != null || getHiddenAccountCount() > 0) {
            super.getEditIntent()!!.apply {
                candidate?.let {
                    putExtra(KEY_ACCOUNTID, it.id)
                    putExtra(KEY_CURRENCY, it.currency)
                    putExtra(KEY_COLOR, it._color)
                }
                val accountId = selectedAccountId
                if (isAggregate(accountId)) {
                    putExtra(ExpenseEdit.KEY_AUTOFILL_MAY_SET_ACCOUNT, true)
                }
            }
        } else {
            showSnackBar(R.string.no_accounts)
            null
        }
    }

    private fun createRow(type: Int, isIncome: Boolean = false) {
        if (type == TYPE_SPLIT) {
            contribFeatureRequested(ContribFeature.SPLIT_TRANSACTION)
        } else if (type == TYPE_TRANSFER && accountCount == 1) {
            showTransferAccountMissingMessage()
        } else {
            createRowDo(type, isIncome)
        }
    }

    private fun createRowDo(type: Int, isIncome: Boolean) {
        lifecycleScope.launch {
            createRowIntent(type, isIncome)?.let { startEdit(it) }
        }
    }

    override fun startEdit(intent: Intent) {
        floatingActionButton.hide()
        super.startEdit(intent)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean =
        if (super.onResult(dialogTag, which, extras)) true
        else if (which == BUTTON_POSITIVE) {
            when (dialogTag) {

                DIALOG_TAG_NEW_BALANCE -> {
                    lifecycleScope.launch {
                        createRowIntent(Transactions.TYPE_TRANSACTION, false)?.apply {
                            putExtra(
                                KEY_AMOUNT,
                                (BundleCompat.getSerializable(
                                    extras,
                                    KEY_AMOUNT,
                                    BigDecimal::class.java
                                ))!! -
                                        Money(
                                            currentAccount!!.currencyUnit,
                                            currentAccount!!.currentBalance
                                        ).amountMajor
                            )
                        }?.let {
                            startEdit(it)
                        }
                    }
                    true
                }

                else -> false
            }
        } else false

    private fun shareExport(format: ExportFormat, uriList: List<Uri>) {
        baseViewModel.share(
            this, uriList,
            shareTarget,
            "text/" + format.name.lowercase(Locale.US)
        )
    }

    private val createAccount =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                selectedAccountId = it.data!!.getLongExtra(KEY_ROWID, 0)
            }
        }

    private val createAccountForTransfer =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                createRow(TYPE_TRANSFER)
            }
        }

    private fun createAccountDo() {
        closeDrawer()
        createAccount.launch(createAccountIntent)
    }

    private fun configureEquivalentWorthMenuItemIcon(menuItem: MenuItem, checked: Boolean) {
        menuItem.isChecked = checked
        menuItem.icon = if (checked) {
            ContextCompat.getDrawable(this, R.drawable.checkbox_checked)
        } else {
            ContextCompat.getDrawable(this, R.drawable.checkbox_not_checked)
        }
    }

    fun handleNavigationClick(item: MenuItem) = when (item.itemId) {
        R.id.EQUIVALENT_WORTH_COMMAND -> {
            viewModel.persistShowEquivalentWorth(!item.isChecked)
            true
        }

        else -> dispatchCommand(item.itemId, null)
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        } else when (command) {
            R.id.MANAGE_ACCOUNT_TYPES_COMMAND -> {
                startActivity(Intent(this, ManageAccountTypes::class.java))
            }

            R.id.ACCOUNT_FLAGS_COMMAND -> {
                startActivity(Intent(this, ManageAccountFlags::class.java))
            }

            R.id.CREATE_ACCOUNT_COMMAND -> {
                if (licenceHandler.hasAccessTo(ContribFeature.ACCOUNTS_UNLIMITED)) {
                    createAccountDo()
                } else {
                    lifecycleScope.launch {
                        if (accountCount + getHiddenAccountCount() < ContribFeature.FREE_ACCOUNTS) {
                            createAccountDo()
                        } else {
                            showContribDialog(ContribFeature.ACCOUNTS_UNLIMITED, null)
                        }
                    }
                }
            }

            R.id.CREATE_ACCOUNT_FOR_TRANSFER_COMMAND -> {
                createAccountForTransfer.launch(createAccountIntent)
            }

            R.id.SAFE_MODE_COMMAND -> {
                prefHandler.putBoolean(PrefKey.DB_SAFE_MODE, true)
                viewModel.triggerAccountListRefresh()
                contentResolver.notifyChange(TRANSACTIONS_URI, null, false)
            }

            R.id.HISTORY_COMMAND -> contribFeatureRequested(ContribFeature.HISTORY)

            R.id.CLEAR_FILTER_COMMAND -> {
                lifecycleScope.launch {
                    currentFilter.persist(null)
                    invalidateOptionsMenu()
                }
            }


            R.id.DISTRIBUTION_COMMAND -> contribFeatureRequested(ContribFeature.DISTRIBUTION)

            R.id.RESET_COMMAND -> checkReset()

            R.id.OCR_DOWNLOAD_COMMAND -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = "market://details?id=org.totschnig.ocr.tesseract".toUri()
                }
                packageManager.queryIntentActivities(intent, 0)
                    .map { it.activityInfo }
                    .find {
                        it.packageName == "org.fdroid.fdroid" || it.packageName == "org.fdroid.basic"
                    }?.let {
                        intent.component = ComponentName(it.applicationInfo.packageName, it.name)
                        startActivity(intent)
                    }
                    ?: run {
                        Toast.makeText(this, "F-Droid not installed", Toast.LENGTH_LONG).show()
                    }
            }

            R.id.DELETE_ACCOUNT_COMMAND_DO -> {
                val accountIds = tag as LongArray
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

            R.id.BALANCE_COMMAND -> {
                with(currentAccount!!) {
                    if (hasCleared) {
                        BalanceDialogFragment.newInstance(Bundle().apply {

                            putLong(KEY_ROWID, id)
                            putString(KEY_LABEL, label)
                            putString(
                                KEY_RECONCILED_TOTAL,
                                currencyFormatter.formatMoney(
                                    Money(currencyUnit, reconciledTotal)
                                )
                            )
                            putString(
                                KEY_CLEARED_TOTAL,
                                currencyFormatter.formatMoney(
                                    Money(currencyUnit, clearedTotal)
                                )
                            )
                        }).show(supportFragmentManager, "BALANCE_ACCOUNT")
                    } else {
                        showSnackBar(R.string.dialog_command_disabled_balance)
                    }
                }
            }

            R.id.SYNC_COMMAND -> currentAccount?.takeIf { it.syncAccountName != null }?.let {
                requestSync(
                    accountName = it.syncAccountName!!,
                    uuid = if (prefHandler.getBoolean(
                            PrefKey.SYNC_NOW_ALL,
                            false
                        )
                    ) null else it.uuid
                )
            }

            R.id.FINTS_SYNC_COMMAND -> currentAccount?.takeIf { it.bankId != null }?.let {
                contribFeatureRequested(
                    ContribFeature.BANKING,
                    Triple(it.bankId, it.id, it.type.id)
                )
            }

            R.id.EDIT_ACCOUNT_COMMAND -> currentAccount?.let { editAccount(it) }

            R.id.DELETE_ACCOUNT_COMMAND -> currentAccount?.let { confirmAccountDelete(it) }

            R.id.TOGGLE_SEALED_COMMAND -> currentAccount?.let { toggleAccountSealed(it) }

            R.id.EXCLUDE_FROM_TOTALS_COMMAND -> currentAccount?.let { toggleExcludeFromTotals(it) }

            R.id.DYNAMIC_EXCHANGE_RATE_COMMAND -> currentAccount?.let { toggleDynamicExchangeRate(it) }

            R.id.BUDGET_COMMAND -> contribFeatureRequested(ContribFeature.BUDGET, null)

            R.id.HELP_COMMAND_DRAWER -> startActivity(Intent(this, Help::class.java).apply {
                putExtra(HelpDialogFragment.KEY_CONTEXT, "NavigationDrawer")
            })

            R.id.MANAGE_TEMPLATES_COMMAND -> startActivity(
                Intent(
                    this,
                    ManageTemplates::class.java
                )
            )

            R.id.SHARE_COMMAND -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                putExtra(
                    Intent.EXTRA_TEXT,
                    Utils.getTellAFriendMessage(this@BaseMyExpenses).toString()
                )
                setType("text/plain")
            }, getResources().getText(R.string.menu_share)))

            R.id.CANCEL_CALLBACK_COMMAND -> finishActionMode()

            R.id.ROADMAP_COMMAND -> startActivity(Intent(this, RoadmapVoteActivity::class.java))

            R.id.BACKUP_COMMAND -> startActivity(
                Intent(
                    this,
                    BackupRestoreActivity::class.java
                ).apply {
                    setAction(BackupRestoreActivity.ACTION_BACKUP)
                })


            R.id.RESTORE_COMMAND -> startActivity(
                Intent(
                    this,
                    BackupRestoreActivity::class.java
                ).apply {
                    setAction(BackupRestoreActivity.ACTION_RESTORE)
                })

            R.id.MANAGE_PARTIES_COMMAND -> startActivity(
                Intent(
                    this,
                    ManageParties::class.java
                ).apply {
                    setAction(Action.MANAGE.name)
                })

            R.id.BALANCE_SHEET_COMMAND -> {
                openBalanceSheet()
            }

            else -> return false
        }
        return true
    }

    fun openBalanceSheet() {
        viewModel.showBalanceSheet = true
        onBackPressedCallback.isEnabled = true
        val screenWidth = resources.displayMetrics.widthPixels
        val preferredBalanceSheetWidth = resources.getDimensionPixelSize(R.dimen.drawerWidth) * 2
        val veryLarge = screenWidth >= preferredBalanceSheetWidth * 2
        binding.accountPanel.root.layoutParams.width = when {
            binding.drawer != null -> screenWidth
            veryLarge -> preferredBalanceSheetWidth
            else -> ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.accountPanel.root.displayedChild = 1
        binding.accountPanel.balanceSheet.setContent {
            AppTheme {
                val data =
                    viewModel.accountsForBalanceSheet.collectAsState(LocalDate.now() to emptyList()).value
                BalanceSheetView(
                    data.second,
                    viewModel.debtSum.collectAsState(0).value,
                    data.first,
                    onClose = { closeBalanceSheet() },
                    onNavigate = {
                        if (it.isVisible) {
                            selectedAccountId = it.id
                            if (!closeDrawer() && !veryLarge) {
                                closeBalanceSheet()
                            }
                        } else {
                            TransactionListComposeDialogFragment.newInstance(
                                TransactionListViewModel.LoadingInfo(
                                    accountId = it.id,
                                    currency = it.currency,
                                    label = it.label,
                                    color = it.color,
                                    withTransfers = true,
                                    withNewButton = true
                                )
                            ).show(supportFragmentManager, "HIDDEN_ACCOUNT_LIST")
                        }
                    },
                    onSetDate = {
                        viewModel.setBalanceDate(it)
                    },
                    onPrint = {
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
                )
            }
        }
    }

    fun closeBalanceSheet() = if (binding.accountPanel.root.displayedChild == 1) {
        viewModel.showBalanceSheet = false
        binding.accountPanel.root.layoutParams.width =
            resources.getDimensionPixelSize(R.dimen.drawerWidth)
                .coerceAtMost(resources.displayMetrics.widthPixels)
        binding.accountPanel.root.displayedChild = 0
        if (binding.drawer == null) {
            onBackPressedCallback.isEnabled = false
        }
        true
    } else false

    fun setupFabSubMenu() {
        floatingActionButton.setOnLongClickListener { fab ->
            discoveryHelper.markDiscovered(DiscoveryHelper.Feature.FabLongPress)
            val popup = PopupMenu(this, fab)
            val popupMenu = popup.menu
            popup.setOnMenuItemClickListener { item ->
                trackCommand(item.itemId)
                createRow(
                    when (item.itemId) {
                        R.string.split_transaction -> TYPE_SPLIT
                        R.string.transfer -> TYPE_TRANSFER
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
            @Suppress("UsePropertyAccessSyntax", "RestrictedApi")
            (popup.menu as? MenuBuilder)?.setOptionalIconsVisible(true)
            popup.show()
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        prefHandler.mainMenu.forEach { menuItem ->
            if (menuItem.subMenu != null) {
                val subMenu =
                    menu.addSubMenu(Menu.NONE, menuItem.id, Menu.NONE, menuItem.getLabel(this))
                menuInflater.inflate(menuItem.subMenu, subMenu)
                subMenu.item
            } else {
                menu.add(Menu.NONE, menuItem.id, Menu.NONE, menuItem.getLabel(this))
            }
                .apply {
                    menuItem.icon?.let { setIcon(it) }
                    isCheckable = menuItem.isCheckable
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.WEB_UI_COMMAND)?.let {
            it.isChecked = isWebUiActive()
            checkMenuIcon(it, R.drawable.ic_computer)
        }
        if (accountData.isNotEmpty() && currentAccount != null) {
            menu.findItem(R.id.SCAN_MODE_COMMAND)?.let {
                it.isChecked = isScanMode()
                checkMenuIcon(it, R.drawable.ic_scan)
            }
            with(currentAccount!!) {
                val reconciliationAvailable = type.supportsReconciliation && !sealed
                val groupingMenu = menu.findItem(R.id.GROUPING_COMMAND)
                val groupingEnabled = sortBy == KEY_DATE
                groupingMenu?.setEnabledAndVisible(groupingEnabled)

                if (groupingEnabled) {
                    groupingMenu?.subMenu?.let {
                        Utils.configureGroupingMenu(it, grouping)
                    }
                }

                menu.findItem(R.id.SORT_MENU)?.subMenu?.let {
                    configureSortDirectionMenu(this@BaseMyExpenses, it, sortBy, sortDirection)
                }

                menu.findItem(R.id.BALANCE_COMMAND)
                    ?.setEnabledAndVisible(reconciliationAvailable && !isAggregate)

                menu.findItem(R.id.SHOW_STATUS_HANDLE_COMMAND)?.apply {
                    setEnabledAndVisible(reconciliationAvailable)
                    if (reconciliationAvailable) {
                        lifecycleScope.launch {
                            isChecked = viewModel.showStatusHandle().first()
                            checkMenuIcon(this@apply, R.drawable.ic_square)
                        }
                    }
                }

                menu.findItem(R.id.SYNC_COMMAND)?.setEnabledAndVisible(syncAccountName != null)
                menu.findItem(R.id.FINTS_SYNC_COMMAND)?.apply {
                    setEnabledAndVisible(bankId != null)
                    if (bankId != null) {
                        title = bankingFeature.syncMenuTitle(this@BaseMyExpenses)
                    }
                }
                menu.findItem(R.id.ARCHIVE_COMMAND)
                    ?.setEnabledAndVisible(!isAggregate && !sealed && hasItems)
            }
            menu.findItem(R.id.SEARCH_COMMAND)?.let {
                it.setEnabledAndVisible(hasItems)
                it.isChecked = currentFilter.whereFilter.value != null
                checkMenuIcon(it, R.drawable.ic_menu_search)
            }
            menu.findItem(R.id.DISTRIBUTION_COMMAND)
                ?.setEnabledAndVisible(sumInfo.value.mappedCategories)
            menu.findItem(R.id.HISTORY_COMMAND)?.setEnabledAndVisible(hasItems)
            menu.findItem(R.id.RESET_COMMAND)?.setEnabledAndVisible(hasItems)
            menu.findItem(R.id.PRINT_COMMAND)?.setEnabledAndVisible(hasItems)
        } else {
            for (item in listOf(
                R.id.SEARCH_COMMAND,
                R.id.DISTRIBUTION_COMMAND,
                R.id.HISTORY_COMMAND,
                R.id.SCAN_MODE_COMMAND,
                R.id.RESET_COMMAND,
                R.id.SYNC_COMMAND,
                R.id.BALANCE_COMMAND,
                R.id.SORT_MENU,
                R.id.PRINT_COMMAND,
                R.id.GROUPING_COMMAND,
                R.id.SHOW_STATUS_HANDLE_COMMAND,
                R.id.FINTS_SYNC_COMMAND
            )) {
                menu.findItem(item)?.setEnabledAndVisible(false)
            }
        }

        return super.onPrepareOptionsMenu(menu)
    }

    private val hasItems
        get() = sumInfo.value.hasItems

    private fun setupToolbarClickHandlers() {
        listOf(binding.toolbar.subtitle, binding.toolbar.title).forEach {
            it.setOnClickListener {
                if (accountCount > 0) {
                    val popup = PopupMenu(this, toolbar)
                    val popupMenu = popup.menu
                    popupMenu.add(
                        Menu.NONE,
                        R.id.COPY_TO_CLIPBOARD_COMMAND,
                        Menu.NONE,
                        R.string.copy_text
                    )
                    if (currentAccount?.isAggregate == false) {
                        popupMenu.add(
                            Menu.NONE,
                            R.id.NEW_BALANCE_COMMAND,
                            Menu.NONE,
                            getString(R.string.new_balance)
                        )
                    }
                    popup.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.COPY_TO_CLIPBOARD_COMMAND -> copyToClipBoard()
                            R.id.NEW_BALANCE_COMMAND -> if (selectedAccountId > 0) {
                                AmountInputHostDialog.build().title(R.string.new_balance)
                                    .fields(
                                        AmountInput.plain(KEY_AMOUNT)
                                            .label(R.string.new_balance)
                                            .fractionDigits(currentAccount!!.currencyUnit.fractionDigits)
                                            .withTypeSwitch(currentAccount!!.currentBalance > 0)
                                    ).show(this, DIALOG_TAG_NEW_BALANCE)
                            }
                        }
                        true
                    }
                    popup.show()
                }
            }
        }
        listOf(binding.toolbar.donutView, binding.toolbar.progressPercent).forEach {
            it.setOnClickListener {
                currentAccount?.run {
                    criterion?.also {
                        CriterionReachedDialogFragment.newInstance(
                            CriterionInfo(
                                id,
                                currentBalance,
                                criterion,
                                0,
                                _color,
                                currencyUnit,
                                label,
                                sealed
                            ), withAnimation = false
                        )
                            .show(supportFragmentManager, "CRITERION")
                    } ?: run {
                        CrashHandler.report(Exception("Progress is visible, but no criterion is defined"))
                    }
                }
            }
        }
    }

    override fun onFabClicked() {
        super.onFabClicked()
        when {
            currentAccount?.sealed == true -> showSnackBar(
                message = getString(R.string.account_closed),
                snackBarAction = SnackbarAction(getString(R.string.menu_reopen)) {
                    dispatchCommand(R.id.TOGGLE_SEALED_COMMAND, null)
                }
            )

            isScanMode() -> contribFeatureRequested(ContribFeature.OCR, true)
            else -> createRowDo(Transactions.TYPE_TRANSACTION, false)
        }
    }

    /**
     * adapt UI to currently selected account
     * @return currently selected account
     */
    private fun configureUiWithCurrentAccount(account: FullAccount?, animateProgress: Boolean) {
        if (account != null) {
            prefHandler.putLong(PrefKey.CURRENT_ACCOUNT, account.id)
            tintFab(account.color(resources))
            setBalance(account, animateProgress)
        }
        updateFab()
        invalidateOptionsMenu()
    }

    private fun setBalance(account: FullAccount, animateProgress: Boolean) {

        val isHome = account.isHomeAggregate
        currentBalance = (if (isHome) " ≈ " else "") +
                currencyFormatter.formatMoney(
                    Money(
                        account.currencyUnit,
                        if (isHome) account.equivalentCurrentBalance else account.currentBalance
                    )
                )
        binding.toolbar.title.text =
            if (isHome) getString(R.string.grand_total) else account.label
        with(binding.toolbar.subtitle) {
            text = currentBalance
            setTextColor(getAmountColor(account.currentBalance.sign))
        }

        val progress = account.progress

        val accountVisual = when {
            account.isAggregate -> ACCOUNT_VISUAL_NONE
            progress != null -> ACCOUNT_VISUAL_PROGRESS
            account.bankId != null -> ACCOUNT_VISUAL_ICON
            else -> ACCOUNT_VISUAL_COLOR
        }

        binding.toolbar.donutView.isVisible = accountVisual == ACCOUNT_VISUAL_PROGRESS
        binding.toolbar.progressPercent.isVisible = accountVisual == ACCOUNT_VISUAL_PROGRESS
        binding.toolbar.accountColorIndicator.isVisible = accountVisual == ACCOUNT_VISUAL_COLOR
        binding.toolbar.bankIcon.isVisible = accountVisual == ACCOUNT_VISUAL_ICON
        when (accountVisual) {
            ACCOUNT_VISUAL_ICON -> {
                viewModel.banks.value.find { it.id == account.bankId }?.let {
                    bankingFeature.bankIcon(it)
                }?.let {
                    binding.toolbar.bankIcon.setImageResource(it)
                }
            }

            ACCOUNT_VISUAL_COLOR -> {
                (binding.toolbar.accountColorIndicator.background as GradientDrawable).setColor(
                    account._color
                )
            }

            ACCOUNT_VISUAL_PROGRESS -> {
                val (sign, progress) = progress!!
                with(binding.toolbar.donutView) {
                    animateChanges = animateProgress
                    submitData(
                        sections = DisplayProgress.calcProgressVisualRepresentation(progress)
                            .forViewSystem(
                                account._color,
                                getAmountColor(sign)
                            ).also {
                                Timber.d("Sections: %s", progress)
                            }
                    )
                    contentDescription =
                        getString(if (sign > 0) R.string.saving_goal else R.string.credit_limit) + ": " +
                                DisplayProgress.contentDescription(this@BaseMyExpenses, progress)
                }

                with(binding.toolbar.progressPercent) {
                    text = progress.displayProgress
                    setTextColor(this@BaseMyExpenses.getAmountColor(sign))
                }
            }
        }
        progress?.let { (sign, progress) ->
            with(binding.toolbar.donutView) {
                animateChanges = animateProgress
                submitData(
                    sections = DisplayProgress.calcProgressVisualRepresentation(progress)
                        .forViewSystem(
                            account._color,
                            getAmountColor(sign)
                        ).also {
                            Timber.d("Sections: %s", progress)
                        }
                )
                contentDescription =
                    getString(if (sign > 0) R.string.saving_goal else R.string.credit_limit) + ": " +
                            DisplayProgress.contentDescription(this@BaseMyExpenses, progress)
            }

            with(binding.toolbar.progressPercent) {
                text = progress.displayProgress
                setTextColor(this@BaseMyExpenses.getAmountColor(sign))
            }
        }
    }

    private fun copyToClipBoard() {
        currentBalance.takeIf { it.isNotEmpty() }?.let { copyToClipboard(it) }
    }

    fun updateFab() {
        if (accountCount > 0) {
            updateFabDo()
        } else {
            lifecycleScope.launch {
                if (getHiddenAccountCount() > 0) {
                    updateFabDo()
                } else {
                    floatingActionButton.hide()
                }
            }
        }
    }

    fun updateFabDo() {
        val scanMode = isScanMode()
        val sealed = currentAccount?.sealed == true
        with(floatingActionButton) {
            show()
            alpha = if (sealed) 0.5f else 1f
            setImageResource(
                when {
                    sealed -> R.drawable.ic_lock
                    scanMode -> R.drawable.ic_scan
                    else -> R.drawable.ic_menu_add_fab
                }
            )
            contentDescription = when {
                sealed -> getString(R.string.content_description_closed)
                scanMode -> getString(R.string.contrib_feature_ocr_label)
                else -> TextUtils.concatResStrings(
                    this@BaseMyExpenses,
                    ". ",
                    R.string.menu_create_transaction,
                    R.string.menu_create_transfer,
                    R.string.menu_create_split
                )
            }
        }
    }

    fun isScanMode() = prefHandler.getBoolean(PrefKey.OCR, false)
    private fun isWebUiActive() = prefHandler.getBoolean(PrefKey.UI_WEB, false)

    private fun activateOcrMode() {
        prefHandler.putBoolean(PrefKey.OCR, true)
        updateFab()
        invalidateOptionsMenu()
    }

    private fun Intent.fillIntentForGroupingFromTag(tag: Int) {
        val year = (tag / 1000)
        val groupingSecond = (tag % 1000)
        putExtra(KEY_YEAR, year)
        putExtra(KEY_SECOND_GROUP, groupingSecond)
    }

    private fun Intent.forwardCurrentConfiguration(currentAccount: FullAccount) {
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
                        viewModel.print(currentAccount, currentFilter.whereFilter.value)
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

                ContribFeature.OCR -> {
                    if (featureViewModel.isFeatureAvailable(this, Feature.OCR)) {
                        if ((tag as Boolean)) {
                            //ocrViewModel.startOcrFeature(Uri.parse("file:///android_asset/OCR.jpg"), supportFragmentManager);
                            startMediaChooserDo("SCAN")
                        } else {
                            activateOcrMode()
                        }
                    } else {
                        featureViewModel.requestFeature(this, Feature.OCR)
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

                else -> super.contribFeatureCalled(feature, tag)
            }
        } ?: run {
            showSnackBar(R.string.no_accounts)
        }
    }

    private fun confirmAccountDelete(account: FullAccount) {
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

    private fun toggleAccountSealed(account: FullAccount) {
        if (account.sealed) {
            viewModel.setSealed(account.id, false)
        } else {
            if (account.syncAccountName == null) {
                viewModel.setSealed(account.id, true)
            } else {
                showSnackBar(
                    getString(R.string.warning_synced_account_cannot_be_closed),
                    Snackbar.LENGTH_LONG, null, null, binding.accountPanel.accountList
                )
            }
        }
    }

    private fun toggleExcludeFromTotals(account: FullAccount) {
        viewModel.setExcludeFromTotals(account.id, !account.excludeFromTotals)
    }

    private fun toggleDynamicExchangeRate(account: FullAccount) {
        viewModel.setDynamicExchangeRate(account.id, !account.dynamic)
    }

    val navigationView: NavigationView
        get() {
            return binding.accountPanel.expansionContent
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

    private fun checkReset() {
        exportViewModel.checkAppDir().observe(this) { result ->
            result.onSuccess {
                currentAccount?.let { account ->
                    if (account.isAggregate) {
                        //for aggregate account sealed is checked for each account during export
                        showExportDialog(emptyList())
                    } else if (account.sealed) {
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

    fun balance(accountId: Long, reset: Boolean) {
        viewModel.balanceAccount(accountId, reset).observe(
            this
        ) { result ->
            result.onFailure {
                showSnackBar(it.safeMessage)
            }
        }
    }

    private fun Bundle.addFilter() {
        putParcelable(
            KEY_FILTER,
            currentFilter.whereFilter.value
        )
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

    fun addFilterCriterion(c: SimpleCriterion<*>) {
        lifecycleScope.launch {
            currentFilter.addCriterion(c)
            invalidateOptionsMenu()
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
                                CrashHandler.report(it)
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
                        CrashHandler.report(it)
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

    override fun onAction(action: CompletedAction, index: Int?) {
        when (action) {
            is ShareAction -> baseViewModel.share(
                this,
                action.targets,
                "",
                action.mimeType
            )

            is OpenAction -> startActionView(action.targets[index ?: 0], action.mimeType)

            else -> {}
        }
    }

    private fun LiveData<Result<Unit>>.observeAndReportFailure() {
        observe(this@BaseMyExpenses) { result ->
            result.onFailure {
                CrashHandler.report(it)
                showSnackBar(it.safeMessage)
            }
        }
    }

    override fun onNegative(args: Bundle) {
        val command = args.getInt(KEY_COMMAND_NEGATIVE)
        if (command != 0) {
            dispatchCommand(command, null)
        }
    }

    protected fun voteReminderCheck() {
        val prefKey = "vote_reminder_shown_${RoadmapRepository.VERSION}"
        if (!prefHandler.getBoolean(prefKey, false) && Utils.getDaysSinceUpdate(this) > 1) {
            roadmapViewModel.getLastVote().observe(this) { vote: Vote? ->
                val hasNotVoted = vote == null
                if (hasNotVoted) {
                    ConfirmationDialogFragment.newInstance(Bundle().apply {
                        putCharSequence(KEY_MESSAGE, getString(R.string.roadmap_intro))
                        putInt(KEY_COMMAND_POSITIVE, R.id.ROADMAP_COMMAND)
                        putString(ConfirmationDialogFragment.KEY_PREFKEY, prefKey)
                        putInt(KEY_POSITIVE_BUTTON_LABEL, R.string.roadmap_vote)
                    }).show(supportFragmentManager, "ROAD_MAP_VOTE_REMINDER")
                }
            }
        }
    }

    fun showTransactionFromIntent(extras: Bundle) {
        val idFromNotification = extras.getLong(KEY_TRANSACTIONID, 0)
        if (idFromNotification != 0L) {
            showDetails(idFromNotification, false)
            intent.removeExtra(KEY_TRANSACTIONID)
        }
    }

    fun confirmClearFilter() {
        ConfirmationDialogFragment.newInstance(Bundle().apply {
            putString(KEY_MESSAGE, getString(R.string.clear_all_filters))
            putInt(KEY_COMMAND_POSITIVE, R.id.CLEAR_FILTER_COMMAND)
        }).show(supportFragmentManager, "CLEAR_FILTER")
    }

    override val scrollsHorizontally: Boolean = true

    override fun contribFeatureNotCalled(feature: ContribFeature) {
        if (!isGithub && feature == ContribFeature.AD_FREE) {
            finish()
        }
    }

    companion object {
        const val MANAGE_HIDDEN_FRAGMENT_TAG = "MANAGE_HIDDEN"
        const val ACCOUNT_VISUAL_NONE = 0
        const val ACCOUNT_VISUAL_PROGRESS = 1
        const val ACCOUNT_VISUAL_ICON = 2
        const val ACCOUNT_VISUAL_COLOR = 3
    }
}