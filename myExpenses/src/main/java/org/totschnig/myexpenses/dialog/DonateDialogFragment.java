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
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.widget.TextView;
import android.widget.Toast;

/**
 * the DonateDialog is shown on devices where Google Play is not available, in two contexts
 * 1) From the PREFKEY_CONTRIB_INSTALL entry of MyPreferenceActivity, here the Dialog is 
 *    instantiated through buildDialog and shown with showDialog
 * 2) From the ContribDialog when user clicks on "Buy". Here it is shown as DialogFragmen
 */
public class DonateDialogFragment extends CommitSafeDialogFragment {

  private static final String KEY_EXTENDED = "extended";

  public static final DonateDialogFragment newInstance(boolean extended) {
    DonateDialogFragment fragment = new DonateDialogFragment();
    Bundle args = new Bundle();
    args.putBoolean(KEY_EXTENDED,extended);
    fragment.setArguments(args);
    return fragment;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    boolean isExtended = getArguments().getBoolean(KEY_EXTENDED);
    DonationUriVisitor listener = new DonationUriVisitor(getActivity());
    final TextView message = new TextView(getActivity());
    int padding = (int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
    message.setPadding(padding,padding,padding,0);
    message.setMovementMethod(LinkMovementMethod.getInstance());
    CharSequence linefeed = Html.fromHtml("<br><br>");
    message.setText(TextUtils.concat(
        getString(R.string.donate_dialog_text),
        " ",
        Html.fromHtml("<a href=\"http://myexpenses.totschnig.org/#premium\">" + getString(R.string.learn_more) + "</a>."),
        linefeed,
        getString(R.string.thank_you)
    ));
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    return builder
      .setTitle(isExtended ? R.string.extended_key : R.string.contrib_key)
      .setView(message)
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
      } else if (which == AlertDialog.BUTTON_POSITIVE) {
        String paypalButtonId = MyApplication.getInstance().isContribEnabled() ?
            "KPXNZHMXJE8ZJ" : "A7ZPSCUTS23K6";
        String uri =
            "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id="
                +  paypalButtonId;
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