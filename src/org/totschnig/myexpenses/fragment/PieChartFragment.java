package org.totschnig.myexpenses.fragment;

import java.util.ArrayList;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.CommitSafeDialogFragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
    mChart.setUsePercentValues(true);
    mChart.setCenterText("Quarterly\nRevenue");
    mChart.setCenterTextSize(22f);
     
    // radius of the center hole in percent of maximum radius
    mChart.setHoleRadius(45f); 
    mChart.setTransparentCircleRadius(50f);
    
    // enable / disable drawing of x- and y-values
  //  mChart.setDrawYValues(false);
  //  mChart.setDrawXValues(false);
    return v;
  }
  public void setData() {
    mChart.setData(generatePieData());
  }
  protected PieData generatePieData() {
    
    int count = 4;
    
    ArrayList<Entry> entries1 = new ArrayList<Entry>();
    ArrayList<String> xVals = new ArrayList<String>();
    
    xVals.add("Quarter 1");
    xVals.add("Quarter 2");
    xVals.add("Quarter 3");
    xVals.add("Quarter 4");
    
    for(int i = 0; i < count; i++) {
        xVals.add("entry" + (i+1));

        entries1.add(new Entry((float) (Math.random() * 60) + 40, i));
    }
    
    PieDataSet ds1 = new PieDataSet(entries1, "Quarterly Revenues 2014");
    ds1.setColors(ColorTemplate.VORDIPLOM_COLORS);
    ds1.setSliceSpace(2f);
    
    PieData d = new PieData(xVals, ds1);
    return d;
  }
}
