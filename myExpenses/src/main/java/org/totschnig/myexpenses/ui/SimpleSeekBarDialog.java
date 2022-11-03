package org.totschnig.myexpenses.ui;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.slider.Slider;

import eltos.simpledialogfragment.CustomViewDialog;

public class SimpleSeekBarDialog extends CustomViewDialog<SimpleSeekBarDialog> {
  public static final String TAG = "SimpleSeekBarDialog.";

  protected static final String SEEKBAR_MINIMUM = "Seekbar.maximum";
  protected static final String SEEKBAR_MAXIMUM = "Seekbar.minimum";
  public static final String SEEKBAR_VALUE = "Seekbar.value";
  private Slider slider;

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
    slider = new Slider(getContext());
    slider.setValueFrom(getArguments().getInt(SEEKBAR_MINIMUM));
    slider.setValueTo(getArguments().getInt(SEEKBAR_MAXIMUM));
    slider.setStepSize(1F);
    restoreValue(savedInstanceState != null ? savedInstanceState : getArguments());
    return slider;
  }

  @Override
  public Bundle onResult(int which) {
    Bundle result = new Bundle();
    storeValue(result);
    return result;
  }

  private void restoreValue(Bundle bundle) {
    slider.setValue(bundle.getInt(SEEKBAR_VALUE));
  }

  private void storeValue(Bundle bundle) {
    bundle.putInt(SEEKBAR_VALUE, (int) slider.getValue());
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    storeValue(outState);
  }
}