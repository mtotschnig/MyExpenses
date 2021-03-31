package org.totschnig.myexpenses.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.ViewModelProvider
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.AmountEdit
import eltos.simpledialogfragment.form.Hint
import eltos.simpledialogfragment.form.SimpleFormDialog
import eltos.simpledialogfragment.form.Spinner
import icepick.State
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit.Companion.KEY_OCR_RESULT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.OcrHost
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.feature.OcrResultFlat
import org.totschnig.myexpenses.feature.Payee
import org.totschnig.myexpenses.model.AggregateAccount
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.ui.DiscoveryHelper
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.distrib.ReviewManager
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel
import timber.log.Timber
import java.io.File
import java.math.BigDecimal
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
    lateinit var toolbar: Toolbar

    var columnIndexLabel = 0
    var columnIndexRowId = 0
    var columnIndexColor = 0
    var columnIndexCurrency = 0
    var columnIndexGrouping = 0
    var columnIndexType = 0

    private var currentBalance: String? = null
    var currentPosition = -1

    lateinit var viewModel: MyExpensesViewModel

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (savedInstanceState == null) {
            floatingActionButton?.let { discoveryHelper.discover(this, it, 3, DiscoveryHelper.Feature.fab_long_press) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MyExpensesViewModel::class.java]
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
            (pair.second?.let { pair.first.atTime(pair.second) } ?: pair.second).toString()

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
                                    1 -> Hint.plain("%s: %s".format(getString(R.string.amount), it.amountCandidates[0]))
                                    else -> Spinner.plain(KEY_AMOUNT)
                                            .placeholder(R.string.amount)
                                            .items(*it.amountCandidates.toTypedArray())
                                            .preset(0)
                                },
                                when (it.dateCandidates.size) {
                                    0 -> Hint.plain(getString(R.string.scan_result_no_date))
                                    1 -> Hint.plain("%s: %s".format(getString(R.string.date), displayDateCandidate(it.dateCandidates[0])))
                                    else -> Spinner.plain(KEY_DATE)
                                            .placeholder(R.string.date)
                                            .items(*it.dateCandidates.map(this::displayDateCandidate).toTypedArray())
                                            .preset(0)
                                },
                                when (it.payeeCandidates.size) {
                                    0 -> Hint.plain(getString(R.string.scan_result_no_payee))
                                    1 -> Hint.plain("%s: %s".format(getString(R.string.payee), it.payeeCandidates[0].name))
                                    else -> Spinner.plain(KEY_PAYEE_NAME)
                                            .placeholder(R.string.payee)
                                            .items(*it.payeeCandidates.map(Payee::name).toTypedArray())
                                            .preset(0)
                                }
                        )
                        .show(this, DIALOG_TAG_OCR_DISAMBIGUATE)
            } else {
                startEditFromOcrResult(if (it.isEmpty()) {
                    Toast.makeText(this, getString(R.string.scan_result_no_data), Toast.LENGTH_LONG).show()
                    null
                } else {
                    it.selectCandidates()
                })
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
    open fun createRowIntent(type: Int, isIncome: Boolean) = Intent(this, ExpenseEdit::class.java).apply {
        putExtra(Transactions.OPERATION_TYPE, type)
        putExtra(ExpenseEdit.KEY_INCOME, isIncome)
        //if we are called from an aggregate cursor, we also hand over the currency
        if (accountId < 0) {
            putExtra(DatabaseConstants.KEY_CURRENCY, currentCurrency)
            putExtra(ExpenseEdit.KEY_AUTOFILL_MAY_SET_ACCOUNT, true)
        } else {
            //if accountId is 0 ExpenseEdit will retrieve the first entry from the accounts table
            putExtra(DatabaseConstants.KEY_ACCOUNTID, accountId)
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
                    putExtra(DatabaseConstants.KEY_PICTURE_URI, Uri.fromFile(scanFile))
                }
        )
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == OnDialogResultListener.BUTTON_POSITIVE) {
            when (dialogTag) {
                DIALOG_TAG_OCR_DISAMBIGUATE -> {
                    startEditFromOcrResult(extras.getParcelable<OcrResult>(KEY_OCR_RESULT)!!.selectCandidates(
                            extras.getInt(KEY_AMOUNT), extras.getInt(KEY_DATE), extras.getInt(KEY_PAYEE_NAME)))
                    return true
                }
                DIALOG_TAG_NEW_BALANCE -> {
                    if (currentPosition > -1) {
                        accountsCursor?.let { cursor ->
                            currentCurrencyUnit?.let { currencyUnit ->
                                cursor.moveToPosition(currentPosition)
                                startEdit(
                                        createRowIntent(Transactions.TYPE_TRANSACTION, false).apply {
                                            putExtra(KEY_AMOUNT, (extras.getSerializable(KEY_AMOUNT) as BigDecimal) -
                                                    Money(currencyUnit, cursor.getLong(cursor.getColumnIndex(DatabaseConstants.KEY_CURRENT_BALANCE))).amountMajor)
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

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        }
        if (command == R.id.OCR_DOWNLOAD_COMMAND) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=org.totschnig.myexpenses.ocr.tesseract")
            }
            packageManager.queryIntentActivities(intent, 0).find { it.activityInfo.packageName == "org.fdroid.fdroid" }
                    ?.activityInfo?.let {
                        intent.component = ComponentName(it.applicationInfo.packageName, it.name)
                        startActivity(intent)
                    }
                    ?: run { Toast.makeText(this, "F-Droid not installed", Toast.LENGTH_LONG).show() }
            return true
        }
        return false
    }

    fun setupFabSubMenu() {
        floatingActionButton?.setOnLongClickListener { fab ->
            discoveryHelper.markDiscovered(DiscoveryHelper.Feature.fab_long_press)
            val popup = PopupMenu(this, fab)
            val popupMenu = popup.menu
            popup.setOnMenuItemClickListener { item ->
                createRow(when (item.itemId) {
                    R.string.split_transaction -> Transactions.TYPE_SPLIT
                    R.string.transfer -> Transactions.TYPE_TRANSFER
                    else -> Transactions.TYPE_TRANSACTION
                }, item.itemId == R.string.income)
                true
            }
            popupMenu.add(Menu.NONE, R.string.expense, Menu.NONE, R.string.expense).setIcon(R.drawable.ic_expense)
            popupMenu.add(Menu.NONE, R.string.income, Menu.NONE, R.string.income).icon = AppCompatResources.getDrawable(this, R.drawable.ic_menu_add)?.also {
                DrawableCompat.setTint(it, resources.getColor(R.color.colorIncome))
            }
            popupMenu.add(Menu.NONE, R.string.transfer, Menu.NONE, R.string.transfer).setIcon(R.drawable.ic_menu_forward)
            popupMenu.add(Menu.NONE, R.string.split_transaction, Menu.NONE, R.string.split_transaction).setIcon(R.drawable.ic_menu_split)
            //noinspection RestrictedApi
            (popup.menu as? MenuBuilder)?.setOptionalIconsVisible(true)
            popup.show()
            true
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.SCAN_MODE_COMMAND)?.isChecked = prefHandler.getBoolean(PrefKey.OCR, false)
        return super.onPrepareOptionsMenu(menu)
    }

    fun setupToolbarPopupMenu() {
        toolbar.setOnClickListener {
            if (currentPosition > -1) {
                val popup = PopupMenu(this, toolbar)
                val popupMenu = popup.menu
                popupMenu.add(Menu.NONE, R.id.COPY_TO_CLIPBOARD_COMMAND, Menu.NONE, R.string.copy_text)
                popupMenu.add(Menu.NONE, R.id.NEW_BALANCE_COMMAND, Menu.NONE, getString(R.string.new_balance))
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.COPY_TO_CLIPBOARD_COMMAND -> copyToClipBoard()
                        R.id.NEW_BALANCE_COMMAND -> if (accountId > 0) {
                            SimpleFormDialog.build().fields(
                                    AmountEdit.plain(KEY_AMOUNT).label(R.string.new_balance).fractionDigits(currentCurrencyUnit!!.fractionDigits)
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
                val balance = cursor.getLong(cursor.getColumnIndex(DatabaseConstants.KEY_CURRENT_BALANCE))
                val label = cursor.getString(columnIndexLabel)
                val isHome = cursor.getInt(cursor.getColumnIndex(DatabaseConstants.KEY_IS_AGGREGATE)) == AggregateAccount.AGGREGATE_HOME
                currentBalance = String.format(Locale.getDefault(), "%s%s", if (isHome) " ≈ " else "",
                        currencyFormatter.formatCurrency(Money(currencyUnit, balance)))
                title = if (isHome) getString(R.string.grand_total) else label
                toolbar.subtitle = currentBalance
                toolbar.setSubtitleTextColor(resources.getColor(if (balance < 0) R.color.colorExpense else R.color.colorIncome))
            }
        }

    }

    private fun copyToClipBoard() {
        try {
            ContextCompat.getSystemService(this, ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, currentBalance))
            showSnackbar(R.string.toast_text_copied)
        } catch (e: RuntimeException) {
            Timber.e(e)
        }
    }

    fun updateFab() {
        val scanMode = isScanMode()
        requireFloatingActionButtonWithContentDescription(if (scanMode)
            getString(R.string.contrib_feature_ocr_label)
        else
            TextUtils.concatResStrings(this, ". ",
                    R.string.menu_create_transaction, R.string.menu_create_transfer, R.string.menu_create_split))
        floatingActionButton!!.setImageResource(if (scanMode) R.drawable.ic_scan else R.drawable.ic_menu_add_fab)
    }

    fun isScanMode(): Boolean {
        return prefHandler.getBoolean(PrefKey.OCR, false)
    }

    fun activateOcrMode() {
        prefHandler.putBoolean(PrefKey.OCR, true)
        updateFab()
        invalidateOptionsMenu()
    }
}