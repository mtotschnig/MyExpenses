package org.totschnig.myexpenses.dialog;

import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.view.KeyEvent;

public class ProgressDialogFragment extends DialogFragment {
  
  public static ProgressDialogFragment newInstance(int message) {
    ProgressDialogFragment f = new ProgressDialogFragment ();
    Bundle bundle = new Bundle();
    bundle.putInt("message", message);
    f.setArguments(bundle);
    return f;
  }
 
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) { 
    final ProgressDialog dialog = new ProgressDialog(getActivity());
    dialog.setMessage(getString(getArguments().getInt("message")));
    dialog.setIndeterminate(true);
    dialog.setCancelable(false);
     
    // Disable the back button
    OnKeyListener keyListener = new OnKeyListener() {
     
      @Override
      public boolean onKey(DialogInterface dialog, int keyCode,
      KeyEvent event) {
        if( keyCode == KeyEvent.KEYCODE_BACK){  
        return true;
      }
        return false;
      }
       
    };
    dialog.setOnKeyListener(keyListener);
    return dialog;
  }
}