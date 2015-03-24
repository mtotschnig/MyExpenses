//on some occasions, upon showing a DialogFragment we run into
//"java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState"
//we catch this here, and ignore silently, which hopefully should be save, since activity is being paused
//https://code.google.com/p/android/issues/detail?id=23096#c4

package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.util.Utils;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public abstract class CommitSafeDialogFragment extends DialogFragment {

  @Override
  public int show(FragmentTransaction transaction, String tag) {
      try {
          return super.show(transaction, tag);
      } catch (IllegalStateException e) {
        Utils.reportToAcra(e);
      }
      return -1;
  }

  @Override
  public void show(FragmentManager manager, String tag) {
      try {
          super.show(manager, tag);
      } catch (IllegalStateException e) {
        Utils.reportToAcra(e);
      }
  }
}
