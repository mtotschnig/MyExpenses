//on some occasions, upon showing a DialogFragment we run into
//"java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState"
//we catch this here, and ignore silently, which hopefully should be save, since activity is being paused
//https://code.google.com/p/android/issues/detail?id=23096#c4

package org.totschnig.myexpenses.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.ui.SnackbarAction;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import timber.log.Timber;

public abstract class BaseDialogFragment extends DialogFragment {

  protected View dialogView;
  protected LayoutInflater layoutInflater;
  private Snackbar snackbar;

  @Inject
  PrefHandler prefHandler;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((MyApplication) requireActivity().getApplication()).getAppComponent().inject(this);
  }

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

  public void report(IllegalStateException e) {
    @Nullable final FragmentActivity activity = getActivity();
    if (activity == null) {
      Timber.w("Activity is null");
    } else {
      Timber.w("Activity is finishing?: %b", activity.isFinishing());
    }
    CrashHandler.report(e);
  }

  @Override
  public void dismiss() {
    try {
      super.dismiss();
    } catch (IllegalStateException e) {
      report(e);
    }
  }

  protected void showSnackbar(int resId) {
    showSnackbar(getString(resId), Snackbar.LENGTH_LONG, null);
  }

  public void showSnackbar(CharSequence message, int duration, SnackbarAction snackbarAction) {
    View view = dialogView != null ? dialogView : getDialog().getWindow().getDecorView();
    snackbar = Snackbar.make(view, message, duration);
    UiUtils.increaseSnackbarMaxLines(snackbar);
    if (snackbarAction != null) {
      snackbar.setAction(snackbarAction.resId, snackbarAction.listener);
    }
    snackbar.show();
  }

  protected void dismissSnackbar() {
    if (snackbar != null) {
      snackbar.dismiss();
    }
  }

}
