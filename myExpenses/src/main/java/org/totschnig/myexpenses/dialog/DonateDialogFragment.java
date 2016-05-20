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

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ContribIFace;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.util.Utils;

import android.app.Activity;
import android.os.Build;
import android.support.v7.app.AlertDialog;
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

import java.util.Locale;

/**
 * the DonateDialog is shown on devices where Google Play is not available, in two contexts
 * 1) From the PREFKEY_CONTRIB_INSTALL entry of MyPreferenceActivity, here the Dialog is
 * instantiated through buildDialog and shown with showDialog
 * 2) From the ContribDialog when user clicks on "Buy". Here it is shown as DialogFragmen
 */
public class DonateDialogFragment extends CommitSafeDialogFragment {

  private static final String KEY_EXTENDED = "extended";
  public static final String PAYPAL_BUTTON_CONTRIB = "Contrib";
  public static final String PAYPAL_BUTTON_EXTENDED = "Extended";
  public static final String PAYPAL_BUTTON_UPGRADE = "Upgrade";
  public static final String BITCOIN_ADDRESS = "1GCUGCSfFXzSC81ogHu12KxfUn3cShekMn";

  public static final DonateDialogFragment newInstance(boolean extended) {
    DonateDialogFragment fragment = new DonateDialogFragment();
    Bundle args = new Bundle();
    args.putBoolean(KEY_EXTENDED, extended);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    boolean isExtended = getArguments().getBoolean(KEY_EXTENDED);
    DonationUriVisitor listener = new DonationUriVisitor();
    final TextView message = new TextView(getActivity());
    int padding = (int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
    message.setPadding(padding, padding, padding, 0);
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
    int title = MyApplication.getInstance().isContribEnabled() ?
        R.string.pref_contrib_purchase_title_upgrade :
        (isExtended ? R.string.extended_key : R.string.contrib_key);
    return builder
        .setTitle(title)
        .setView(message)
        .setPositiveButton(R.string.donate_button_paypal, listener)
        .setNeutralButton(R.string.donate_button_bitcoin, listener)
        .create();
  }

  public class DonationUriVisitor implements OnClickListener {

    @Override
    public void onClick(DialogInterface dialog, int which) {
      Intent intent;
      Activity ctx = getActivity();
      if (which == AlertDialog.BUTTON_NEUTRAL) {
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("bitcoin:" + BITCOIN_ADDRESS));
        if (Utils.isIntentAvailable(ctx, intent)) {
          ctx.startActivityForResult(intent, 0);
        } else {
          ClipboardManager clipboard = (ClipboardManager)
              ctx.getSystemService(Context.CLIPBOARD_SERVICE);
          clipboard.setText(BITCOIN_ADDRESS);
          Toast.makeText(ctx,
              "My Expenses Bitcoin Donation address " + BITCOIN_ADDRESS + " copied to clipboard",
              Toast.LENGTH_LONG).show();
          if (ctx instanceof MessageDialogListener) {
            ((MessageDialogListener) ctx).onMessageDialogDismissOrCancel();
          }
        }
      } else if (which == AlertDialog.BUTTON_POSITIVE) {
        String host = BuildConfig.DEBUG ? "www.sandbox.paypal.com" : "www.paypal.com" ;
        String paypalButtonId = BuildConfig.DEBUG? "TURRUESSCUG8N" : "LBUDF8DSWJAZ8";
        String whichLicence = MyApplication.getInstance().isContribEnabled() ?
            PAYPAL_BUTTON_UPGRADE :
            (getArguments().getBoolean(KEY_EXTENDED) ? PAYPAL_BUTTON_EXTENDED : PAYPAL_BUTTON_CONTRIB);
        String uri = String.format(Locale.US,
            "https://%s/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=%s&on0=%s&os0=%s&lc=%s",
            host, paypalButtonId, "Licence", whichLicence, getPaypalLocale());



        intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));
        ctx.startActivityForResult(intent, 0);
      }
    }
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    if (getActivity() == null) {
      return;
    }
    getActivity().finish();
  }

  private String getPaypalLocale() {
    return Locale.getDefault().toString();
  }
}