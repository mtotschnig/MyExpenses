package org.totschnig.myexpenses.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import butterknife.ButterKnife;
import icepick.Icepick;

public abstract class ButtonWithDialog extends MaterialButton {

  public ButtonWithDialog(Context context, AttributeSet attrs) {
    super(context, attrs);
    ButterKnife.bind(this);
    setOnClickListener(v -> showDialog());
  }

  public void showDialog() {
    getHost().hideKeyBoardAndShowDialog(getId());
  }

  @NonNull
  @Override public Parcelable onSaveInstanceState() {
    return Icepick.saveInstanceState(this, super.onSaveInstanceState());
  }

  @Override public void onRestoreInstanceState(Parcelable state) {
    super.onRestoreInstanceState(Icepick.restoreInstanceState(this, state));
    update();
  }

  protected abstract void update();

  protected Host getHost() {
    Context context = getContext();
    while (context instanceof android.content.ContextWrapper) {
      if (context instanceof Host) {
        return (Host)context;
      }
      context = ((ContextWrapper)context).getBaseContext();
    }
    throw new IllegalStateException("Host context does not implement interface");
  }

  public interface Host {
    void hideKeyBoardAndShowDialog(int id);
    void onValueSet(View v);
  }

  public abstract Dialog onCreateDialog();
}
