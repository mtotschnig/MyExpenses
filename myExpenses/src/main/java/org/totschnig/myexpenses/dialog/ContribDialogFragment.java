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
import org.totschnig.myexpenses.util.licence.Package;
import org.totschnig.myexpenses.util.tracking.Tracker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

import static org.totschnig.myexpenses.activity.ContribInfoDialogActivity.KEY_FEATURE;
import static org.totschnig.myexpenses.util.LicenceHandler.LicenceStatus.CONTRIB;
import static org.totschnig.myexpenses.util.LicenceHandler.LicenceStatus.EXTENDED;
import static org.totschnig.myexpenses.util.LicenceHandler.LicenceStatus.PROFESSIONAL;

public class ContribDialogFragment extends CommitSafeDialogFragment implements DialogInterface.OnClickListener, View.OnClickListener {
  @Nullable
  private ContribFeature feature;
  private RadioButton contribButton, extendedButton, professionalButton;
  private boolean contribVisible;
  private boolean extendedVisible;
  @Inject
  LicenceHandler licenceHandler;

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
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx = getActivity();
    LicenceHandler.LicenceStatus licenceStatus = licenceHandler.getLicenceStatus();
    @SuppressLint("InflateParams")
    final View view = LayoutInflater.from(ctx).inflate(R.layout.contrib_dialog, null);
    AlertDialog.Builder builder = new AlertDialog.Builder(ctx,
        MyApplication.getThemeType().equals(MyApplication.ThemeType.dark) ?
            R.style.ContribDialogThemeDark : R.style.ContribDialogThemeLight);

    //preapre HEADER
    CharSequence message;
    if (feature != null) {
      CharSequence featureDescription = feature.buildFullInfoString(ctx),
          linefeed = Html.fromHtml("<br>"),
          removePhrase = feature.buildRemoveLimitation(getActivity(), true);
      message = TextUtils.concat(featureDescription, linefeed, removePhrase);
      if (feature.hasTrial()) {
        TextView usagesLeftTextView = view.findViewById(R.id.usages_left);
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

    List<CharSequence> contribFeatureLabelsAsList = Utils.getContribFeatureLabelsAsList(ctx, CONTRIB);
    List<CharSequence> extendedFeatureLabelsAsList = Utils.getContribFeatureLabelsAsList(ctx, EXTENDED);

    //prepare CONTRIB section
    View contribContainer = view.findViewById(R.id.contrib_feature_container);
    contribContainer.setBackgroundColor(getResources().getColor(R.color.premium_licence));
    if (licenceStatus == null && CONTRIB.covers(feature)) {
     contribVisible = true;
      CharSequence contribList = Utils.makeBulletList(ctx, contribFeatureLabelsAsList);
      ((TextView) contribContainer.findViewById(R.id.package_feature_list)).setText(contribList);
    } else {
      contribContainer.setVisibility(View.GONE);
    }

    //prepare EXTENDED section
    View extendedContainer = view.findViewById(R.id.extended_feature_container);
    extendedContainer.setBackgroundColor(getResources().getColor(R.color.extended_licence));
    if (LicenceHandler.HAS_EXTENDED && CONTRIB.greaterOrEqual(licenceStatus) && EXTENDED.covers(feature)) {
      extendedVisible = true;
      ArrayList<CharSequence> lines = new ArrayList<>();
      if (contribVisible) {
        lines.add(getString(R.string.all_contrib_key_features) + "\n+");
      } else if (licenceStatus == null && feature != null && feature.isExtended()) {
        lines.addAll(contribFeatureLabelsAsList);
      }
      lines.addAll(extendedFeatureLabelsAsList);
      ((TextView) extendedContainer.findViewById(R.id.package_feature_list)).setText(Utils.makeBulletList(ctx, lines));
    } else {
      extendedContainer.setVisibility(View.GONE);
    }

    //prepare PROFESSIONAL section
    ArrayList<CharSequence> lines = new ArrayList<>();
    View professionalContainer = view.findViewById(R.id.professional_feature_container);
    professionalContainer.setBackgroundColor(getResources().getColor(R.color.professional_licence));
    if (extendedVisible) {
      lines.add(getString(R.string.all_extended_key_features) + "\n+");
    } else if(feature != null && feature.isProfessional()) {
      if (licenceStatus == null) {
        lines.addAll(contribFeatureLabelsAsList);
      }
      if (CONTRIB.greaterOrEqual(licenceStatus)) {
        lines.addAll(extendedFeatureLabelsAsList);
      }
    }
    lines.addAll(Utils.getContribFeatureLabelsAsList(ctx, PROFESSIONAL));

    ((TextView) professionalContainer.findViewById(R.id.package_feature_list)).setText(Utils.makeBulletList(ctx, lines));

    builder.setTitle(feature == null ? R.string.menu_contrib :
            feature.isExtended() ? R.string.dialog_title_extended_feature :
                R.string.dialog_title_contrib_feature)
        .setView(view)
        .setNegativeButton(R.string.dialog_contrib_no, this)
        .setIcon(R.mipmap.ic_launcher_alt)
        .setPositiveButton(R.string.upgrade_now, this);
    AlertDialog dialog = builder.create();
    if (contribVisible || extendedVisible) {
      if (contribVisible) {
        contribButton = contribContainer.findViewById(R.id.package_button);
        ((TextView) contribContainer.findViewById(R.id.package_label)).setText(R.string.contrib_key);
        ((TextView) contribContainer.findViewById(R.id.package_price)).setText(
            licenceHandler.getFormattedPrice(Package.Contrib));
        contribContainer.setOnClickListener(this);
        contribButton.setOnClickListener(this);
      }
      if (extendedVisible) {
        extendedButton = extendedContainer.findViewById(R.id.package_button);
        ((TextView) extendedContainer.findViewById(R.id.package_label)).setText(R.string.extended_key);
        ((TextView) extendedContainer.findViewById(R.id.package_price)).setText(
            licenceHandler.getFormattedPrice(licenceStatus == null ? Package.Extended : Package.Upgrade));
        extendedContainer.setOnClickListener(this);
        extendedButton.setOnClickListener(this);
      }
      professionalButton = professionalContainer.findViewById(R.id.package_button);
      ((TextView) professionalContainer.findViewById(R.id.package_label)).setText(R.string.professional_key);
      ((TextView) professionalContainer.findViewById(R.id.package_price)).setText("less than 1.00 € / month");
      view.findViewById(R.id.professional_feature_container).setOnClickListener(this);
      professionalButton.setOnClickListener(this);

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
    boolean contribChecked = false, extendedChecked = false, professionalChecked = false;
    if (v.getId() == R.id.contrib_feature_container || v == contribButton) {
      contribChecked = true;
    } else if (v.getId() == R.id.extended_feature_container || v == extendedButton) {
      extendedChecked = true;
    } else {
      professionalChecked = true;
    }
    professionalButton.setChecked(professionalChecked);
    if (contribVisible) {
      contribButton.setChecked(contribChecked);
    }
    if (extendedVisible) {
      extendedButton.setChecked(extendedChecked);
    }
    ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
  }
}