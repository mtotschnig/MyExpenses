package org.totschnig.myexpenses.ui;

import android.graphics.RectF;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarLineScatterCandleBubbleData;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.highlight.BarHighlighter;
import com.github.mikephil.charting.highlight.ChartHighlighter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.interfaces.dataprovider.CombinedDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.utils.MPPointD;

import java.util.List;

import androidx.annotation.NonNull;

public class ExactStackedBarHighlighter extends BarHighlighter {

  private RectF barBoundsBuffer = new RectF();

  public ExactStackedBarHighlighter(BarDataProvider chart) {
    super(chart);
  }

  @Override
  public Highlight getHighlight(float x, float y) {

    MPPointD pos = getValsForTouch(x, y);
    float xVal = (float) pos.x;
    MPPointD.recycleInstance(pos);

    Highlight high = getHighlightForX(xVal, x, y);
    return high;
  }

  @Override
  protected Highlight getHighlightForX(float xVal, float x, float y) {
    IBarDataSet barDataSet = mChart.getBarData().getDataSetByIndex(0);
    BarEntry barEntry = barDataSet.getEntryForXValue(xVal, Float.NaN);
    for (int i = 0; i < barEntry.getYVals().length; i++) {
      getBarBounds(barEntry, i);
      if (barBoundsBuffer.contains(x, y)) {
        float yVal = barEntry.getYVals()[i];
        MPPointD pixels = mChart.getTransformer(
            barDataSet.getAxisDependency()).getPixelForValues(xVal, yVal);
        return new Highlight(
            barEntry.getX(), barEntry.getY(),
            (float) pixels.x, (float) pixels.y,
            0, i, barDataSet.getAxisDependency());
      }
    }
    return null;
  }

  public void getBarBounds(BarEntry e, int stackIndex) {

    BarData barData = mChart.getBarData();
    IBarDataSet set = barData.getDataSetForEntry(e);

    float y = stackIndex == -1 ? e.getY() : e.getYVals()[stackIndex];
    float x = e.getX();

    float barWidth = barData.getBarWidth();

    float left = x - barWidth / 2f;
    float right = x + barWidth / 2f;
    float top = y >= 0 ? y : 0;
    float bottom = y <= 0 ? y : 0;

    barBoundsBuffer.set(left, top, right, bottom);

    mChart.getTransformer(set.getAxisDependency()).rectValueToPixel(barBoundsBuffer);
  }

  public static class CombinedHighlighter extends ChartHighlighter<CombinedDataProvider> {

    @NonNull
    private BarHighlighter barHighlighter;

    public CombinedHighlighter(CombinedDataProvider chart) {
      super(chart);
      barHighlighter = new ExactStackedBarHighlighter(chart);
    }

    @Override
    protected List<Highlight> getHighlightsAtXValue(float xVal, float x, float y) {

      mHighlightBuffer.clear();

      //if we have a match on a bar, we are only interested in this match
      Highlight high = barHighlighter.getHighlight(x, y);

      if (high != null) {
        high.setDataIndex(mChart.getCombinedData().getDataIndex(mChart.getBarData()));
        mHighlightBuffer.add(high);
      } else {

        List<BarLineScatterCandleBubbleData> dataObjects = mChart.getCombinedData().getAllData();

        for (int i = 0; i < dataObjects.size(); i++) {

          ChartData dataObject = dataObjects.get(i);

          if (dataObject instanceof BarData) continue;

          for (int j = 0, dataSetCount = dataObject.getDataSetCount(); j < dataSetCount; j++) {
            IDataSet dataSet = dataObjects.get(i).getDataSetByIndex(j);

            // don't include datasets that cannot be highlighted
            if (!dataSet.isHighlightEnabled()) continue;

            List<Highlight> highs = buildHighlights(dataSet, j, xVal, DataSet.Rounding.CLOSEST);
            for (Highlight highlight : highs) {
              highlight.setDataIndex(i);
              mHighlightBuffer.add(highlight);
            }
          }
        }
      }

      return mHighlightBuffer;
    }
  }
}
