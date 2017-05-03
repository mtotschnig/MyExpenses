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
import android.support.annotation.Nullable;
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
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.LicenceHandler;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.tracking.Tracker;

import java.io.Serializable;

import timber.log.Timber;

import static org.totschnig.myexpenses.activity.ContribInfoDialogActivity.KEY_FEATURE;

public class ContribDialogFragment extends CommitSafeDialogFragment implements DialogInterface.OnClickListener, View.OnClickListener {
  @Nullable
  private ContribFeature feature;
  private RadioButton contribButton, extendedButton;

  public static ContribDialogFragment newInstance(String feature, Serializable tag) {
    ContribDialogFragment dialogFragment = new ContribDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putString(KEY_FEATURE, feature);
    bundle.putSerializable(ContribInfoDialogActivity.KEY_TAG, tag);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String featureStringExtra = getArguments().getString(KEY_FEATURE);
    if (featureStringExtra != null) {
      feature = ContribFeature.valueOf(featureStringExtra);
    }
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx = getActivity();
    @SuppressLint("InflateParams")
    final View view = LayoutInflater.from(ctx).inflate(R.layout.contrib_dialog, null);
    AlertDialog.Builder builder = new AlertDialog.Builder(ctx,
        MyApplication.getThemeType().equals(MyApplication.ThemeType.dark) ?
            R.style.ContribDialogThemeDark : R.style.ContribDialogThemeLight);
    CharSequence message;
    if (feature != null) {
      CharSequence featureDescription = feature.buildFullInfoString(ctx),
          linefeed = Html.fromHtml("<br>"),
          removePhrase = feature.buildRemoveLimitation(getActivity(), true);
      message = TextUtils.concat(featureDescription, linefeed, removePhrase);
      if (feature.hasTrial()) {
        TextView usagesLeftTextView = (TextView) view.findViewById(R.id.usages_left);
        usagesLeftTextView.setText(feature.buildUsagesLefString(ctx));
        usagesLeftTextView.setVisibility(View.VISIBLE);
      }
    } else {
      message = getText(R.string.dialog_contrib_text_2);
      if (DistribHelper.isGithub()) {
        message = TextUtils.concat(getText(R.string.dialog_contrib_text_1), " ",
            getText(R.string.dialog_contrib_text_2));
      }
    }
    ((TextView) view.findViewById(R.id.feature_info)).setText(message);
    boolean isContrib = MyApplication.getInstance().getLicenceHandler().isContribEnabled();
    boolean userCanChoose = true;
    contribButton = (RadioButton) view.findViewById(R.id.contrib_button);
    extendedButton = (RadioButton) view.findViewById(R.id.extended_button);
    if (isFeatureExtended() || (feature == null && isContrib)) {
      view.findViewById(R.id.contrib_feature_container).setVisibility(View.GONE);
      userCanChoose = false;
      extendedButton.setChecked(true);
    } else {
      ((TextView) view.findViewById(R.id.contrib_feature_list)).setText(
          Utils.makeBulletList(ctx, Utils.getContribFeatureLabelsAsList(ctx, LicenceHandler.LicenceStatus.CONTRIB)));
    }
    if (LicenceHandler.HAS_EXTENDED) {
      String[] lines;
      if (isFeatureExtended() && !isContrib) {
        lines = Utils.getContribFeatureLabelsAsList(ctx, null);
      } else {
        String[] extendedFeatures = Utils.getContribFeatureLabelsAsList(ctx, LicenceHandler.LicenceStatus.EXTENDED);
        lines = feature == null || feature.isExtended()  ? extendedFeatures : //user is Contrib
            Utils.joinArrays(new String[]{getString(R.string.all_contrib_key_features) + "\n+"},
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
        .setTitle(feature == null ? R.string.menu_contrib :
            feature.isExtended() ? R.string.dialog_title_extended_feature :
                R.string.dialog_title_contrib_feature)
        .setView(view)
        .setNegativeButton(R.string.dialog_contrib_no, this)
        .setIcon(R.mipmap.ic_launcher_alt)
        .setPositiveButton(R.string.upgrade_now, this);
    AlertDialog dialog = builder.create();
    if (userCanChoose) {
      view.findViewById(R.id.contrib_feature_container).setOnClickListener(this);
      contribButton.setOnClickListener(this);
      view.findViewById(R.id.extended_feature_container).setOnClickListener(this);
      extendedButton.setOnClickListener(this);
      dialog.setOnShowListener(new ButtonOnShowDisabler());
    }
    return dialog;
  }

  protected boolean isFeatureExtended() {
    return feature != null && feature.isExtended();
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
        Timber.w("Neither premium nor extended button checked, should not happen");
      }
    } else {
      //BUTTON_NEGATIV
      ctx.logEvent(Tracker.EVENT_CONTRIB_DIALOG_NEGATIVE, null);
      ctx.finish(false);
    }
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    ContribInfoDialogActivity ctx = (ContribInfoDialogActivity) getActivity();
    if (ctx != null) {
      ctx.logEvent(Tracker.EVENT_CONTRIB_DIALOG_CANCEL, null);
      ctx.finish(true);
    }
  }

  @Override
  public void onClick(View v) {
    RadioButton self, other;
    if (v.getId() == R.id.contrib_feature_container || v.getId() == R.id.contrib_button) {
      other = extendedButton;
      self = contribButton;
    }
    else {
      other = contribButton;
      self = extendedButton;
    }
    self.setChecked(true);
    other.setChecked(false);
    ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
  }
}