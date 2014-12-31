package org.totschnig.myexpenses.fragment;

import java.util.ArrayList;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.CommitSafeDialogFragment;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.interfaces.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class PieChartFragment extends Fragment {  
  private PieChart mChart;

  @Override
  public View onCreateView(LayoutInflater inflater,
      @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.piechart, null, false);
    mChart = (PieChart) v.findViewById(R.id.chart1);
    mChart.setDescription("");
    
    //Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "OpenSans-Regular.ttf");
    
    //mChart.setValueTypeface(tf);
    //mChart.setCenterTextTypeface(Typeface.createFromAsset(getActivity().getAssets(), "OpenSans-Light.ttf"));
    //mChart.setUsePercentValues(true);
    //mChart.setCenterText("Quarterly\nRevenue");
    //mChart.setCenterTextSize(22f);
     
    // radius of the center hole in percent of maximum radius
    mChart.setHoleRadius(0f); 
    mChart.setTransparentCircleRadius(0f);
    mChart.setDrawLegend(false);
    mChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {

      @Override
      public void onValueSelected(Entry e, int dataSetIndex) {
        Toast.makeText(getActivity(),"Selected index " + e.getXIndex(),Toast.LENGTH_LONG).show();
      }

      @Override
      public void onNothingSelected() {
        Toast.makeText(getActivity(),"Unselected",Toast.LENGTH_LONG).show();
      }
    });
    
    // enable / disable drawing of x- and y-values
  //  mChart.setDrawYValues(false);
  //  mChart.setDrawXValues(false);
    return v;
  }
  public void setData(Cursor c) {
    ArrayList<Entry> entries1 = new ArrayList<Entry>();
    ArrayList<String> xVals = new ArrayList<String>();
    if (c!= null && c.moveToFirst()) {
      do {
        xVals.add(c.getString(c.getColumnIndex(DatabaseConstants.KEY_LABEL)));
        entries1.add(
            new Entry(
                (float) c.getLong(c.getColumnIndex(DatabaseConstants.KEY_SUM)),
                c.getPosition()));
      } while (c.moveToNext());
      PieDataSet ds1 = new PieDataSet(entries1, "");
      ds1.setColors(ColorTemplate.PASTEL_COLORS);
      ds1.setSliceSpace(0f);
      mChart.setData(new PieData(xVals, ds1));
      // undo all highlights
      mChart.highlightValues(null);
      mChart.invalidate();
    } else {
      mChart.clear();
    }
  }
}
