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
package org.totschnig.myexpenses.dialog

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.ImageViewIntentProvider
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.adapter.SplitPartRVAdapter
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.UiUtils.DateMode
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.TransactionDetailViewModel
import org.totschnig.myexpenses.viewmodel.data.Transaction
import javax.inject.Inject

class TransactionDetailFragment : CommitSafeDialogFragment(), DialogInterface.OnClickListener {
    private var transactionData: List<Transaction>? = null

    @Inject
    lateinit var imageViewIntentProvider: ImageViewIntentProvider

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var prefHandler: PrefHandler

    @BindView(R.id.progress)
    lateinit var progressView: View

    @BindView(R.id.error)
    lateinit var errorView: TextView

    @BindView(R.id.Table)
    lateinit var tableView: ViewGroup

    @BindView(R.id.SplitContainer)
    lateinit var splitContainer: ViewGroup

    @BindView(R.id.split_list)
    lateinit var splitList: RecyclerView

    @BindView(R.id.AccountLabel)
    lateinit var accountLabelView: TextView

    @BindView(R.id.CategoryLabel)
    lateinit var categoryLabelView: TextView

    @BindView(R.id.PayeeLabel)
    lateinit var payeeLabelView: TextView

    @BindView(R.id.DateLabel)
    lateinit var dateLabel: TextView

    @BindView(R.id.Account)
    lateinit var accountView: TextView

    @BindView(R.id.Category)
    lateinit var categoryView: TextView

    @BindView(R.id.CategoryRow)
    lateinit var categoryRow: View

    @BindView(R.id.CommentRow)
    lateinit var commentRow: View

    @BindView(R.id.NumberRow)
    lateinit var numberRow: View

    @BindView(R.id.PayeeRow)
    lateinit var payeeRow: View

    @BindView(R.id.MethodRow)
    lateinit var methodRow: View

    @BindView(R.id.StatusRow)
    lateinit var statusRow: View

    @BindView(R.id.PlanRow)
    lateinit var planRow: View

    @BindView(R.id.Date2Row)
    lateinit var date2Row: View

    @BindView(R.id.OriginalAmountRow)
    lateinit var originalAmountRow: View

    @BindView(R.id.EquivalentAmountRow)
    lateinit var equivalentAmountRow: View

    @BindView(R.id.Date)
    lateinit var dateView: TextView

    @BindView(R.id.Date2)
    lateinit var date2View: TextView

    @BindView(R.id.Amount)
    lateinit var amountView: TextView

    @BindView(R.id.OriginalAmount)
    lateinit var originalAmountView: TextView

    @BindView(R.id.EquivalentAmount)
    lateinit var equivalentAmountView: TextView

    @BindView(R.id.Comment)
    lateinit var commentView: TextView

    @BindView(R.id.Number)
    lateinit var numberView: TextView

    @BindView(R.id.Payee)
    lateinit var payeeView: TextView

    @BindView(R.id.Method)
    lateinit var methodView: TextView

    @BindView(R.id.Status)
    lateinit var statusView: TextView

    @BindView(R.id.Plan)
    lateinit var planView: TextView

    @BindView(R.id.TagGroup)
    lateinit var tagGroup: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyApplication.getInstance().appComponent.inject(this)
        val viewModel = ViewModelProvider(this).get(TransactionDetailViewModel::class.java)
        val rowId = arguments!!.getLong(DatabaseConstants.KEY_ROWID)
        viewModel.transaction(rowId)
                .observe(this, Observer { o -> fillData(o) })
        //viewModel.getTags().observe(this, tags -> addChipsBulk(tagGroup, tags, null));
        //viewModel.loadOriginalTags(rowId);
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val li = LayoutInflater.from(activity)
        dialogView = li.inflate(R.layout.transaction_detail, null)
        ButterKnife.bind(this, dialogView)
        val alertDialog = AlertDialog.Builder(activity!!)
                .setTitle(R.string.progress_dialog_loading) //.setIcon(android.R.color.transparent)
                .setView(dialogView)
                .setNegativeButton(android.R.string.ok, this)
                .setPositiveButton(R.string.menu_edit, null)
                .setNeutralButton(R.string.menu_view_picture, this)
                .create()
        alertDialog.setOnShowListener(object : ButtonOnShowDisabler() {
            override fun onShow(dialog: DialogInterface) {
                if (transactionData == null) {
                    super.onShow(dialog)
                    val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL)
                    if (button != null) {
                        button.visibility = View.GONE
                    }
                }
                //prevent automatic dismiss on button click
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { v: View? -> onClick(alertDialog, AlertDialog.BUTTON_POSITIVE) }
            }
        })
        return alertDialog
    }

    /*  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
    if (getActivity() == null) {
      return null;
    }
    switch (id) {
      case SPLIT_PART_CURSOR:
        CursorLoader cursorLoader = new CursorLoader(getActivity(), TransactionProvider.TRANSACTIONS_URI, null, "parent_id = ?",
            new String[]{String.valueOf(mTransaction.getId())}, null);
        return cursorLoader;
    }
    return null;
  }*/
    override fun onClick(dialog: DialogInterface, which: Int) {
        val ctx: Activity? = activity
        if (ctx == null) {
            return
        }
        transactionData?.let {
            val transaction = it[0]
            when (which) {
                AlertDialog.BUTTON_POSITIVE -> {
                    if (transaction.isTransfer && transaction.hasTransferPeerParent) {
                        showSnackbar(R.string.warning_splitpartcategory_context)
                        return
                    }
                    dismiss()
                    val i = Intent(ctx, ExpenseEdit::class.java)
                    i.putExtra(DatabaseConstants.KEY_ROWID, transaction.id)
                    //i.putExtra("operationType", operationType);
                    ctx.startActivityForResult(i, ProtectedFragmentActivity.EDIT_REQUEST)
                }
                AlertDialog.BUTTON_NEUTRAL -> {
                    transaction.pictureUri?.let {
                        imageViewIntentProvider.startViewIntent(ctx, it)
                    }
                    return
                }
            }
        }
    }

    fun fillData(list: List<Transaction>) {
        transactionData = list
        (dialog as? AlertDialog)?.let { dlg ->
            if (list.size > 0) {
                val transaction = list[0]
                progressView.visibility = View.GONE
                var doShowPicture = false
                if (transaction.pictureUri != null) {
                    doShowPicture = true
                    try {
                        if (!PictureDirHelper.doesPictureExist(transaction.pictureUri)) {
                            showSnackbar(R.string.image_deleted)
                            doShowPicture = false
                        }
                    } catch (e: IllegalArgumentException) {
                        CrashHandler.report(e)
                        showSnackbar("Unable to handle image: " + e.message, Snackbar.LENGTH_LONG, null)
                        doShowPicture = false
                    }
                }
                var btn = dlg.getButton(AlertDialog.BUTTON_POSITIVE)
                if (btn != null) {
                    if (transaction.crStatus == CrStatus.VOID || transaction.isSealed) {
                        btn.visibility = View.GONE
                    } else {
                        btn.isEnabled = true
                    }
                }
                btn = dlg.getButton(AlertDialog.BUTTON_NEUTRAL)
                if (btn != null) {
                    btn.visibility = if (doShowPicture) View.VISIBLE else View.GONE
                }
                tableView.visibility = View.VISIBLE
                val title: Int
                val isIncome = transaction.amount.amountMinor > 0
                if (transaction.isSplit) {
                    splitContainer.visibility = View.VISIBLE
                    title = R.string.split_transaction
                    SplitPartRVAdapter(context!!, transaction.amount.currencyUnit, currencyFormatter, list.subList(1, list.size)).also {
                        splitList.adapter = it
                        it.notifyDataSetChanged()
                    }
                } else if (transaction.isTransfer) {
                    title = R.string.transfer
                    accountLabelView.setText(R.string.transfer_from_account)
                    categoryLabelView.setText(R.string.transfer_to_account)
                } else {
                    title = if (isIncome) R.string.income else R.string.expense
                }
                val amountText: String
                if (transaction.isTransfer) {
                    accountView.text = if (isIncome) transaction.label else transaction.accountLabel
                    categoryView.text = if (isIncome) transaction.accountLabel else transaction.label
                    amountText = if (transaction.isSameCurrency) {
                        formatCurrencyAbs(transaction.amount)
                    } else {
                        val self = formatCurrencyAbs(transaction.amount)
                        val other = formatCurrencyAbs(transaction.transferAmount)
                        if (isIncome) "$other => $self" else "$self => $other"
                    }
                } else {
                    accountView.text = transaction.accountLabel
                    if (transaction.catId != null && transaction.catId > 0) {
                        categoryView.text = transaction.label
                    } else {
                        categoryRow.visibility = View.GONE
                    }
                    amountText = formatCurrencyAbs(transaction.amount)
                }
                amountView.text = amountText
                if (transaction.originalAmount != null) {
                    originalAmountRow.visibility = View.VISIBLE
                    originalAmountView.text = formatCurrencyAbs(transaction.originalAmount)
                }
                if (!transaction.isTransfer && transaction.amount.currencyUnit.code() != Utils.getHomeCurrency().code()) {
                    equivalentAmountRow.visibility = View.VISIBLE
                    equivalentAmountView.text = formatCurrencyAbs(transaction.equivalentAmount)
                }
                val dateMode = UiUtils.getDateMode(transaction.accountType, prefHandler)
                val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
                if (dateMode == DateMode.BOOKING_VALUE) {
                    dateLabel.setText(R.string.booking_date)
                    date2Row.visibility = View.VISIBLE
                    date2View.text = ZonedDateTime.ofInstant(Instant.ofEpochSecond(transaction.valueDate),
                            ZoneId.systemDefault()).format(dateFormatter)
                }
                val dateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(transaction.date),
                        ZoneId.systemDefault())
                var dateText = dateTime.format(dateFormatter)
                if (dateMode == DateMode.DATE_TIME) {
                    dateText += " " + dateTime.format(timeFormatter)
                }
                dateView.text = dateText
                if (transaction.comment != "") {
                    commentView.text = transaction.comment
                } else {
                    commentRow.visibility = View.GONE
                }
                if (transaction.referenceNumber != "") {
                    numberView.text = transaction.referenceNumber
                } else {
                    numberRow.visibility = View.GONE
                }
                if (transaction.payee != "") {
                    payeeView.text = transaction.payee
                    payeeLabelView.setText(if (isIncome) R.string.payer else R.string.payee)
                } else {
                    payeeRow.visibility = View.GONE
                }
                if (transaction.methodLabel != null) {
                    methodView.text = transaction.methodLabel
                } else {
                    methodRow.visibility = View.GONE
                }
                if (transaction.accountType == AccountType.CASH) {
                    statusRow.visibility = View.GONE
                } else {
                    statusView.setBackgroundColor(transaction.crStatus.color)
                    statusView.setText(transaction.crStatus.toStringRes())
                }
                if (transaction.originTemplatePlanInfo == null) {
                    planRow.visibility = View.GONE
                } else {
                    planView.text = transaction.originTemplatePlanInfo
                }

/*    if (mTransaction.getOriginTemplatePlan() == null) {
      planRow.setVisibility(View.GONE);
    } else {
      viewModel.transaction(mTransaction.getOriginTemplateId(), TransactionViewModel.InstantiationTask.TEMPLATE, false, false, null).observe(this,
          transaction -> {
            Template template = ((Template) transaction);
            planView.setText(template.getPlan() == null ?
                getString(R.string.plan_event_deleted) : Plan.prettyTimeInfo(getActivity(),
                template.getPlan().rrule, template.getPlan().dtstart));
          });
    }*/
                dlg.setTitle(title)
                if (doShowPicture) {
                    dlg.window?.findViewById<ImageView>(android.R.id.icon)?.let { image ->
                        image.visibility = View.VISIBLE
                        image.scaleType = ImageView.ScaleType.CENTER_CROP
                        Picasso.get().load(transaction.pictureUri).fit().into(image)
                    }
                }
            } else {
                errorView.visibility = View.VISIBLE
                errorView.setText(R.string.transaction_deleted)
            }
        }
    }

    private fun formatCurrencyAbs(money: Money?): String {
        return currencyFormatter.formatCurrency(money!!.amountMajor.abs(), money.currencyUnit)
    }

    companion object {
        @JvmStatic
        fun newInstance(id: Long?): TransactionDetailFragment {
            val dialogFragment = TransactionDetailFragment()
            val bundle = Bundle()
            bundle.putLong(DatabaseConstants.KEY_ROWID, id!!)
            dialogFragment.arguments = bundle
            return dialogFragment
        }
    }
}