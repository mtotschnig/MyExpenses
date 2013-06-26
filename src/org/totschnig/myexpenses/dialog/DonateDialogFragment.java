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
    alertDialogBuilder.setMessage(R.string.donate_dialog_text);
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