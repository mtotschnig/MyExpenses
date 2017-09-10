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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

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
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.PictureDirHelper;

import java.text.DateFormat;

import javax.inject.Inject;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_MAIN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public class TransactionDetailFragment extends CommitSafeDialogFragment implements LoaderManager.LoaderCallbacks<Cursor>, OnClickListener {
  public static final int SPLIT_PART_CURSOR = 3;
  Transaction mTransaction;
  SimpleCursorAdapter mAdapter;
  View mLayout;

  @Inject
  ImageViewIntentProvider imageViewIntentProvider;

  @Inject
  CurrencyFormatter currencyFormatter;

  public static final TransactionDetailFragment newInstance(Long id) {
    TransactionDetailFragment dialogFragment = new TransactionDetailFragment();
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_ROWID, id);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    ((ProtectedFragmentActivity) activity).startTaskExecution(
        TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_2,
        new Long[]{getArguments().getLong(KEY_ROWID)},
        null,
        0);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    final LayoutInflater li = LayoutInflater.from(getActivity());
    //noinspection InflateParams
    mLayout = li.inflate(R.layout.transaction_detail, null);
    AlertDialog dialog = new AlertDialog.Builder(getActivity())
        .setTitle(R.string.progress_dialog_loading)
        //.setIcon(android.R.color.transparent)
        .setView(mLayout)
        .setNegativeButton(android.R.string.ok, this)
        .setPositiveButton(R.string.menu_edit, this)
        .setNeutralButton(R.string.menu_view_picture, this)
        .create();
    dialog.setOnShowListener(new ButtonOnShowDisabler() {
      @Override
      public void onShow(DialogInterface dialog) {
        if (mTransaction == null) {
          super.onShow(dialog);
          Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
          if (button != null) {
            button.setVisibility(View.GONE);
          }
        }
      }
    });
    return dialog;
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
        if (mTransaction instanceof Transfer && DbUtils.hasParent(((Transfer) mTransaction).getTransferPeer())) {
          Toast.makeText(ctx, getString(R.string.warning_splitpartcategory_context), Toast.LENGTH_LONG).show();
          return;
        }
        Intent i = new Intent(ctx, ExpenseEdit.class);
        i.putExtra(KEY_ROWID, mTransaction.getId());
        //i.putExtra("operationType", operationType);
        ctx.startActivityForResult(i, ProtectedFragmentActivity.EDIT_TRANSACTION_REQUEST);
        break;
      case AlertDialog.BUTTON_NEUTRAL:
        startActivity(imageViewIntentProvider.getViewIntent(ctx, mTransaction.getPictureUri()));
        break;
      case AlertDialog.BUTTON_NEGATIVE:
        dismiss();
    }
  }

  public void fillData(Transaction o) {
    final FragmentActivity ctx = getActivity();
    mLayout.findViewById(R.id.progress).setVisibility(View.GONE);
    mTransaction = o;
    if (mTransaction == null) {
      TextView error = (TextView) mLayout.findViewById(R.id.error);
      error.setVisibility(View.VISIBLE);
      error.setText(R.string.transaction_deleted);
      return;
    }
    boolean doShowPicture = false;
    if (mTransaction.getPictureUri() != null) {
      doShowPicture = true;
      try {
        if (!PictureDirHelper.doesPictureExist(mTransaction.getPictureUri())) {
          Toast.makeText(getActivity(), R.string.image_deleted, Toast.LENGTH_SHORT).show();
          doShowPicture = false;
        }
      } catch (IllegalArgumentException e) {
        AcraHelper.report(e);
        Toast.makeText(getActivity(), "Unable to handle image " + e.getMessage(), Toast.LENGTH_SHORT).show();
        doShowPicture = false;
      }
    }
    AlertDialog dlg = (AlertDialog) getDialog();
    if (dlg != null) {
      Button btn = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
      if (btn != null) {
        if (mTransaction.crStatus != Transaction.CrStatus.VOID) {
          btn.setEnabled(true);
        } else {
          btn.setVisibility(View.GONE);
        }
      }
      btn = dlg.getButton(AlertDialog.BUTTON_NEUTRAL);
      if (btn != null) {
        btn.setVisibility(doShowPicture ? View.VISIBLE : View.GONE);
      }
    }
    mLayout.findViewById(R.id.Table).setVisibility(View.VISIBLE);
    int title;
    boolean type = mTransaction.getAmount().getAmountMinor() > 0 ? ExpenseEdit.INCOME : ExpenseEdit.EXPENSE;

    if (mTransaction instanceof SplitTransaction) {
      mLayout.findViewById(R.id.SplitContainer).setVisibility(View.VISIBLE);
      //TODO: refactor duplicated code with SplitPartList
      title = R.string.split_transaction;
      View emptyView = mLayout.findViewById(R.id.empty);

      ListView lv = (ListView) mLayout.findViewById(R.id.list);
      // Create an array to specify the fields we want to display in the list
      String[] from = new String[]{KEY_LABEL_MAIN, KEY_AMOUNT};

      // and an array of the fields we want to bind those fields to 
      int[] to = new int[]{R.id.category, R.id.amount};

      // Now create a simple cursor adapter and set it to display
      mAdapter = new SplitPartAdapter(ctx, R.layout.split_part_row, null, from, to, 0,
          mTransaction.getAmount().getCurrency(), currencyFormatter);
      lv.setAdapter(mAdapter);
      lv.setEmptyView(emptyView);

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
        ((TextView) mLayout.findViewById(R.id.AccountLabel)).setText(R.string.transfer_from_account);
        ((TextView) mLayout.findViewById(R.id.CategoryLabel)).setText(R.string.transfer_to_account);
      } else {
        title = type ? R.string.income : R.string.expense;
      }
    }

    String amountText;
    String accountLabel = Account.getInstanceFromDb(mTransaction.getAccountId()).label;
    if (mTransaction instanceof Transfer) {
      ((TextView) mLayout.findViewById(R.id.Account)).setText(type ? mTransaction.getLabel() : accountLabel);
      ((TextView) mLayout.findViewById(R.id.Category)).setText(type ? accountLabel : mTransaction.getLabel());
      if (((Transfer) mTransaction).isSameCurrency()) {
        amountText = formatCurrencyAbs(mTransaction.getAmount());
      } else {
        String self = formatCurrencyAbs(mTransaction.getAmount());
        String other = formatCurrencyAbs(mTransaction.getTransferAmount());
        amountText = type == ExpenseEdit.EXPENSE ? (self + " => " + other) : (other + " => " + self);
      }
    } else {
      ((TextView) mLayout.findViewById(R.id.Account)).setText(accountLabel);
      if ((mTransaction.getCatId() != null && mTransaction.getCatId() > 0)) {
        ((TextView) mLayout.findViewById(R.id.Category)).setText(mTransaction.getLabel());
      } else {
        mLayout.findViewById(R.id.CategoryRow).setVisibility(View.GONE);
      }
      amountText = formatCurrencyAbs(mTransaction.getAmount());
    }

    //noinspection SetTextI18n
    ((TextView) mLayout.findViewById(R.id.Date)).setText(
        DateFormat.getDateInstance(DateFormat.FULL).format(mTransaction.getDate())
            + " "
            + DateFormat.getTimeInstance(DateFormat.SHORT).format(mTransaction.getDate()));

    ((TextView) mLayout.findViewById(R.id.Amount)).setText(amountText);

    if (!mTransaction.getComment().equals("")) {
      ((TextView) mLayout.findViewById(R.id.Comment)).setText(mTransaction.getComment());
    } else {
      mLayout.findViewById(R.id.CommentRow).setVisibility(View.GONE);
    }

    if (!mTransaction.getReferenceNumber().equals("")) {
      ((TextView) mLayout.findViewById(R.id.Number)).setText(mTransaction.getReferenceNumber());
    } else {
      mLayout.findViewById(R.id.NumberRow).setVisibility(View.GONE);
    }

    if (!mTransaction.getPayee().equals("")) {
      ((TextView) mLayout.findViewById(R.id.Payee)).setText(mTransaction.getPayee());
      ((TextView) mLayout.findViewById(R.id.PayeeLabel)).setText(type ? R.string.payer : R.string.payee);
    } else {
      mLayout.findViewById(R.id.PayeeRow).setVisibility(View.GONE);
    }

    if (mTransaction.getMethodId() != null) {
      ((TextView) mLayout.findViewById(R.id.Method))
          .setText(PaymentMethod.getInstanceFromDb(mTransaction.getMethodId()).getLabel());
    } else {
      mLayout.findViewById(R.id.MethodRow).setVisibility(View.GONE);
    }

    if (Account.getInstanceFromDb(mTransaction.getAccountId()).type.equals(AccountType.CASH)) {
      mLayout.findViewById(R.id.StatusRow).setVisibility(View.GONE);
    } else {
      TextView tv = (TextView) mLayout.findViewById(R.id.Status);
      tv.setBackgroundColor(mTransaction.crStatus.color);
      tv.setText(mTransaction.crStatus.toStringRes());
    }

    if (mTransaction.originTemplate == null) {
      mLayout.findViewById(R.id.PlannerRow).setVisibility(View.GONE);
    } else {
      ((TextView) mLayout.findViewById(R.id.Plan)).setText(mTransaction.originTemplate.getPlan() == null ?
          getString(R.string.plan_event_deleted) : Plan.prettyTimeInfo(getActivity(),
          mTransaction.originTemplate.getPlan().rrule, mTransaction.originTemplate.getPlan().dtstart));
    }

    dlg.setTitle(title);
    if (doShowPicture) {
      ImageView image = ((ImageView) dlg.getWindow().findViewById(android.R.id.icon));
      image.setVisibility(View.VISIBLE);
      image.setScaleType(ImageView.ScaleType.CENTER_CROP);
      Picasso.with(ctx).load(mTransaction.getPictureUri()).fit().into(image);
    }
  }

  @NonNull
  private String formatCurrencyAbs(Money money) {
    return currencyFormatter.formatCurrency(money.getAmountMajor().abs(), money.getCurrency());
  }
}
