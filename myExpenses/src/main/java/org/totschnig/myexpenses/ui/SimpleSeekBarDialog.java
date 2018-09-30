package org.totschnig.myexpenses.ui;

import android.os.Bundle;
import android.view.View;

import com.pavelsikun.seekbarpreference.SeekBarPreferenceView;

import eltos.simpledialogfragment.CustomViewDialog;

public class SimpleSeekBarDialog extends CustomViewDialog<SimpleSeekBarDialog> {
  public static final String TAG = "SimpleSeekBarDialog.";

  protected static final String SEEKBAR_MINIMUM = "Seekbar.maximum";
  protected static final String SEEKBAR_MAXIMUM = "Seekbar.minimum";
  public static final String SEEKBAR_VALUE = "Seekbar.value";
  private SeekBarPreferenceView seekBarPreferenceView;

  public static SimpleSeekBarDialog build() {
    return new SimpleSeekBarDialog();
  }

  /**
   * Sets the seeekbar's maximum range
   *
   * @param max the maximum range
   */
  public SimpleSeekBarDialog max(int max) {
    return setArg(SEEKBAR_MAXIMUM, max);
  }

  /**
   * Sets the seeekbar's mimimum range
   *
   * @param min the maximum range
   */
  public SimpleSeekBarDialog min(int min) {
    return setArg(SEEKBAR_MINIMUM, min);
  }

  /**
   * Sets the seeekbar's current value
   *
   * @param value the maximum range
   */
  public SimpleSeekBarDialog value(int value) {
    return setArg(SEEKBAR_VALUE, value);
  }

  @Override
  public View onCreateContentView(Bundle savedInstanceState) {
    seekBarPreferenceView = new SeekBarPreferenceView(getContext());
    seekBarPreferenceView.setMinValue(getArguments().getInt(SEEKBAR_MINIMUM));
    seekBarPreferenceView.setMaxValue(getArguments().getInt(SEEKBAR_MAXIMUM));
    restoreValue(savedInstanceState != null ? savedInstanceState : getArguments());
    return seekBarPreferenceView;
  }

  @Override
  public Bundle onResult(int which) {
    Bundle result = new Bundle();
    storeValue(result);
    return result;
  }

  private void restoreValue(Bundle bundle) {
    seekBarPreferenceView.setCurrentValue(bundle.getInt(SEEKBAR_VALUE));
  }

  private void storeValue(Bundle bundle) {
    bundle.putInt(SEEKBAR_VALUE, seekBarPreferenceView.getCurrentValue());
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    storeValue(outState);
  }
}