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

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.EDIT_REQUEST
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.ImageViewIntentProvider
import org.totschnig.myexpenses.adapter.SplitPartRVAdapter
import org.totschnig.myexpenses.databinding.TransactionDetailBinding
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.UiUtils.DateMode
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.addChipsBulk
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.TransactionDetailViewModel
import org.totschnig.myexpenses.viewmodel.data.Transaction
import javax.inject.Inject

class TransactionDetailFragment : BaseDialogFragment(), DialogInterface.OnClickListener {
    private var transactionData: List<Transaction>? = null
    private var _binding: TransactionDetailBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    @Inject
    lateinit var imageViewIntentProvider: ImageViewIntentProvider

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var picasso: Picasso

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().applicationContext as MyApplication).appComponent.inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = initBuilderWithBinding {
            TransactionDetailBinding.inflate(materialLayoutInflater).also { _binding = it }
        }
        val viewModel = ViewModelProvider(this).get(TransactionDetailViewModel::class.java)
        val rowId = requireArguments().getLong(DatabaseConstants.KEY_ROWID)
        viewModel.transaction(rowId).observe(this, { o -> fillData(o) })
        viewModel.getTags().observe(this, { tags ->
            if (tags.size > 0) {
                binding.TagGroup.addChipsBulk(tags, null)
            } else {
                binding.TagRow.visibility = View.GONE
            }
        })
        viewModel.loadOriginalTags(rowId, TransactionProvider.TRANSACTIONS_TAGS_URI, DatabaseConstants.KEY_TRANSACTIONID)
        val alertDialog = builder.setTitle(R.string.progress_dialog_loading) //.setIcon(android.R.color.transparent)
                .setNegativeButton(android.R.string.ok, this)
                .setPositiveButton(R.string.menu_edit, null)
                .setNeutralButton(R.string.menu_view_picture, this)
                .create()
        alertDialog.setOnShowListener(object : ButtonOnShowDisabler() {
            override fun onShow(dialog: DialogInterface) {
                if (transactionData == null) {
                    super.onShow(dialog)
                    (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL)?.let { it.visibility = View.GONE }
                }
                //prevent automatic dismiss on button click
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onClick(alertDialog, AlertDialog.BUTTON_POSITIVE) }
            }
        })
        return alertDialog
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val ctx: FragmentActivity = activity ?: return
        transactionData?.takeIf { it.isNotEmpty() }?.let { list ->
            val transaction = list[0]
            when (which) {
                AlertDialog.BUTTON_POSITIVE -> {
                    if (transaction.isTransfer && transaction.hasTransferPeerParent) {
                        showSnackbar(R.string.warning_splitpartcategory_context)
                        return
                    }
                    dismiss()
                    val i = Intent(ctx, ExpenseEdit::class.java)
                    i.putExtra(DatabaseConstants.KEY_ROWID, transaction.id)
                    ctx.startActivityForResult(i, EDIT_REQUEST)
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

    private fun fillData(list: List<Transaction>) {
        transactionData = list
        (dialog as? AlertDialog)?.let { dlg ->
            if (list.isNotEmpty()) {
                val transaction = list[0]
                binding.progress.visibility = View.GONE
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
                dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.let {
                    if (transaction.crStatus == CrStatus.VOID || transaction.isSealed) {
                        it.visibility = View.GONE
                    } else {
                        it.isEnabled = true
                    }
                }
                dlg.getButton(AlertDialog.BUTTON_NEUTRAL)?.let {
                    it.visibility = if (doShowPicture) View.VISIBLE else View.GONE
                }
                binding.Table.visibility = View.VISIBLE
                val title: Int
                val isIncome = transaction.amount.amountMinor > 0
                when {
                    transaction.isSplit -> {
                        binding.SplitContainer.visibility = View.VISIBLE
                        title = R.string.split_transaction
                        SplitPartRVAdapter(requireContext(), transaction.amount.currencyUnit, currencyFormatter, list.subList(1, list.size)).also {
                            binding.splitList.adapter = it
                            it.notifyDataSetChanged()
                        }
                    }
                    transaction.isTransfer -> {
                        title = R.string.transfer
                        binding.AccountLabel.setText(R.string.transfer_from_account)
                        binding.CategoryLabel.setText(R.string.transfer_to_account)
                    }
                    else -> {
                        title = if (isIncome) R.string.income else R.string.expense
                    }
                }
                val amountText: String
                if (transaction.isTransfer) {
                    binding.Account.text = if (isIncome) transaction.label else transaction.accountLabel
                    binding.Category.text = if (isIncome) transaction.accountLabel else transaction.label
                    amountText = if (transaction.isSameCurrency) {
                        formatCurrencyAbs(transaction.amount)
                    } else {
                        val self = formatCurrencyAbs(transaction.amount)
                        val other = formatCurrencyAbs(transaction.transferAmount)
                        if (isIncome) "$other => $self" else "$self => $other"
                    }
                } else {
                    binding.Account.text = transaction.accountLabel
                    if (transaction.catId != null && transaction.catId > 0) {
                        binding.Category.text = transaction.label
                    } else {
                        binding.CategoryRow.visibility = View.GONE
                    }
                    amountText = formatCurrencyAbs(transaction.amount)
                }
                binding.Amount.text = amountText
                transaction.originalAmount?.let {
                    binding.OriginalAmountRow.visibility = View.VISIBLE
                    binding.OriginalAmount.text = formatCurrencyAbs(it)
                }
                if (!transaction.isTransfer && transaction.amount.currencyUnit.code != Utils.getHomeCurrency().code) {
                    binding.EquivalentAmountRow.visibility = View.VISIBLE
                    binding.EquivalentAmount.text = formatCurrencyAbs(transaction.equivalentAmount)
                }
                val dateMode = UiUtils.getDateMode(transaction.accountType, prefHandler)
                val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
                if (dateMode == DateMode.BOOKING_VALUE) {
                    binding.DateLabel.setText(R.string.booking_date)
                    binding.Date2Row.visibility = View.VISIBLE
                    binding.Date2.text = ZonedDateTime.ofInstant(Instant.ofEpochSecond(transaction.valueDate),
                            ZoneId.systemDefault()).format(dateFormatter)
                }
                val dateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(transaction.date),
                        ZoneId.systemDefault())
                var dateText = dateTime.format(dateFormatter)
                if (dateMode == DateMode.DATE_TIME) {
                    dateText += " " + dateTime.format(timeFormatter)
                }
                binding.Date.text = dateText
                if (transaction.comment != "") {
                    binding.Comment.text = transaction.comment
                } else {
                    binding.CommentRow.visibility = View.GONE
                }
                if (transaction.referenceNumber != "") {
                    binding.Number.text = transaction.referenceNumber
                } else {
                    binding.NumberRow.visibility = View.GONE
                }
                if (transaction.payee != "") {
                    binding.Payee.text = transaction.payee
                    binding.PayeeLabel.setText(if (isIncome) R.string.payer else R.string.payee)
                } else {
                    binding.PayeeRow.visibility = View.GONE
                }
                transaction.methodLabel?.let {
                    binding.Method.text = it
                } ?: kotlin.run { binding.MethodRow.visibility = View.GONE }

                if (transaction.accountType == AccountType.CASH) {
                    binding.StatusRow.visibility = View.GONE
                } else {
                    binding.Status.setBackgroundColor(transaction.crStatus.color)
                    binding.Status.setText(transaction.crStatus.toStringRes())
                }
                if (transaction.originTemplate == null) {
                    binding.PlanRow.visibility = View.GONE
                } else {
                    binding.Plan.text = transaction.originTemplate.plan?.let {
                        Plan.prettyTimeInfo(requireContext(), it.rRule, it.dtStart)
                    } ?: getString(R.string.plan_event_deleted)
                }
                dlg.setTitle(title)
                if (doShowPicture) {
                    dlg.window?.findViewById<ImageView>(android.R.id.icon)?.let { image ->
                        image.visibility = View.VISIBLE
                        image.scaleType = ImageView.ScaleType.CENTER_CROP
                        picasso.load(transaction.pictureUri).fit().into(image)
                    }
                }
            } else {
                binding.error.visibility = View.VISIBLE
                binding.error.setText(R.string.transaction_deleted)
            }
        }
    }

    private fun formatCurrencyAbs(money: Money?): String {
        return currencyFormatter.formatCurrency(money!!.amountMajor.abs(), money.currencyUnit)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(id: Long): TransactionDetailFragment = TransactionDetailFragment().apply {
            arguments = Bundle().apply {
                putLong(DatabaseConstants.KEY_ROWID, id)
            }
        }
    }
}