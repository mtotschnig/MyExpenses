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

package org.totschnig.myexpenses.dialog;


import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ImageViewIntentProvider;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.SplitPartAdapter;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.PictureDirHelper;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.math.BigDecimal;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import butterknife.BindView;
import butterknife.ButterKnife;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_MAIN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public class TransactionDetailFragment extends CommitSafeDialogFragment implements LoaderManager.LoaderCallbacks<Cursor>, OnClickListener {
  public static final int SPLIT_PART_CURSOR = 3;
  Transaction mTransaction;
  SimpleCursorAdapter mAdapter;

  @Inject
  ImageViewIntentProvider imageViewIntentProvider;

  @Inject
  CurrencyFormatter currencyFormatter;

  @Inject
  PrefHandler prefHandler;

  @BindView(R.id.progress)
  View progressView;
  @BindView(R.id.error)
  TextView errorView;
  @BindView(R.id.Table)
  ViewGroup tableView;
  @BindView(R.id.SplitContainer)
  ViewGroup splitContainer;
  @BindView(R.id.empty)
  View emptyView;
  @BindView(R.id.list)
  ListView listView;
  @BindView(R.id.AccountLabel)
  TextView accountLabelView;
  @BindView(R.id.CategoryLabel)
  TextView categoryLabelView;
  @BindView(R.id.PayeeLabel)
  TextView payeeLabelView;
  @BindView(R.id.DateLabel)
  TextView dateLabel;
  @BindView(R.id.Account)
  TextView accountView;
  @BindView(R.id.Category)
  TextView categoryView;
  @BindView(R.id.CategoryRow)
  View categoryRow;
  @BindView(R.id.CommentRow)
  View commentRow;
  @BindView(R.id.NumberRow)
  View numberRow;
  @BindView(R.id.PayeeRow)
  View payeeRow;
  @BindView(R.id.MethodRow)
  View methodRow;
  @BindView(R.id.StatusRow)
  View statusRow;
  @BindView(R.id.PlanRow)
  View planRow;
  @BindView(R.id.Date2Row)
  View date2Row;
  @BindView(R.id.OriginalAmountRow)
  View originalAmountRow;
  @BindView(R.id.EquivalentAmountRow)
  View equivalentAmountRow;
  @BindView(R.id.Date)
  TextView dateView;
  @BindView(R.id.Date2)
  TextView date2View;
  @BindView(R.id.Amount)
  TextView amountView;
  @BindView(R.id.OriginalAmount)
  TextView originalAmountView;
  @BindView(R.id.EquivalentAmount)
  TextView equivalentAmountView;
  @BindView(R.id.Comment)
  TextView commentView;
  @BindView(R.id.Number)
  TextView numberView;
  @BindView(R.id.Payee)
  TextView payeeView;
  @BindView(R.id.Method)
  TextView methodView;
  @BindView(R.id.Status)
  TextView statusView;
  @BindView(R.id.Plan)
  TextView planView;

  public static final TransactionDetailFragment newInstance(Long id) {
    TransactionDetailFragment dialogFragment = new TransactionDetailFragment();
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_ROWID, id);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MyApplication.getInstance().getAppComponent().inject(this);
    final ProtectedFragmentActivity activity = (ProtectedFragmentActivity) getActivity();
    if (activity != null && !activity.hasPendingTask(false)) {
      activity.startTaskExecution(
          TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_2,
          new Long[]{getArguments().getLong(KEY_ROWID)},
          null,
          0);
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    final LayoutInflater li = LayoutInflater.from(getActivity());
    //noinspection InflateParams
    dialogView = li.inflate(R.layout.transaction_detail, null);
    ButterKnife.bind(this, dialogView);
    AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
        .setTitle(R.string.progress_dialog_loading)
        //.setIcon(android.R.color.transparent)
        .setView(dialogView)
        .setNegativeButton(android.R.string.ok, this)
        .setPositiveButton(R.string.menu_edit, null)
        .setNeutralButton(R.string.menu_view_picture, this)
        .create();
    alertDialog.setOnShowListener(new ButtonOnShowDisabler() {
      @Override
      public void onShow(DialogInterface dialog) {
        if (mTransaction == null) {
          super.onShow(dialog);
          Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
          if (button != null) {
            button.setVisibility(View.GONE);
          }
        }
        //prevent automatic dismiss on button click
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> onClick(alertDialog, AlertDialog.BUTTON_POSITIVE));
      }
    });
    return alertDialog;
  }

  @Override
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
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    int id = loader.getId();
    switch (id) {
      case SPLIT_PART_CURSOR:
        mAdapter.swapCursor(cursor);
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mAdapter.swapCursor(null);
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    Activity ctx = getActivity();
    if (ctx == null || mTransaction == null) {
      return;
    }
    switch (which) {
      case AlertDialog.BUTTON_POSITIVE:
        //TODO strict mode
        if (mTransaction instanceof Transfer && Transaction.hasParent(((Transfer) mTransaction).getTransferPeer())) {
          showSnackbar(R.string.warning_splitpartcategory_context);
          return;
        }
        dismiss();
        Intent i = new Intent(ctx, ExpenseEdit.class);
        i.putExtra(KEY_ROWID, mTransaction.getId());
        //i.putExtra("operationType", operationType);
        ctx.startActivityForResult(i, ProtectedFragmentActivity.EDIT_REQUEST);
        break;
      case AlertDialog.BUTTON_NEUTRAL:
        if (mTransaction.getPictureUri() != null) {
          imageViewIntentProvider.startViewIntent(ctx, mTransaction.getPictureUri());
        }
        break;
    }
  }

  public void fillData(Transaction o) {
    if (o == null) {
      errorView.setVisibility(View.VISIBLE);
      errorView.setText(R.string.transaction_deleted);
      return;
    }
    final ProtectedFragmentActivity ctx = (ProtectedFragmentActivity) getActivity();
    progressView.setVisibility(View.GONE);
    mTransaction = o;
    final Account account = Account.getInstanceFromDb(mTransaction.getAccountId());
    boolean doShowPicture = false;
    if (mTransaction.getPictureUri() != null) {
      doShowPicture = true;
      try {
        if (!PictureDirHelper.doesPictureExist(mTransaction.getPictureUri())) {
          showSnackbar(R.string.image_deleted);
          doShowPicture = false;
        }
      } catch (IllegalArgumentException e) {
        CrashHandler.report(e);
        showSnackbar("Unable to handle image: " + e.getMessage(), Snackbar.LENGTH_LONG, null);
        doShowPicture = false;
      }
    }
    AlertDialog dlg = (AlertDialog) getDialog();
    if (dlg != null) {
      Button btn = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
      if (btn != null) {
        if (mTransaction.getCrStatus() == Transaction.CrStatus.VOID || mTransaction.isSealed()) {
          btn.setVisibility(View.GONE);
        } else {
          btn.setEnabled(true);
        }
      }
      btn = dlg.getButton(AlertDialog.BUTTON_NEUTRAL);
      if (btn != null) {
        btn.setVisibility(doShowPicture ? View.VISIBLE : View.GONE);
      }
    }
    tableView.setVisibility(View.VISIBLE);
    int title;
    boolean isIncome = mTransaction.getAmount().getAmountMinor() > 0;

    if (mTransaction instanceof SplitTransaction) {
      splitContainer.setVisibility(View.VISIBLE);
      //TODO: refactor duplicated code with SplitPartList
      title = R.string.split_transaction;

      // Create an array to specify the fields we want to display in the list
      String[] from = new String[]{KEY_LABEL_MAIN, KEY_AMOUNT};

      // and an array of the fields we want to bind those fields to 
      int[] to = new int[]{R.id.category, R.id.amount};

      // Now create a simple cursor adapter and set it to display
      mAdapter = new SplitPartAdapter(ctx, R.layout.split_part_row, null, from, to, 0,
          mTransaction.getAmount().getCurrencyUnit(), currencyFormatter);
      listView.setAdapter(mAdapter);
      listView.setEmptyView(emptyView);

      LoaderManager manager = getLoaderManager();
      if (manager.getLoader(SPLIT_PART_CURSOR) != null &&
          !manager.getLoader(SPLIT_PART_CURSOR).isReset()) {
        manager.restartLoader(SPLIT_PART_CURSOR, null, this);
      } else {
        manager.initLoader(SPLIT_PART_CURSOR, null, this);
      }

    } else {
      if (mTransaction instanceof Transfer) {
        title = R.string.transfer;
        accountLabelView.setText(R.string.transfer_from_account);
        categoryLabelView.setText(R.string.transfer_to_account);
      } else {
        title = isIncome ? R.string.income : R.string.expense;
      }
    }

    String amountText;
    String accountLabel = account.getLabel();
    if (mTransaction instanceof Transfer) {
      accountView.setText(isIncome ? mTransaction.getLabel() : accountLabel);
      categoryView.setText(isIncome ? accountLabel : mTransaction.getLabel());
      if (((Transfer) mTransaction).isSameCurrency()) {
        amountText = formatCurrencyAbs(mTransaction.getAmount());
      } else {
        String self = formatCurrencyAbs(mTransaction.getAmount());
        String other = formatCurrencyAbs(mTransaction.getTransferAmount());
        amountText = isIncome ? (other + " => " + self) : (self + " => " + other);
      }
    } else {
      accountView.setText(accountLabel);
      if ((mTransaction.getCatId() != null && mTransaction.getCatId() > 0)) {
        categoryView.setText(mTransaction.getLabel());
      } else {
        categoryRow.setVisibility(View.GONE);
      }
      amountText = formatCurrencyAbs(mTransaction.getAmount());
    }

    amountView.setText(amountText);

    if (mTransaction.getOriginalAmount() != null) {
      originalAmountRow.setVisibility(View.VISIBLE);
      originalAmountView.setText(formatCurrencyAbs(mTransaction.getOriginalAmount()));
    }

    if (!(mTransaction instanceof Transfer) && !account.getCurrencyUnit().code().equals(Utils.getHomeCurrency().code())) {
      equivalentAmountRow.setVisibility(View.VISIBLE);
      Money equivalentAmount = mTransaction.getEquivalentAmount();
      if (equivalentAmount == null) {
        equivalentAmount = new Money(Utils.getHomeCurrency(),
            mTransaction.getAmount().getAmountMajor()
                .multiply(new BigDecimal(account.getExchangeRate())));
      }
      equivalentAmountView.setText(formatCurrencyAbs(equivalentAmount));
    }

    UiUtils.DateMode dateMode = UiUtils.getDateMode(account.getType(), prefHandler);
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL),
        timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
    if (dateMode == UiUtils.DateMode.BOOKING_VALUE) {
      dateLabel.setText(R.string.booking_date);
      date2Row.setVisibility(View.VISIBLE);
      date2View.setText(ZonedDateTime.ofInstant(Instant.ofEpochSecond(mTransaction.getValueDate()),
          ZoneId.systemDefault()).format(dateFormatter));
    }
    final ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(mTransaction.getDate()),
        ZoneId.systemDefault());
    String dateText = dateTime.format(dateFormatter);
    if (dateMode == UiUtils.DateMode.DATE_TIME) {
      dateText += " " + dateTime.format(timeFormatter);
    }
    dateView.setText(dateText);

    if (!mTransaction.getComment().equals("")) {
      commentView.setText(mTransaction.getComment());
    } else {
      commentRow.setVisibility(View.GONE);
    }

    if (!mTransaction.getReferenceNumber().equals("")) {
      numberView.setText(mTransaction.getReferenceNumber());
    } else {
      numberRow.setVisibility(View.GONE);
    }

    if (!mTransaction.getPayee().equals("")) {
      payeeView.setText(mTransaction.getPayee());
      payeeLabelView.setText(isIncome ? R.string.payer : R.string.payee);
    } else {
      payeeRow.setVisibility(View.GONE);
    }

    if (mTransaction.getMethodId() != null) {
      methodView.setText(PaymentMethod.getInstanceFromDb(mTransaction.getMethodId()).getLabel());
    } else {
      methodRow.setVisibility(View.GONE);
    }

    if (account.getType().equals(AccountType.CASH)) {
      statusRow.setVisibility(View.GONE);
    } else {
      statusView.setBackgroundColor(mTransaction.getCrStatus().color);
      statusView.setText(mTransaction.getCrStatus().toStringRes());
    }

    if (mTransaction.getOriginTemplate() == null) {
      planRow.setVisibility(View.GONE);
    } else {
      planView.setText(mTransaction.getOriginTemplate().getPlan() == null ?
          getString(R.string.plan_event_deleted) : Plan.prettyTimeInfo(getActivity(),
          mTransaction.getOriginTemplate().getPlan().rrule, mTransaction.getOriginTemplate().getPlan().dtstart));
    }

    dlg.setTitle(title);
    if (doShowPicture) {
      ImageView image = dlg.getWindow().findViewById(android.R.id.icon);
      image.setVisibility(View.VISIBLE);
      image.setScaleType(ImageView.ScaleType.CENTER_CROP);
      Picasso.get().load(mTransaction.getPictureUri()).fit().into(image);
    }
  }

  @NonNull
  private String formatCurrencyAbs(Money money) {
    return currencyFormatter.formatCurrency(money.getAmountMajor().abs(), money.getCurrencyUnit());
  }
}
