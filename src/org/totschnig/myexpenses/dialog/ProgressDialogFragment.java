/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

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
    f.setCancelable(false);
    return f;
  }
 
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) { 
    final ProgressDialog dialog = new ProgressDialog(getActivity());
    int message = getArguments().getInt("message");
    if (message != 0)
      dialog.setMessage(getString(message));
    else
      dialog.setMessage("…");
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