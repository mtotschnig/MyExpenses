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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.util.LicenceHandler;
import org.totschnig.myexpenses.util.Utils;

import java.io.Serializable;

public class ContribDialogFragment extends CommitSafeDialogFragment implements DialogInterface.OnClickListener, View.OnClickListener {
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

    AlertDialog.Builder builder = new AlertDialog.Builder(ctx,
        MyApplication.getThemeType().equals(MyApplication.ThemeType.dark) ?
            R.style.ContribDialogThemeDark : R.style.ContribDialogThemeLight);
    CharSequence
        linefeed = Html.fromHtml("<br>"),
        removePhrase = feature.buildRemoveLimitation(getActivity(), true),
        message = TextUtils.concat(featureDescription, linefeed, removePhrase);
    boolean isContrib = MyApplication.getInstance().getLicenceHandler().isContribEnabled();
    ((TextView) view.findViewById(R.id.feature_info)).setText(message);
    ((TextView) view.findViewById(R.id.usages_left)).setText(feature.buildUsagesLefString(ctx));
    boolean userCanChoose = true;
    contribButton = (RadioButton) view.findViewById(R.id.contrib_button);
    extendedButton = (RadioButton) view.findViewById(R.id.extended_button);
    if (feature.isExtended()) {
      view.findViewById(R.id.contrib_feature_container).setVisibility(View.GONE);
      userCanChoose = false;
      extendedButton.setChecked(true);
    } else {
      ((TextView) view.findViewById(R.id.contrib_feature_list)).setText(
          Utils.makeBulletList(ctx, Utils.getContribFeatureLabelsAsList(ctx, LicenceHandler.LicenceStatus.CONTRIB)));
    }
    if (LicenceHandler.HAS_EXTENDED) {
      String[] lines;
      if (feature.isExtended() && !isContrib) {
        lines = Utils.getContribFeatureLabelsAsList(ctx, null);
      } else {
        String[] extendedFeatures = Utils.getContribFeatureLabelsAsList(ctx, LicenceHandler.LicenceStatus.EXTENDED);
        lines = feature.isExtended() ? extendedFeatures : //user is Contrib
            Utils.joinArrays(new String[]{getString(R.string.all_premium_key_features) + "\n+"},
                extendedFeatures);
      }
      ((TextView) view.findViewById(R.id.extended_feature_list)).setText(
          Utils.makeBulletList(ctx, lines));
    } else {
      view.findViewById(R.id.extended_feature_container).setVisibility(View.GONE);
      contribButton.setChecked(true);
      userCanChoose = false;
    }
    builder
        .setTitle(feature.isExtended() ? R.string.dialog_title_extended_feature : R.string.dialog_title_contrib_feature)
        .setView(view)
        .setNegativeButton(R.string.dialog_contrib_no, this)
        .setIcon(R.mipmap.ic_launcher_alt)
        .setPositiveButton(R.string.upgrade_now, this);
    AlertDialog dialog = builder.create();
    if (userCanChoose) {
      contribButton.setOnClickListener(this);
      extendedButton.setOnClickListener(this);
      dialog.setOnShowListener(new ButtonOnShowDisabler());
    }
    return dialog;
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    ContribInfoDialogActivity ctx = (ContribInfoDialogActivity) getActivity();
    if (ctx == null) {
      return;
    }
    if (which == AlertDialog.BUTTON_POSITIVE) {
      if(extendedButton.isChecked()) {
        ctx.contribBuyDo(true);
      } else if (contribButton.isChecked()) {
        ctx.contribBuyDo(false);
      } else {
        Log.w(MyApplication.TAG, "Neither premium nor extended button checked, should not happen");
      }
    } else {
      //BUTTON_NEGATIV
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

  @Override
  public void onClick(View v) {
    (v.equals(contribButton) ? extendedButton : contribButton).setChecked(false);
    ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
  }
}