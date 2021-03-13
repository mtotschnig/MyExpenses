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

import com.annimon.stream.IntStream;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.licence.Package;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class DonateDialogFragment extends BaseDialogFragment {

  private static final String KEY_PACKAGE = "package";
  @Inject
  LicenceHandler licenceHandler;

  public static DonateDialogFragment newInstance(Package aPackage) {
    DonateDialogFragment fragment = new DonateDialogFragment();
    Bundle args = new Bundle();
    args.putParcelable(KEY_PACKAGE, aPackage);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((MyApplication) requireActivity().getApplication()).getAppComponent().inject(this);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Package aPackage = getPackage();
    DonationUriVisitor listener = new DonationUriVisitor();

    AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
    CharSequence[] items = IntStream.of(getPaymentOptions(aPackage)).mapToObj(this::getString).toArray(String[]::new);
    return builder
        .setTitle(licenceHandler.getButtonLabel(aPackage))
        .setItems(items, listener)
        .create();
  }

  @NonNull
  private Package getPackage() {
    Package aPackage= getArguments().getParcelable(KEY_PACKAGE);
    if (aPackage == null) {
      aPackage = Package.Contrib.INSTANCE;
    }
    return aPackage;
  }

  private class DonationUriVisitor implements OnClickListener {

    @Override
    public void onClick(DialogInterface dialog, int which) {
      Activity ctx = getActivity();
      Package aPackage = getPackage();
      ((ContribInfoDialogActivity) ctx).startPayment(getPaymentOptions(aPackage)[which], aPackage);
    }
  }

  private int[] getPaymentOptions(Package aPackage) {
    return licenceHandler.getPaymentOptions(aPackage);
  }

  @Override
  public void onCancel(@NonNull DialogInterface dialog) {
    if (getActivity() instanceof ContribInfoDialogActivity) {
      getActivity().finish();
    }
  }
}