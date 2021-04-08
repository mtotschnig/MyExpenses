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

package org.totschnig.myexpenses.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.lang3.ArrayUtils;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.Sort;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.TemplatesListViewModel;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import icepick.Icepick;
import icepick.State;

import static android.app.Activity.RESULT_OK;
import static org.totschnig.myexpenses.activity.ConstantsKt.EDIT_REQUEST;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEFAULT_ACTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_MAIN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_SUB;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLAN_INFO;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;
import static org.totschnig.myexpenses.util.Utils.menuItemSetEnabledAndVisible;

public class TemplatesList extends SortableListFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {
  protected static final int SORTABLE_CURSOR = -1;
  public static final String CALDROID_DIALOG_FRAGMENT_TAG = "CALDROID_DIALOG_FRAGMENT";
  public static final String PLANNER_FRAGMENT_TAG = "PLANNER_FRAGMENT";
  private ListView mListView;

  protected int getMenuResource() {
    return R.menu.templateslist_context;
  }

  private Cursor mTemplatesCursor;
  private SimpleCursorAdapter mAdapter;
  private LoaderManager mManager;

  private int columnIndexAmount, columnIndexLabelSub, columnIndexComment,
      columnIndexPayee, columnIndexColor, columnIndexDefaultAction,
      columnIndexCurrency, columnIndexTransferAccount, columnIndexPlanId,
      columnIndexTitle, columnIndexRowId, columnIndexPlanInfo, columnIndexIsSealed;
  private boolean indexesCalculated = false;
  private boolean hasPlans = false;
  /**
   * if we are called from the calendar app, we only need to handle display of plan once
   */
  @State
  boolean expandedHandled = false;

  @State
  boolean repairTriggered = false;

  @Inject
  CurrencyFormatter currencyFormatter;
  @Inject
  CurrencyContext currencyContext;
  @Inject
  PrefHandler prefHandler;

  private TemplatesListViewModel viewModel;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    Icepick.restoreInstanceState(this, savedInstanceState);
    ((MyApplication) requireActivity().getApplication()).getAppComponent().inject(this);
    viewModel = new ViewModelProvider(this).get(TemplatesListViewModel.class);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    Icepick.saveInstanceState(this, outState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    ProtectedFragmentActivity ctx = (ProtectedFragmentActivity) getActivity();
    View v = inflater.inflate(R.layout.templates_list, container, false);
    mListView = v.findViewById(R.id.list);

    mManager = LoaderManager.getInstance(this);
    mManager.initLoader(SORTABLE_CURSOR, null, this);
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{KEY_TITLE, KEY_LABEL_MAIN, KEY_AMOUNT};
    // and an array of the fields we want to bind those fields to
    int[] to = new int[]{R.id.title, R.id.category, R.id.amount};
    mAdapter = new MyAdapter(
        ctx,
        R.layout.template_row,
        null,
        from,
        to,
        0);
    mListView.setAdapter(mAdapter);
    mListView.setEmptyView(v.findViewById(R.id.empty));
    mListView.setOnItemClickListener((parent, view, position, id) -> {
      if (mTemplatesCursor == null || !mTemplatesCursor.moveToPosition(position)) return;
      final boolean isSealed = mTemplatesCursor.getInt(columnIndexIsSealed) != 0;
      if (isSealed) {
        ctx.showSnackbar(R.string.object_sealed);
      }
      if (mTemplatesCursor.isNull(columnIndexPlanId)) {
        if (!isSealed) {
          if (isForeignExchangeTransfer(position)) {
            dispatchCreateInstanceEditDo(id);
          } else {
            boolean splitAtPosition = isSplitAtPosition(position);
            Template.Action defaultAction;
            try {
              defaultAction = Template.Action.valueOf(mTemplatesCursor.getString(columnIndexDefaultAction));
            } catch (IllegalArgumentException e) {
              defaultAction = Template.Action.SAVE;
            }
            if (defaultAction == Template.Action.SAVE) {
              if (splitAtPosition) {
                requestSplitTransaction(new Long[]{id});
              } else {
                dispatchCreateInstanceSaveDo(new Long[]{id}, null);
              }
            } else {
              if (splitAtPosition) {
                requestSplitTransaction(id);
              } else {
                dispatchCreateInstanceEditDo(id);
              }
            }
          }
        }
      } else {
        if (isCalendarPermissionGranted()) {
          PlanMonthFragment planMonthFragment = PlanMonthFragment.newInstance(
              mTemplatesCursor.getString(columnIndexTitle),
              id,
              mTemplatesCursor.getLong(columnIndexPlanId),
              mTemplatesCursor.getInt(columnIndexColor), isSealed);
          if (!getChildFragmentManager().isStateSaved()) {
            planMonthFragment.show(getChildFragmentManager(), CALDROID_DIALOG_FRAGMENT_TAG);
          }
        } else {
          ctx.requestCalendarPermission();
        }
      }
    });
    registerForContextualActionBar(mListView);
    return v;
  }

  private boolean isCalendarPermissionGranted() {
    return CALENDAR.hasPermission(getContext());
  }

  private void bulkUpdateDefaultAction(Long[] itemIds, Template.Action action, int resultFeedBack) {
    viewModel.updateDefaultAction(ArrayUtils.toPrimitive(itemIds), action).observe(getViewLifecycleOwner(), result -> showSnackbar(result ? getString(resultFeedBack) : "Error while setting default action for template click"));
  }

  @Override
  public boolean dispatchCommandMultiple(int command,
                                         SparseBooleanArray positions, Long[] itemIds) {
    if (super.dispatchCommandMultiple(command, positions, itemIds)) {
      return true;
    }
    if (command == R.id.DEFAULT_ACTION_EDIT_COMMAND) {
      bulkUpdateDefaultAction(itemIds, Template.Action.EDIT, R.string.menu_create_instance_edit);
      return true;
    } else if (command == R.id.DEFAULT_ACTION_SAVE_COMMAND) {
      bulkUpdateDefaultAction(itemIds, Template.Action.SAVE, R.string.menu_create_instance_save);
      return true;
    } else if (command == R.id.DELETE_COMMAND) {
      MessageDialogFragment.newInstance(
          getString(R.string.dialog_title_warning_delete_template),//TODO check if template
          getResources().getQuantityString(R.plurals.warning_delete_template, itemIds.length, itemIds.length),
          new MessageDialogFragment.Button(
              R.string.menu_delete,
              R.id.DELETE_COMMAND_DO,
              itemIds),
          null,
          new MessageDialogFragment.Button(R.string.response_no, R.id.CANCEL_CALLBACK_COMMAND, null))
          .show(getActivity().getSupportFragmentManager(), "DELETE_TEMPLATE");
      return true;
    } else if (command == R.id.CREATE_INSTANCE_SAVE_COMMAND) {
      if (hasSplitAtPositions(positions)) {
        requestSplitTransaction(itemIds);
      } else {
        dispatchCreateInstanceSaveDo(itemIds, null);
      }
      finishActionMode();
      return true;
    } else if (command == R.id.CREATE_PLAN_INSTANCE_SAVE_COMMAND || command == R.id.CANCEL_PLAN_INSTANCE_COMMAND || command == R.id.RESET_PLAN_INSTANCE_COMMAND) {
      final PlanMonthFragment planMonthFragment = getPlanMonthFragment();
      if (planMonthFragment != null) {
        planMonthFragment.dispatchCommandMultiple(command, positions);
      }
      finishActionMode();
      return true;
    }
    return false;
  }

  @Override
  public boolean dispatchCommandSingle(int command, ContextMenu.ContextMenuInfo info) {
    if (super.dispatchCommandSingle(command, info)) {
      return true;
    }
    AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) info;
    Intent i;
    if (command == R.id.CREATE_INSTANCE_EDIT_COMMAND) {
      if (isSplitAtPosition(menuInfo.position)) {
        requestSplitTransaction(menuInfo.id);
      } else {
        dispatchCreateInstanceEditDo(menuInfo.id);
      }
      finishActionMode();
      return true;
    } else if (command == R.id.EDIT_COMMAND) {
      finishActionMode();
      i = new Intent(getActivity(), ExpenseEdit.class);
      i.putExtra(KEY_TEMPLATEID, menuInfo.id);
      //TODO check what to do on Result
      startActivityForResult(i, EDIT_REQUEST);
      return true;
    } else if (command == R.id.EDIT_PLAN_INSTANCE_COMMAND || command == R.id.CREATE_PLAN_INSTANCE_EDIT_COMMAND) {
      final PlanMonthFragment planMonthFragment = getPlanMonthFragment();
      if (planMonthFragment != null) {
        planMonthFragment.dispatchCommandSingle(command, menuInfo.position);
      }
      finishActionMode();
      return true;
    }
    return false;
  }

  private boolean isSplitAtPosition(int position) {
    if (mTemplatesCursor != null) {
      return mTemplatesCursor.moveToPosition(position) &&
          SPLIT_CATID.equals(DbUtils.getLongOrNull(mTemplatesCursor, KEY_CATID));
    }
    return false;
  }

  private boolean hasSplitAtPositions(SparseBooleanArray positions) {
    for (int i = 0; i < positions.size(); i++) {
      if (positions.valueAt(i) && isSplitAtPosition(positions.keyAt(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * calls {@link ProtectedFragmentActivity#contribFeatureRequested(ContribFeature, Serializable)}
   * for feature {@link ContribFeature#SPLIT_TRANSACTION}
   *
   * @param tag if tag holds a single long the new instance will be edited, if tag holds an array of longs
   *            new instances will be immediately saved for each
   */
  public void requestSplitTransaction(Serializable tag) {
    ((ProtectedFragmentActivity) getActivity()).contribFeatureRequested(ContribFeature.SPLIT_TRANSACTION, tag);
  }

  public void dispatchCreateInstanceSaveDo(Long[] itemIds, Long[][] extra) {
    dispatchTask(TaskExecutionFragment.TASK_NEW_FROM_TEMPLATE, itemIds, extra);
  }

  public void dispatchTask(int taskId, Long[] itemIds, Long[][] extra) {
    ((ProtectedFragmentActivity) requireActivity()).startTaskExecution(
        taskId,
        itemIds,
        extra,
        0);
  }

  public void dispatchCreateInstanceEditDo(long itemId) {
    Intent intent = new Intent(requireActivity(), ExpenseEdit.class);
    intent.putExtra(KEY_TEMPLATEID, itemId);
    intent.putExtra(KEY_INSTANCEID, -1L);
    startActivity(intent);
  }

  public void dispatchCreateInstanceEdit(long templateId, long instanceId, long date) {
    Intent intent = new Intent(requireActivity(), ExpenseEdit.class);
    intent.putExtra(KEY_TEMPLATEID, templateId);
    intent.putExtra(KEY_INSTANCEID, instanceId);
    intent.putExtra(KEY_DATE, date);
    startActivity(intent);
  }

  public void dispatchEditInstance(Long transactionId) {
    Intent intent = new Intent(requireActivity(), ExpenseEdit.class);
    intent.putExtra(KEY_ROWID, transactionId);
    startActivityForResult(intent, EDIT_REQUEST);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == EDIT_REQUEST && resultCode == RESULT_OK) {
      PlannerFragment fragment = getPlannerFragment();
      if (fragment != null) {
        fragment.onEditRequestOk();
      }
    }
  }

  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    if (id == SORTABLE_CURSOR) {
      return new CursorLoader(getActivity(),
          TransactionProvider.TEMPLATES_URI.buildUpon()
              .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_PLAN_INFO, "1").build(),
          null,
          KEY_PARENTID + " is null",
          null,
          Sort.preferredOrderByForTemplatesWithPlans(prefHandler, Sort.USAGES));
    }
    return null;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
    final ManageTemplates ctx = (ManageTemplates) requireActivity();
    if (loader.getId() == SORTABLE_CURSOR) {
      mTemplatesCursor = c;
      if (c != null && !indexesCalculated) {
        columnIndexRowId = c.getColumnIndex(KEY_ROWID);
        columnIndexAmount = c.getColumnIndex(KEY_AMOUNT);
        columnIndexLabelSub = c.getColumnIndex(KEY_LABEL_SUB);
        columnIndexComment = c.getColumnIndex(KEY_COMMENT);
        columnIndexPayee = c.getColumnIndex(KEY_PAYEE_NAME);
        columnIndexColor = c.getColumnIndex(KEY_COLOR);
        columnIndexCurrency = c.getColumnIndex(KEY_CURRENCY);
        columnIndexTransferAccount = c.getColumnIndex(KEY_TRANSFER_ACCOUNT);
        columnIndexPlanId = c.getColumnIndex(KEY_PLANID);
        columnIndexTitle = c.getColumnIndex(KEY_TITLE);
        columnIndexPlanInfo = c.getColumnIndex(KEY_PLAN_INFO);
        columnIndexIsSealed = c.getColumnIndex(KEY_SEALED);
        columnIndexDefaultAction = c.getColumnIndex(KEY_DEFAULT_ACTION);
        indexesCalculated = true;
      }
      invalidateCAB();
      hasPlans = false;
      if (isCalendarPermissionGranted() &&
          mTemplatesCursor != null && mTemplatesCursor.moveToFirst()) {
        long needToExpand = expandedHandled ? ManageTemplates.NOT_CALLED :
            ctx.getCalledFromCalendarWithId();
        PlanMonthFragment planMonthFragment = null;
        while (!mTemplatesCursor.isAfterLast()) {
          long planId = mTemplatesCursor.getLong(columnIndexPlanId);
          if (planId != 0) {
            hasPlans = true;
          }
          long templateId = mTemplatesCursor.getLong(columnIndexRowId);
          if (needToExpand == templateId) {
            planMonthFragment = PlanMonthFragment.newInstance(
                mTemplatesCursor.getString(columnIndexTitle),
                templateId,
                planId,
                mTemplatesCursor.getInt(columnIndexColor), mTemplatesCursor.getInt(columnIndexIsSealed) != 0);
          }
          mTemplatesCursor.moveToNext();
        }
        if (needToExpand != ManageTemplates.NOT_CALLED) {
          expandedHandled = true;
          if (planMonthFragment != null) {
            planMonthFragment.show(getChildFragmentManager(), CALDROID_DIALOG_FRAGMENT_TAG);
          } else {
            ctx.showSnackbar(R.string.save_transaction_template_deleted);
          }
        }
        //look for plans that we could possible relink
        if (!repairTriggered && mTemplatesCursor.moveToFirst()) {
          final ArrayList<String> missingUuids = new ArrayList<>();
          while (!mTemplatesCursor.isAfterLast()) {
            if (!mTemplatesCursor.isNull(columnIndexPlanId) && mTemplatesCursor.isNull(columnIndexPlanInfo)) {
              missingUuids.add(mTemplatesCursor.getString(mTemplatesCursor.getColumnIndex(KEY_UUID)));
            }
            mTemplatesCursor.moveToNext();
          }
          if (missingUuids.size() > 0) {
            new RepairHandler(this).obtainMessage(
                0, missingUuids.toArray(new String[0]))
                .sendToTarget();
          }
        }
      }
      mAdapter.swapCursor(mTemplatesCursor);
      requireActivity().invalidateOptionsMenu();
    }
  }

  public void showSnackbar(String msg) {
    DialogFragment childFragment = getPlanMonthFragment();
    if (childFragment == null) {
      childFragment = getPlannerFragment();
    }
    if (childFragment != null) {
      showSnackbar(childFragment, msg);
    } else {
      ((ProtectedFragmentActivity) getActivity()).showSnackbar(msg);
    }
  }

  public void showSnackbar(DialogFragment dialogFragment, String msg) {
    final Dialog dialog = dialogFragment.getDialog();
    if (dialog != null) {
      final Window window = dialog.getWindow();
      if (window != null) {
        View view = window.getDecorView();
        Snackbar snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_LONG);
        UiUtils.increaseSnackbarMaxLines(snackbar);
        snackbar.show();
        return;
      }
      Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }
  }

  private static class RepairHandler extends Handler {
    private final WeakReference<TemplatesList> mFragment;

    public RepairHandler(TemplatesList fragment) {
      mFragment = new WeakReference<>(fragment);
    }

    @Override
    public void handleMessage(Message msg) {
      String[] missingUuids = (String[]) msg.obj;
      TemplatesList fragment = mFragment.get();
      if (fragment != null && fragment.getActivity() != null) {
        fragment.repairTriggered = true;
        ((ProtectedFragmentActivity) fragment.getActivity()).startTaskExecution(
            TaskExecutionFragment.TASK_REPAIR_PLAN,
            missingUuids,
            null,
            0);
      }
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    if (loader.getId() == SORTABLE_CURSOR) {
      mTemplatesCursor = null;
      mAdapter.swapCursor(null);
    }
  }

  @Override
  protected PrefKey getSortOrderPrefKey() {
    return PrefKey.SORT_ORDER_TEMPLATES;
  }


  //after orientation change, we need to restore the reference
  @Nullable
  private PlanMonthFragment getPlanMonthFragment() {
    return (PlanMonthFragment) getChildFragmentManager().findFragmentByTag(CALDROID_DIALOG_FRAGMENT_TAG);
  }

  @Nullable
  private PlannerFragment getPlannerFragment() {
    return (PlannerFragment) getChildFragmentManager().findFragmentByTag(PLANNER_FRAGMENT_TAG);
  }

  private class MyAdapter extends SimpleCursorAdapter {
    private final int colorExpense;
    private final int colorIncome;

    public MyAdapter(Context context, int layout, Cursor c, String[] from,
                     int[] to, int flags) {
      super(context, layout, c, from, to, flags);
      colorIncome = context.getResources().getColor(R.color.colorIncome);
      colorExpense = context.getResources().getColor(R.color.colorExpense);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      convertView = super.getView(position, convertView, parent);
      Cursor c = getCursor();
      c.moveToPosition(position);
      boolean isSealed = c.getInt(columnIndexIsSealed) != 0;
      boolean doesHavePlan = !c.isNull(columnIndexPlanId);
      TextView tv1 = convertView.findViewById(R.id.amount);
      long amount = c.getLong(columnIndexAmount);
      tv1.setTextColor(amount < 0 ? colorExpense : colorIncome);
      tv1.setText(currencyFormatter.convAmount(amount,
          currencyContext.get(c.getString(columnIndexCurrency))));
      int color = c.getInt(columnIndexColor);
      convertView.findViewById(R.id.colorAccount).setBackgroundColor(color);
      TextView tv2 = convertView.findViewById(R.id.category);
      CharSequence catText = tv2.getText();
      if (!c.isNull(columnIndexTransferAccount)) {
        catText = Transfer.getIndicatorPrefixForLabel(amount) + catText;
      } else {
        Long catId = DbUtils.getLongOrNull(c, KEY_CATID);
        if (catId == null) {
          catText = Category.NO_CATEGORY_ASSIGNED_LABEL;
        } else {
          String label_sub = c.getString(columnIndexLabelSub);
          if (label_sub != null && label_sub.length() > 0) {
            String categorySeparator = " : ";
            catText = catText + categorySeparator + label_sub;
          }
        }
      }
      //TODO: simplify confer TemplateWidget
      SpannableStringBuilder ssb;
      String comment = c.getString(columnIndexComment);
      String commentSeparator = " / ";
      if (comment != null && comment.length() > 0) {
        ssb = new SpannableStringBuilder(comment);
        ssb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 0, comment.length(), 0);
        catText = TextUtils.concat(catText, commentSeparator, ssb);
      }
      String payee = c.getString(columnIndexPayee);
      if (payee != null && payee.length() > 0) {
        ssb = new SpannableStringBuilder(payee);
        ssb.setSpan(new UnderlineSpan(), 0, payee.length(), 0);
        catText = TextUtils.concat(catText, commentSeparator, ssb);
      }
      tv2.setText(catText);

      if (doesHavePlan) {
        CharSequence planInfo = columnIndexPlanInfo == -1 ? null : c.getString(columnIndexPlanInfo);
        if (planInfo == null) {
          if (isCalendarPermissionGranted()) {
            planInfo = getString(R.string.plan_event_deleted);
          } else {
            planInfo = Utils.getTextWithAppName(getContext(), R.string.calendar_permission_required);
          }
        }
        ((TextView) convertView.findViewById(R.id.title)).setText(
            //noinspection SetTextI18n
            c.getString(columnIndexTitle) + " (" + planInfo + ")");
      }
      ImageView planImage = convertView.findViewById(R.id.Plan);
      planImage.setImageResource(
          isSealed ? R.drawable.ic_lock : (doesHavePlan ? R.drawable.ic_event : R.drawable.ic_menu_template));
      planImage.setContentDescription(getString(doesHavePlan ?
          R.string.plan : R.string.template));
      return convertView;
    }
  }

  @Override
  protected void configureMenu(@NonNull Menu menu, @NonNull AbsListView lv) {
    super.configureMenu(menu, lv);
    int id = lv.getId();
    if (id == R.id.list) {
      SparseBooleanArray checkedItemPositions = mListView.getCheckedItemPositions();
      boolean hasForeignExchangeTransfer = false, hasPlan = false, hasSealed = false;
      for (int i = 0; i < checkedItemPositions.size(); i++) {
        if (checkedItemPositions.valueAt(i) && isForeignExchangeTransfer(checkedItemPositions.keyAt
            (i))) {
          hasForeignExchangeTransfer = true;
          break;
        }
      }
      for (int i = 0; i < checkedItemPositions.size(); i++) {
        if (checkedItemPositions.valueAt(i) && isPlan(checkedItemPositions.keyAt
            (i))) {
          hasPlan = true;
          break;
        }
      }
      for (int i = 0; i < checkedItemPositions.size(); i++) {
        if (checkedItemPositions.valueAt(i) && isSealed(checkedItemPositions.keyAt
            (i))) {
          hasSealed = true;
          break;
        }
      }
      configureMenuInternal(menu, lv.getCheckedItemCount(), hasForeignExchangeTransfer, hasPlan, hasSealed);
    } else if (id == R.id.calendar_gridview) {
      final PlanMonthFragment planMonthFragment = getPlanMonthFragment();
      if (planMonthFragment != null) {
        planMonthFragment.configureMenu11(menu, lv.getCheckedItemCount(), lv);
      }
    }
  }

  private void configureMenuInternal(Menu menu, int count, boolean foreignExchangeTransfer, boolean hasPlan, boolean hasSealed) {
    menu.findItem(R.id.CREATE_INSTANCE_SAVE_COMMAND).setVisible(!foreignExchangeTransfer && !hasPlan & !hasSealed);
    menu.findItem(R.id.CREATE_INSTANCE_EDIT_COMMAND).setVisible(count == 1 && !hasPlan && !hasSealed);
    menu.findItem(R.id.DEFAULT_ACTION_MENU).setVisible(!hasPlan);
    menu.findItem(R.id.EDIT_COMMAND).setVisible(count == 1 && !hasSealed);
  }

  private boolean isForeignExchangeTransfer(int position) {
    if (mTemplatesCursor != null && mTemplatesCursor.moveToPosition(position)) {
      if (!mTemplatesCursor.isNull(columnIndexTransferAccount)) {
        Account transferAccount = Account.getInstanceFromDb(
            mTemplatesCursor.getLong(columnIndexTransferAccount));
        return !mTemplatesCursor.getString(columnIndexCurrency).equals(
            transferAccount.getCurrencyUnit().getCode());
      }
    }
    return false;
  }

  private boolean isPlan(int position) {
    if (mTemplatesCursor != null && mTemplatesCursor.moveToPosition(position)) {
      return !mTemplatesCursor.isNull(columnIndexPlanId);
    }
    return false;
  }

  private boolean isSealed(int position) {
    if (mTemplatesCursor != null && mTemplatesCursor.moveToPosition(position)) {
      return mTemplatesCursor.getInt(columnIndexIsSealed) == 1;
    }
    return false;
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.templates, menu);
  }

  @Override
  public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    MenuItem menuItem = menu.findItem(R.id.PLANNER_COMMAND);
    if (menuItem != null) {
      menuItemSetEnabledAndVisible(menuItem, hasPlans);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.PLANNER_COMMAND) {
      new PlannerFragment().show(getChildFragmentManager(), PLANNER_FRAGMENT_TAG);
      return true;
    }
    return handleSortOption(item);
  }

  @Override
  protected void inflateContextualActionBar(Menu menu, int listId) {
    if (listId == R.id.list) {
      super.inflateContextualActionBar(menu, listId);
    } else if (listId == R.id.calendar_gridview) {
      getActivity().getMenuInflater().inflate(R.menu.planlist_context, menu);
    }
  }

  public void loadData() {
    Utils.requireLoader(mManager, SORTABLE_CURSOR, null, this);
  }
}
