package org.totschnig.myexpenses.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.csv.CSVRecord;
import org.totschnig.myexpenses.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by privat on 30.06.15.
 */
public class CsvImportDataFragment extends Fragment {
  public static final String KEY_DATASET = "KEY_DATASET";
  private RecyclerView mRecyclerView;
  private RecyclerView.Adapter mAdapter;
  private RecyclerView.LayoutManager mLayoutManager;
  private ArrayList<CSVRecord> mDataset;

  public static CsvImportDataFragment newInstance() {
    return new CsvImportDataFragment();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.import_csv_data, container, false);
    mRecyclerView = (RecyclerView) view.findViewById(R.id.my_recycler_view);

    // use this setting to improve performance if you know that changes
    // in content do not change the layout size of the RecyclerView
    // http://www.vogella.com/tutorials/AndroidRecyclerView/article.html
    mRecyclerView.setHasFixedSize(true);

    // use a linear layout manager
    mLayoutManager = new LinearLayoutManager(getActivity());
    mRecyclerView.setLayoutManager(mLayoutManager);
    if (savedInstanceState!=null) {
      setData((ArrayList<CSVRecord>) savedInstanceState.getSerializable(KEY_DATASET));
    }

    return view;
  }

  public void setData(ArrayList<CSVRecord> data) {
    mDataset = data;
    mAdapter = new MyAdapter();
    mRecyclerView.setAdapter(mAdapter);
  }
  private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private int nrOfColumns;

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
    public MyAdapter() {
      nrOfColumns = mDataset.get(0).size();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
      // create a new view
      LinearLayout v = new LinearLayout(parent.getContext());
      for (int i = 0; i < nrOfColumns; i++) {
        v.addView(new TextView(parent.getContext()));
      }
      // set the view's size, margins, paddings and layout parameters
      ViewHolder vh = new ViewHolder(v);
      return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
      // - get element from your dataset at this position
      // - replace the contents of the view with that element
      final CSVRecord record = mDataset.get(position);
      for (int i = 0; i < nrOfColumns; i++) {
        ((TextView) holder.row.getChildAt(i)).setText(record.get(i));
      }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
      return mDataset.size();
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(KEY_DATASET, mDataset);
  }
}
