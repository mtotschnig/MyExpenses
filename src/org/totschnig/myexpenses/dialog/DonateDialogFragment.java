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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ContribIFace;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.text.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/**
 * the DonateDialog is shown on devices where Google Play is not available, in two contexts
 * 1) From the PREFKEY_CONTRIB_INSTALL entry of MyPreferenceActivity, here the Dialog is 
 *    instantiated through buildDialog and shown with showDialog
 * 2) From the ContribDialog when user clicks on "Buy". Here it is shown as DialogFragmen
 */
public class DonateDialogFragment extends CommitSafeDialogFragment {

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
    DonationUriVisitor listener = new DonationUriVisitor((Activity) ctx);
    return new AlertDialog.Builder(ctx)
      .setTitle(R.string.donate)
      .setMessage(
        ctx.getString(MyApplication.getInstance().isContribEnabled ?
            R.string.pref_contrib_donate_summary_already_contrib :
            R.string.donate_dialog_text)
        +"\n\n"+
        ctx.getString(R.string.thank_you))
      .setNegativeButton(R.string.donate_button_flattr, listener)
      .setPositiveButton(R.string.donate_button_paypal, listener)
      .setNeutralButton(R.string.donate_button_bitcoin, listener)
      .create();
  }
  public static class DonationUriVisitor implements OnClickListener {
    Activity ctx;

    public DonationUriVisitor(Activity ctx) {
      super();
      this.ctx = ctx;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
      String bitcoinAddress = "1GCUGCSfFXzSC81ogHu12KxfUn3cShekMn";
      Intent intent;
      if (which == AlertDialog.BUTTON_NEUTRAL) {
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("bitcoin:" + bitcoinAddress));
        if (Utils.isIntentAvailable(ctx,intent)) {
          ctx.startActivityForResult(intent, 0);
        } else {
          ClipboardManager clipboard = (ClipboardManager)
              ctx.getSystemService(Context.CLIPBOARD_SERVICE);
          clipboard.setText(bitcoinAddress);
          Toast.makeText(ctx,
              "My Expenses Bitcoin Donation address " + bitcoinAddress + " copied to clipboard",
              Toast.LENGTH_LONG).show();
          if (ctx instanceof MessageDialogListener) {
            ((MessageDialogListener) ctx).onMessageDialogDismissOrCancel();
          }
        }
      } else {
        String uri = (which == AlertDialog.BUTTON_POSITIVE) ?
            "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=A7ZPSCUTS23K6" :
            "https://flattr.com/thing/1028216/My-Expenses-GPL-licenced-Android-Expense-Tracking-App";
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));
        ctx.startActivityForResult(intent, 0);
      }
    }
  }
  @Override
  public void onCancel (DialogInterface dialog) {
    if (getActivity()==null) {
      return;
    }
    ((ContribIFace)getActivity()).contribFeatureNotCalled();
  }
}