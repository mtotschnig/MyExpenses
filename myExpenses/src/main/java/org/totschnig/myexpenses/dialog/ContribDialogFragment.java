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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.util.LicenceHandler;

import java.io.Serializable;

public class ContribDialogFragment extends CommitSafeDialogFragment implements DialogInterface.OnClickListener {
  private ContribFeature feature;
  RadioButton contribButton, extendedButton;

  public static ContribDialogFragment newInstance(ContribFeature feature, Serializable tag) {
    ContribDialogFragment dialogFragment = new ContribDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putSerializable(ContribInfoDialogActivity.KEY_FEATURE, feature);
    bundle.putSerializable(ContribInfoDialogActivity.KEY_TAG, tag);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Bundle bundle = getArguments();
    feature = (ContribFeature) bundle.getSerializable(ContribInfoDialogActivity.KEY_FEATURE);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx = getActivity();
    @SuppressLint("InflateParams")
    final View view = LayoutInflater.from(ctx).inflate(R.layout.contrib_dialog, null);
    CharSequence featureDescription = feature.buildFullInfoString(ctx);

    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
    CharSequence
        linefeed = Html.fromHtml("<br>"),
        removePhrase = feature.buildRemoveLimitation(getActivity(), true),
        message = TextUtils.concat(featureDescription, linefeed, removePhrase);
    boolean isContrib = MyApplication.getInstance().getLicenceHandler().isContribEnabled();
    /*if (!isContrib) {
      CharSequence featureList = Utils.getContribFeatureLabelsAsFormattedList(ctx, feature,
          feature.isExtended() ? null : LicenceHandler.LicenceStatus.CONTRIB); //if feature is extended, we list all features
      //if user has contrib key, he already has access to premium features,
      message = TextUtils.concat(message,
          linefeed, featureList);
      if (!feature.isExtended()) {
        if (LicenceHandler.HAS_EXTENDED) {
          String pro = getString(R.string.dialog_contrib_extended_gain_access);
          CharSequence extendedList = Utils.getContribFeatureLabelsAsFormattedList(ctx, feature, LicenceHandler.LicenceStatus.EXTENDED);
          message = TextUtils.concat(message, linefeed, pro, linefeed, extendedList);
        }
        builder.setNegativeButton(R.string.dialog_contrib_buy_premium, this);
      }
    }*/
    ((TextView) view.findViewById(R.id.feature_info)).setText(message);
    ((TextView) view.findViewById(R.id.usages_left)).setText(feature.buildUsagesLefString(ctx));
    contribButton = (RadioButton) view.findViewById(R.id.contrib_button);
    extendedButton = (RadioButton) view.findViewById(R.id.extended_button);
    contribButton.setOnClickListener(v -> extendedButton.setChecked(false));
    extendedButton.setOnClickListener(v -> contribButton.setChecked(false));
    builder
        .setTitle(feature.isExtended() ? R.string.dialog_title_extended_feature : R.string.dialog_title_contrib_feature)
        .setView(view)
        .setNeutralButton(R.string.dialog_contrib_no, this)
        .setIcon(R.mipmap.ic_launcher_alt);
    if (LicenceHandler.HAS_EXTENDED) {
      builder.setPositiveButton(isContrib ? R.string.dialog_contrib_upgrade_extended : R.string.dialog_contrib_buy_extended, this);
    }
    return builder.create();
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    ContribInfoDialogActivity ctx = (ContribInfoDialogActivity) getActivity();
    if (ctx == null) {
      return;
    }
    if (which == AlertDialog.BUTTON_POSITIVE) {
      ctx.contribBuyDo(true);
    } else if (which == AlertDialog.BUTTON_NEGATIVE) {
      ctx.contribBuyDo(false);
    } else {
      //BUTTON_NEUTRAL
      ctx.finish(false);
    }
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    ContribInfoDialogActivity ctx = (ContribInfoDialogActivity) getActivity();
    if (ctx == null) {
      return;
    }
    ctx.finish(true);
  }
}