package org.totschnig.myexpenses.ui;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

/**
 * Spinner Helper class that works around some common issues
 * with the stock Android Spinner
 *
 * A Spinner will normally call it's OnItemSelectedListener
 * when you use setSelection(...) in your initialization code.
 * This is usually unwanted behavior, and a common work-around
 * is to use spinner.post(...) with a Runnable to assign the
 * OnItemSelectedListener after layout.
 *
 * If you do not call setSelection(...) manually, the callback
 * may be called with the first item in the adapter you have
 * set. The common work-around for that is to count callbacks.
 *
 * While these workarounds usually *seem* to work, the callback
 * may still be called repeatedly for other reasons while the
 * selection hasn't actually changed. This will happen for
 * example, if the user has accessibility options enabled -
 * which is more common than you might think as several apps
 * use this for different purposes, like detecting which
 * notifications are active.
 *
 * Ideally, your OnItemSelectedListener callback should be
 * coded defensively so that no problem would occur even
 * if the callback was called repeatedly with the same values
 * without any user interaction, so no workarounds are needed.
 *
 * This class does that for you. It keeps track of the values
 * you have set with the setSelection(...) methods, and
 * proxies the OnItemSelectedListener callback so your callback
 * only gets called if the selected item's position differs
 * from the one you have set by code, or the first item if you
 * did not set it.
 *
 * This also means that if the user actually clicks the item
 * that was previously selected by code (or the first item
 * if you didn't set a selection by code), the callback will
 * not fire.
 *
 * To implement, replace current occurrences of:
 *
 *     Spinner spinner =
 *         (Spinner)findViewById(R.id.xxx);
 *
 * with:
 *
 *     SpinnerHelper spinner =
 *         new SpinnerHelper(findViewById(R.id.xxx))
 *
 * SpinnerHelper proxies the (my) most used calls to Spinner
 * but not all of them. Should a method not be available, use:
 *
 *      spinner.getSpinner().someMethod(...)
 *
 * Or just add the proxy method yourself :)
 *
 * (Quickly) Tested on devices from 2.3.6 through 4.2.2
 *
 * @author Jorrit "Chainfire" Jongma
 * @license WTFPL (do whatever you want with this, nobody cares)
 * http://stackoverflow.com/a/17125287
 */
public class SpinnerHelper implements OnItemSelectedListener {
  private final Spinner spinner;

  private int lastPosition = -1;
  private OnItemSelectedListener proxiedItemSelectedListener = null;

  public SpinnerHelper(Spinner spinner) {
    if (spinner != null) {
      this.spinner = spinner;
      if (this.spinner.getAdapter() != null && this.spinner.getAdapter().getCount() > 0) {
        lastPosition = 0;
      }
    } else {
      this.spinner = null;
    }
  }

  public Spinner getSpinner() {
    return spinner;
  }

  public void setSelection(int position) {
    lastPosition = Math.max(-1, position);
    spinner.setSelection(position);
  }

  public void setSelection(int position, boolean animate) {
    lastPosition = Math.max(-1, position);
    spinner.setSelection(position, animate);
  }

  public void setOnItemSelectedListener(OnItemSelectedListener listener) {
    proxiedItemSelectedListener = listener;
    spinner.setOnItemSelectedListener(listener == null ? null : this);
  }

  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    if (position != lastPosition) {
      lastPosition = position;
      if (proxiedItemSelectedListener != null) {
        proxiedItemSelectedListener.onItemSelected(
            parent, view, position, id
        );
      }
    }
  }

  public void onNothingSelected(AdapterView<?> parent) {
    if (-1 != lastPosition) {
      lastPosition = -1;
      if (proxiedItemSelectedListener != null) {
        proxiedItemSelectedListener.onNothingSelected(
            parent
        );
      }
    }
  }

  public void setAdapter(SpinnerAdapter adapter) {
    if (adapter.getCount() > 0) {
      lastPosition = 0;
    }
    spinner.setAdapter(adapter);
  }

  public SpinnerAdapter getAdapter() {
    return spinner.getAdapter();
  }

  public int getCount() {
    return spinner.getCount();
  }

  public Object getItemAtPosition(int position) {
    return spinner.getItemAtPosition(position);
  }

  public long getItemIdAtPosition(int position) {
    return spinner.getItemIdAtPosition(position);
  }

  public Object getSelectedItem() {
    return spinner.getSelectedItem();
  }

  public long getSelectedItemId() {
    return spinner.getSelectedItemId();
  }

  public int getSelectedItemPosition() {
    return spinner.getSelectedItemPosition();
  }

  public void setEnabled(boolean enabled) {
    spinner.setEnabled(enabled);
  }

  public boolean isEnabled() {
    return spinner.isEnabled();
  }
}