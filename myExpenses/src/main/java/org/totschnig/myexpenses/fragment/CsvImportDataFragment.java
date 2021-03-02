package org.totschnig.myexpenses.fragment;

import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.commons.csv.CSVRecord;
import org.json.JSONException;
import org.json.JSONObject;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.CsvImportActivity;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.SparseBooleanArrayParcelable;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.ASYNC_TAG;
import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.PROGRESS_TAG;

/**
 * Created by privat on 30.06.15.
 */
public class CsvImportDataFragment extends Fragment {
  public static final String KEY_DATASET = "DATASET";
  public static final String KEY_DISCARDED_ROWS = "DISCARDED_ROWS";
  public static final String KEY_COLUMN_TO_FIELD = "COLUMN_TO_FIELD";
  public static final String KEY_FIELD_TO_COLUMN = "FIELD_TO_COLUMN";
  public static final String KEY_FIRST_LINE_IS_HEADER = "FIRST_LINE_IS_HEADER";

  public static final int CELL_MIN_WIDTH = 100;
  public static final int CHECKBOX_COLUMN_WIDTH = 60;
  public static final int CELL_MARGIN = 5;
  private RecyclerView mRecyclerView;
  private LinearLayout mHeaderLine;
  private RecyclerView.Adapter mAdapter;
  private RecyclerView.LayoutManager mLayoutManager;
  private ArrayList<CSVRecord> mDataset;
  private SparseBooleanArrayParcelable discardedRows;

  private ArrayAdapter<Integer> mFieldAdapter;
  private LinearLayout.LayoutParams cellParams, cbParams;
  private boolean firstLineIsHeader;

  private int nrOfColumns;

  public static final String FIELD_KEY_DISCARD = "DISCARD";
  public static final String FIELD_KEY_AMOUNT = "AMOUNT";
  public static final String FIELD_KEY_EXPENSE = "EXPENSE";
  public static final String FIELD_KEY_INCOME = "INCOME";
  public static final String FIELD_KEY_DATE = "DATE";
  public static final String FIELD_KEY_PAYEE = "PAYEE";
  public static final String FIELD_KEY_COMMENT = "COMMENT";
  public static final String FIELD_KEY_CATEGORY = "CATEGORY";
  public static final String FIELD_KEY_SUBCATEGORY = "SUBACTEGORY";
  public static final String FIELD_KEY_METHOD = "METHOD";
  public static final String FIELD_KEY_STATUS = "STATUS";
  public static final String FIELD_KEY_NUMBER = "NUMBER";
  public static final String FIELD_KEY_SPLIT = "SPLIT";


  private final String[] fieldKeys = new String[]{
      FIELD_KEY_DISCARD, FIELD_KEY_AMOUNT, FIELD_KEY_EXPENSE, FIELD_KEY_INCOME,
      FIELD_KEY_DATE, FIELD_KEY_PAYEE, FIELD_KEY_COMMENT, FIELD_KEY_CATEGORY,
      FIELD_KEY_SUBCATEGORY, FIELD_KEY_METHOD, FIELD_KEY_STATUS, FIELD_KEY_NUMBER,
      FIELD_KEY_SPLIT
  };
  private final Integer[] fields = new Integer[]{
      R.string.cvs_import_discard,
      R.string.amount,
      R.string.expense,
      R.string.income,
      R.string.date,
      R.string.payer_or_payee,
      R.string.comment,
      R.string.category,
      R.string.subcategory,
      R.string.method,
      R.string.status,
      R.string.reference_number,
      R.string.split_transaction
  };
  private JSONObject header2FieldMap;
  private float windowWidth;

  public static CsvImportDataFragment newInstance() {
    return new CsvImportDataFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

    windowWidth = displayMetrics.widthPixels / displayMetrics.density;

    String header2FieldMapJson = PrefKey.CSV_IMPORT_HEADER_TO_FIELD_MAP.getString(null);
    if (header2FieldMapJson != null) {
      try {
        header2FieldMap = new JSONObject(header2FieldMapJson);
      } catch (JSONException e) {
        header2FieldMap = new JSONObject();
      }
    } else {
      header2FieldMap = new JSONObject();
    }

    cbParams = new LinearLayout.LayoutParams(UiUtils.dp2Px(CHECKBOX_COLUMN_WIDTH, getResources()),
        LinearLayout.LayoutParams.WRAP_CONTENT);
    cbParams.setMargins(CELL_MARGIN, CELL_MARGIN, CELL_MARGIN, CELL_MARGIN);

    mFieldAdapter = new ArrayAdapter<Integer>(
        getActivity(), android.R.layout.simple_spinner_item, fields) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) super.getView(position, convertView, parent);
        tv.setText(getString(fields[position]));
        return tv;
      }

      @Override
      public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
        tv.setText(getString(fields[position]));
        return tv;
      }
    };
    mFieldAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
    View view = inflater.inflate(R.layout.import_csv_data, container, false);
    mRecyclerView = view.findViewById(R.id.my_recycler_view);
    mHeaderLine = view.findViewById(R.id.header_line);

    // use this setting to improve performance if you know that changes
    // in content do not change the layout size of the RecyclerView
    // http://www.vogella.com/tutorials/AndroidRecyclerView/article.html
    mRecyclerView.setHasFixedSize(true);

    // use a linear layout manager
    mLayoutManager = new LinearLayoutManager(getActivity());
    mRecyclerView.setLayoutManager(mLayoutManager);
    if (savedInstanceState != null) {
      setData((ArrayList<CSVRecord>) savedInstanceState.getSerializable(KEY_DATASET));
      discardedRows = savedInstanceState.getParcelable(KEY_DISCARDED_ROWS);
      firstLineIsHeader = savedInstanceState.getBoolean(KEY_FIRST_LINE_IS_HEADER);
    }

    return view;
  }

  public void setData(ArrayList<CSVRecord> data) {
    if (data == null || data.isEmpty()) return;
    mDataset = data;
    int nrOfColumns = mDataset.get(0).size();
    discardedRows = new SparseBooleanArrayParcelable();

    int availableCellWidth =
        (int) ((windowWidth - CHECKBOX_COLUMN_WIDTH - CELL_MARGIN * (nrOfColumns + 2)) / nrOfColumns);
    int cellWidth, tableWidth;
    if (availableCellWidth > CELL_MIN_WIDTH) {
      cellWidth = availableCellWidth;
      tableWidth = (int) windowWidth;
    } else {
      cellWidth = CELL_MIN_WIDTH;
      tableWidth = CELL_MIN_WIDTH * nrOfColumns + CHECKBOX_COLUMN_WIDTH + CELL_MARGIN * (nrOfColumns + 2);
    }

    cellParams = new LinearLayout.LayoutParams(UiUtils.dp2Px(cellWidth, getResources()),
        LinearLayout.LayoutParams.WRAP_CONTENT);
    cellParams.setMargins(CELL_MARGIN, CELL_MARGIN, CELL_MARGIN, CELL_MARGIN);

    ViewGroup.LayoutParams params = mRecyclerView.getLayoutParams();
    params.width = UiUtils.dp2Px(tableWidth, getResources());
    mRecyclerView.setLayoutParams(params);
    mAdapter = new MyAdapter();
    mRecyclerView.setAdapter(mAdapter);
    //set up header
    mHeaderLine.removeViews(1, mHeaderLine.getChildCount() - 1);
    for (int i = 0; i < nrOfColumns; i++) {
      Spinner cell = new Spinner(getActivity());
      cell.setId(i);
      cell.setAdapter(mFieldAdapter);
      mHeaderLine.addView(cell, cellParams);
    }
  }

  public void setHeader() {
    firstLineIsHeader = true;
    mAdapter.notifyItemChanged(0);
    //automap
    final CSVRecord record = mDataset.get(0);

    outer:
    for (int j = 0; j < record.size(); j++) {
      String headerLabel = Utils.normalize(record.get(j));
      Iterator<String> keys = header2FieldMap.keys();
      while (keys.hasNext()) {
        String storedLabel = keys.next();
        if (storedLabel.equals(headerLabel)) {
          try {
            String fieldKey = header2FieldMap.getString(storedLabel);
            int position = Arrays.asList(fieldKeys).indexOf(fieldKey);
            if (position != -1) {
              ((Spinner) mHeaderLine.getChildAt(j + 1)).setSelection(position);
              continue outer;
            }
          } catch (JSONException e) {
            //ignore
          }
        }
      }
      for (int i = 1 /* 0=Discard ignored  */; i < fields.length; i++) {
        String fieldLabel = Utils.normalize(getString(fields[i]));
        if (fieldLabel.equals(headerLabel)) {
          ((Spinner) mHeaderLine.getChildAt(j + 1)).setSelection(i);
          break;
        }
      }
    }
  }

  private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> implements
      CompoundButton.OnCheckedChangeListener {

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
      int position = (int) buttonView.getTag();
      Timber.d("%s item at position %d", isChecked ? "Discarding" : "Including", position);
      if (isChecked) {
        discardedRows.put(position, true);
        if (position == 0) {
          Bundle b = new Bundle();
          b.putInt(ConfirmationDialogFragment.KEY_TITLE,
              R.string.dialog_title_information);
          b.putString(
              ConfirmationDialogFragment.KEY_MESSAGE,
              getString(R.string.cvs_import_set_first_line_as_header));
          b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
              R.id.SET_HEADER_COMMAND);
          ConfirmationDialogFragment.newInstance(b).show(
              getParentFragmentManager(), "SET_HEADER_CONFIRMATION");
        }
      } else {
        discardedRows.delete(position);
        if (position == 0) {
          firstLineIsHeader = false;
        }
      }
      notifyItemChanged(position);
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
      // each data item is just a string in this case
      public LinearLayout row;

      public ViewHolder(LinearLayout v) {
        super(v);
        row = v;
      }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    MyAdapter() {
      nrOfColumns = mDataset.get(0).size();
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                   int viewType) {
      // create a new view
      LinearLayout v = new LinearLayout(parent.getContext());
      v.setBackgroundResource(R.drawable.csv_import_row_background);
      TextView cell = new CheckBox(parent.getContext());

      v.addView(cell, cbParams);
      for (int i = 0; i < nrOfColumns; i++) {
        cell = new TextView(parent.getContext());
        cell.setSingleLine();
        cell.setEllipsize(TextUtils.TruncateAt.END);
        cell.setSelected(true);
        cell.setOnClickListener(v1 -> ((ProtectedFragmentActivity) getActivity()).showSnackbar(((TextView) v1).getText()));
        if (viewType == 0) {
          cell.setTypeface(null, Typeface.BOLD);
        }
        v.addView(cell, cellParams);
      }
      // set the view's size, margins, paddings and layout parameters
      ViewHolder vh = new ViewHolder(v);
      return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
      // - get element from your dataset at this position
      // - replace the contents of the view with that element
      boolean isDiscarded = discardedRows.get(position, false);
      boolean isHeader = position == 0 && firstLineIsHeader;
      holder.row.setActivated(isDiscarded && !isHeader);
      final CSVRecord record = mDataset.get(position);
      for (int i = 0; i < record.size() && i < nrOfColumns; i++) {
        TextView cell = (TextView) holder.row.getChildAt(i + 1);
        cell.setText(record.get(i));
      }
      CheckBox cb = (CheckBox) holder.row.getChildAt(0);
      cb.setTag(position);
      cb.setOnCheckedChangeListener(null);
      cb.setChecked(isDiscarded);
      cb.setOnCheckedChangeListener(this);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
      return mDataset.size();
    }

    @Override
    public int getItemViewType(int position) {
      return position == 0 & firstLineIsHeader ? 0 : 1;
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(KEY_DATASET, mDataset);
    outState.putParcelable(KEY_DISCARDED_ROWS, discardedRows);
    outState.putBoolean(KEY_FIRST_LINE_IS_HEADER, firstLineIsHeader);
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.csv_import, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.IMPORT_COMMAND) {
      int[] columnToFieldMap = new int[nrOfColumns];
      final CSVRecord header = mDataset.get(0);
      for (int i = 0; i < nrOfColumns; i++) {
        int position = ((Spinner) mHeaderLine.getChildAt(i + 1)).getSelectedItemPosition();
        columnToFieldMap[i] = fields[position];
        if (firstLineIsHeader) {
          try {
            if (!fieldKeys[position].equals(FIELD_KEY_DISCARD)) {
              header2FieldMap.put(Utils.normalize(header.get(i)), fieldKeys[position]);
            }
          } catch (JSONException e) {
            CrashHandler.report(e);
          }
        }
      }
      if (validateMapping(columnToFieldMap)) {
        PrefKey.CSV_IMPORT_HEADER_TO_FIELD_MAP.putString(header2FieldMap.toString());
        long accountId = ((CsvImportActivity) getActivity()).getAccountId();
        CurrencyUnit currency = ((CsvImportActivity) getActivity()).getCurrency();
        QifDateFormat format = ((CsvImportActivity) getActivity()).getDateFormat();
        AccountType type = ((CsvImportActivity) getActivity()).getAccountType();
        TaskExecutionFragment taskExecutionFragment =
            TaskExecutionFragment.newInstanceCSVImport(
                mDataset, columnToFieldMap, discardedRows, format, accountId, currency, type);
        ProgressDialogFragment progressDialogFragment = ProgressDialogFragment.newInstance(
            getString(R.string.pref_import_title, "CSV"),
            null, ProgressDialog.STYLE_HORIZONTAL, false);
        progressDialogFragment.setMax(mDataset.size() - discardedRows.size());
        getParentFragmentManager()
            .beginTransaction()
            .add(taskExecutionFragment, ASYNC_TAG)
            .add(progressDialogFragment, PROGRESS_TAG)
            .commit();

      }
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * Check mapping constraints:<br>
   * <ul>
   * <li>No field mapped more than once</li>
   * <li>Subcategory cannot be mapped withoug category</li>
   * <li>One of amount, income or expense must be mapped.</li>
   * </ul>
   *
   * @param columnToFieldMap
   */
  private boolean validateMapping(int[] columnToFieldMap) {
    SparseBooleanArray foundFields = new SparseBooleanArray();
    ProtectedFragmentActivity activity = (ProtectedFragmentActivity) getActivity();
    for (int field : columnToFieldMap) {
      if (field != R.string.cvs_import_discard) {
        if (foundFields.get(field, false)) {
          activity.showSnackbar(getString(R.string.csv_import_field_mapped_more_than_once, getString(field)));
          return false;
        }
        foundFields.put(field, true);
      }
    }
    if (foundFields.get(R.string.subcategory, false) && !foundFields.get(R.string.category, false)) {
      activity.showSnackbar(R.string.csv_import_subcategory_requires_category);
      return false;
    }
    if (!(foundFields.get(R.string.amount, false) ||
        foundFields.get(R.string.expense, false) ||
        foundFields.get(R.string.income, false))) {
      activity.showSnackbar(R.string.csv_import_no_mapping_found_for_amount);
      return false;
    }
    return true;
  }
}
