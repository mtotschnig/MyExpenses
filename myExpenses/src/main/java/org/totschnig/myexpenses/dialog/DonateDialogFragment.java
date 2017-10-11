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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import com.annimon.stream.Stream;

import org.totschnig.myexpenses.activity.ContribInfoDialogActivity;
import org.totschnig.myexpenses.util.licence.Package;

import java.util.Locale;

public class DonateDialogFragment extends CommitSafeDialogFragment {

  private static final String KEY_PACKAGE = "package";

  public static DonateDialogFragment newInstance(Package aPackage) {
    DonateDialogFragment fragment = new DonateDialogFragment();
    Bundle args = new Bundle();
    args.putSerializable(KEY_PACKAGE, aPackage);
    fragment.setArguments(args);
    return fragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Package aPackage = getPackage();
    DonationUriVisitor listener = new DonationUriVisitor();

    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    CharSequence[] items = Stream.of(aPackage.getPaymentOptions()).map(this::getString).toArray(size -> new String[size]);
    return builder
        .setTitle(aPackage.getButtonLabel(getContext()))
        .setItems(items, listener)
        .create();
  }

  @NonNull
  private Package getPackage() {
    Package aPackage= (Package) getArguments().getSerializable(KEY_PACKAGE);
    if (aPackage == null) aPackage = Package.Contrib;
    return aPackage;
  }

  private class DonationUriVisitor implements OnClickListener {

    @Override
    public void onClick(DialogInterface dialog, int which) {
      Activity ctx = getActivity();
      Package aPackage = getPackage();
      ((ContribInfoDialogActivity) ctx).startPayment(aPackage.getPaymentOptions()[which], aPackage);
    }
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    if (getActivity() instanceof ContribInfoDialogActivity) {
      getActivity().finish();
    }
  }

  private String getPaypalLocale() {
    return Locale.getDefault().toString();
  }
}