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

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ContribIFace;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * the DonateDialog is shown on devices where Google Play is not available, in two contexts
 * 1) From the PREFKEY_CONTRIB_INSTALL entry of MyPreferenceActivity, here the Dialog is 
 *    instantiated through buildDialog and shown with showDialog
 * 2) From the ContribDialog when user clicks on "Buy". Here it is shown as DialogFragmen
 */
public class DonateDialogFragment extends DialogFragment {

  public static final DonateDialogFragment newInstance() {
    return new DonateDialogFragment();
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return buildDialog(getActivity());
  }
  /**
   * we need this Dialog also from MyPreferenceActivity which cannot show a DialogFragment,
   * hence we make it available through static method
   * @param ctx
   * @return
   */
  public static AlertDialog buildDialog(Context ctx) {
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
    alertDialogBuilder.setTitle(R.string.donate);
    alertDialogBuilder.setMessage(
        ctx.getString(R.string.donate_dialog_text)
        +"\n\n"+
        ctx.getString(R.string.thank_you));
    DonationUriVisitor listener = new DonationUriVisitor(ctx);
    alertDialogBuilder.setNegativeButton(R.string.donate_button_flattr, listener);
    alertDialogBuilder.setPositiveButton(R.string.donate_button_paypal, listener);
    return alertDialogBuilder.create();
  }
  public static class DonationUriVisitor implements OnClickListener {
    Context ctx;

    public DonationUriVisitor(Context ctx) {
      super();
      this.ctx = ctx;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
      String uri = (which == AlertDialog.BUTTON_POSITIVE) ?
          "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=KPXNZHMXJE8ZJ" :
          "https://flattr.com/thing/1028216/My-Expenses-GPL-licenced-Android-Expense-Tracking-App";
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse(uri));
      ctx.startActivity(i); 
    }
  }
  @Override
  public void onCancel (DialogInterface dialog) {
    ((ContribIFace)getActivity()).contribFeatureNotCalled();
  }
}