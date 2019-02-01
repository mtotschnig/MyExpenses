package android.support.v7.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.TextView;

public class LiveSeekBarPreference extends SeekBarPreference {
  public LiveSeekBarPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder view) {
    final TextView valueTextView = (TextView) view.findViewById(R.id.seekbar_value);
    super.onBindViewHolder(view);
    mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser)
          if (!mTrackingTouch) {
            syncValueInternal(seekBar);
          } else {
            valueTextView.setText(String.valueOf(progress + mMin));
          }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = true;
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = false;
        if (seekBar.getProgress() + mMin != mSeekBarValue) {
          syncValueInternal(seekBar);
        }
      }
    });
  }
}
