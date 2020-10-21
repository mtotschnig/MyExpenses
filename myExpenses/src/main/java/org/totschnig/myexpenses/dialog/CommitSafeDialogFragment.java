//on some occasions, upon showing a DialogFragment we run into
//"java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState"
//we catch this here, and ignore silently, which hopefully should be save, since activity is being paused
//https://code.google.com/p/android/issues/detail?id=23096#c4

package org.totschnig.myexpenses.dialog;

import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.ui.SnackbarAction;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public abstract class CommitSafeDialogFragment extends DialogFragment {

  protected View dialogView;
  protected LayoutInflater layoutInflater;

  protected AlertDialog.Builder initBuilder() {
    MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(requireContext());
    layoutInflater = LayoutInflater.from(materialAlertDialogBuilder.getContext());
    return materialAlertDialogBuilder;
  }

  protected AlertDialog.Builder initBuilderWithView(int layoutResourceId) {
    AlertDialog.Builder builder = initBuilder();
    //noinspection InflateParams
    dialogView = layoutInflater.inflate(layoutResourceId, null);
    builder.setView(dialogView);
    return builder;
  }

  @Override
  public int show(@NonNull FragmentTransaction transaction, String tag) {
      try {
          return super.show(transaction, tag);
      } catch (IllegalStateException e) {
        CrashHandler.report(e);
      }
      return -1;
  }

  @Override
  public void show(@NonNull FragmentManager manager, String tag) {
      try {
          super.show(manager, tag);
      } catch (IllegalStateException e) {
        CrashHandler.report(e);
      }
  }

  @Override
  public void dismiss() {
    try {
      super.dismiss();
    } catch (IllegalStateException e) {
      CrashHandler.report(e);
    }
  }

  protected void showSnackbar(int resId) {
    showSnackbar(getString(resId), Snackbar.LENGTH_LONG, null);
  }

  public void showSnackbar(CharSequence message, int duration, SnackbarAction snackbarAction) {
    View view = dialogView != null ? dialogView : getDialog().getWindow().getDecorView();
    Snackbar snackbar = Snackbar.make(view, message, duration);
    UiUtils.increaseSnackbarMaxLines(snackbar);
    if (snackbarAction != null) {
      snackbar.setAction(snackbarAction.resId, snackbarAction.listener);
    }
    snackbar.show();
  }

}
