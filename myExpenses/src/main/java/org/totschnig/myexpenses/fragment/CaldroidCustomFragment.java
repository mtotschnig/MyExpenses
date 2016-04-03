package org.totschnig.myexpenses.fragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidGridAdapter;
import com.roomorama.caldroid.CellView;

import org.totschnig.myexpenses.R;

import java.util.Map;

/**
 * Created by michaeltotschnig on 04.04.16.
 */
public class CaldroidCustomFragment extends CaldroidFragment {

  @Override
  public CaldroidGridAdapter getNewDatesGridAdapter(int month, int year) {
    return new CaldroidSampleCustomAdapter(getActivity(), month, year, getCaldroidData(), extraData);
  }

  private class CaldroidSampleCustomAdapter extends CaldroidGridAdapter {

    public CaldroidSampleCustomAdapter(Context context, int month, int year,
                                       Map<String, Object> caldroidData,
                                       Map<String, Object> extraData) {
      super(context, month, year, caldroidData, extraData);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      LayoutInflater inflater = (LayoutInflater) context
          .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View framelayout;

      // For reuse
      if (convertView == null) {
        framelayout = inflater.inflate(R.layout.plan_calendar_cell, null);
      } else {
        framelayout = convertView;
      }

      CellView cell = (CellView) framelayout.findViewById(R.id.cell);
      ImageView state = (ImageView) framelayout.findViewById(R.id.state);

      customizeTextView(position, cell);

      state.setImageResource(R.drawable.ic_stat_applied);

      return framelayout;
    }
  }
}
