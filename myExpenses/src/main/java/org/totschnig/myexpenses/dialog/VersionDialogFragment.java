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
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.BaseActivity;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.licence.LicenceHandler;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

public class VersionDialogFragment extends BaseDialogFragment implements OnClickListener {

  private static final String KEY_FROM = "from";
  private static final String KEY_WITH_IMPORTANT_UPGRADE_INFO = "withImportantUpgradeInfo";

  @Inject
  LicenceHandler licenceHandler;

  public static VersionDialogFragment newInstance(int from, boolean withImportantUpgradeInfo) {
    VersionDialogFragment dialogFragment = new VersionDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putInt(KEY_FROM, from);
    bundle.putBoolean(KEY_WITH_IMPORTANT_UPGRADE_INFO, withImportantUpgradeInfo);
    dialogFragment.setArguments(bundle);
    dialogFragment.setCancelable(false);
    return dialogFragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Bundle bundle = getArguments();
    Activity ctx = getActivity();
    LayoutInflater li = LayoutInflater.from(ctx);
    int from = bundle.getInt(KEY_FROM);
    Resources res = getResources();

    final List<VersionInfo> versions = Stream.of(res.getStringArray(R.array.versions))
        .map(version -> version.split(";"))
        .takeWhile(parts -> Integer.parseInt(parts[0]) > from)
        .map(parts -> new VersionInfo(Integer.parseInt(parts[0]), parts[1]))
        .collect(Collectors.toList());
    AlertDialog.Builder builder = initBuilderWithView(R.layout.versiondialog);
    final ListView lv = dialogView.findViewById(R.id.list);
    ArrayAdapter<VersionInfo> adapter = new ArrayAdapter<VersionInfo>(ctx,
        R.layout.version_row, R.id.versionInfoName, versions) {

      @NonNull
      @Override
      public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewGroup row = (ViewGroup) super.getView(position, convertView, parent);
        VersionInfo version = versions.get(position);
        final TextView heading = row.findViewById(R.id.versionInfoName);
        heading.setText(version.name);
        String[] changes = version.getChanges(getContext());
        ((TextView) row.findViewById(R.id.versionInfoChanges))
            .setText(changes != null ? ("\u25b6 " + TextUtils.join("\n\u25b6 ", changes)) : "");

        configureMoreInfo(row.findViewById(R.id.versionInfoFacebook), version, "version_more_info_", "https://www.facebook.com/MyExpenses/posts/");
        configureMoreInfo(row.findViewById(R.id.versionInfoGithub), version, "project_board_", "https://github.com/mtotschnig/MyExpenses/projects/");
        return row;
      }
    };
    lv.setAdapter(adapter);
    if (getArguments().getBoolean(KEY_WITH_IMPORTANT_UPGRADE_INFO)) {
      dialogView.findViewById(R.id.ImportantUpgradeInfoHeading).setVisibility(View.VISIBLE);
      TextView importantUpgradeInfoBody = dialogView.findViewById(R.id.ImportantUpgradeInfoBody);
      importantUpgradeInfoBody.setVisibility(View.VISIBLE);
      importantUpgradeInfoBody.setText(R.string.upgrade_information_cloud_sync_storage_format);
/*      TextView importantUpgradeInfoLearnMore = view.findViewById(R.id.ImportantUpgradeInfoLearnMore);
      makeVisibleAndClickable(importantUpgradeInfoLearnMore, R.string.roadmap_particpate, new ClickableSpan() {
        @Override
        public void onClick(View widget) {
         getActivity().startActivity(new Intent(getContext(), RoadmapVoteActivity.class));
        }
      });*/
    }

    builder.setTitle(getString(R.string.help_heading_whats_new))
        .setIcon(R.mipmap.ic_myexpenses)
        .setNegativeButton(android.R.string.ok, this);
    if (!licenceHandler.isContribEnabled())
      builder.setPositiveButton(R.string.menu_contrib, this);
    return builder.create();
  }

  private void configureMoreInfo(View imageButton, VersionInfo version, String resPrefix, String baseUri) {
    final int resId = getResources().getIdentifier(resPrefix + version.nameCondensed, "string", requireContext().getPackageName());
    if (resId == 0) {
      imageButton.setVisibility(View.GONE);
    } else {
      imageButton.setVisibility(View.VISIBLE);
      imageButton.setOnClickListener(v -> showMoreInfo(baseUri + getString(resId)));
    }
  }

  void showMoreInfo(String uri) {
    ((BaseActivity) requireActivity()).startActionView(uri);
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (getActivity() == null) {
      return;
    }
    if (which == AlertDialog.BUTTON_POSITIVE)
      ((MessageDialogListener) getActivity()).dispatchCommand(R.id.CONTRIB_INFO_COMMAND, null);
  }

  @VisibleForTesting
  public static class VersionInfo {
    private int code;
    private String name;
    private String nameCondensed;

    public VersionInfo(int code, String name) {
      super();
      this.code = code;
      this.name = name;
      this.nameCondensed = name.replace(".", "");
    }

    public String[] getChanges(Context ctx) {
      Resources res = ctx.getResources();
      int resId = res.getIdentifier("whats_new_" + nameCondensed, "array", ctx.getPackageName());//new based on name
      if (resId == 0) {
        resId = res.getIdentifier("whats_new_" + code, "array", ctx.getPackageName());//legacy based on code
      }
      if (resId == 0) {
        CrashHandler.reportWithFormat("missing change log entry for version %d", code);
        return null;
      } else {
        String[] changesArray = res.getStringArray(resId);
        resId = res.getIdentifier("contributors_" + nameCondensed, "array", ctx.getPackageName());
        if (resId != 0) {
          String[] contributorArray = res.getStringArray(resId);
          String[] resultArray = new String[changesArray.length];
          for (int i = 0; i < changesArray.length; i++) {
            resultArray[i] = changesArray[i] +
                (contributorArray.length <= i || TextUtils.isEmpty(contributorArray[i]) ? "" :
                    (String.format(" (%s)", ctx.getString(R.string.contributed_by, contributorArray[i]))));
          }
          return resultArray;
        }
        return changesArray;
      }
    }
  }
}
