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
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;

import java.util.List;

import timber.log.Timber;

public class VersionDialogFragment extends CommitSafeDialogFragment implements OnClickListener {

  private static final String KEY_FROM = "from";
  private static final String KEY_WITH_IMPORTANT_UPGRADE_INFO = "withImportantUpgradeInfo";

  public static VersionDialogFragment newInstance(int from, boolean withImportantUpgradeInfo) {
    VersionDialogFragment dialogFragment = new VersionDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putInt(KEY_FROM, from);
    bundle.putBoolean(KEY_WITH_IMPORTANT_UPGRADE_INFO, withImportantUpgradeInfo);
    dialogFragment.setArguments(bundle);
    dialogFragment.setCancelable(false);
    return dialogFragment;
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
    //noinspection InflateParams
    View view = li.inflate(R.layout.versiondialog, null);
    final ListView lv = view.findViewById(R.id.list);
    ArrayAdapter<VersionInfo> adapter = new ArrayAdapter<VersionInfo>(ctx,
        R.layout.version_row, R.id.versionInfoName, versions) {

      @NonNull
      @Override
      public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LinearLayout row = (LinearLayout) super.getView(position, convertView, parent);
        VersionInfo version = versions.get(position);
        final TextView heading = row.findViewById(R.id.versionInfoName);
        heading.setText(version.name);
        String[] changes = version.getChanges(getContext());
        ((TextView) row.findViewById(R.id.versionInfoChanges))
            .setText(changes != null ? ("\u25b6 " + TextUtils.join("\n\u25b6 ", changes)) : "");

        TextView learn_more = row.findViewById(R.id.versionInfoLearnMore);
        final int resId = getResources().getIdentifier("version_more_info_" + version.nameCondensed.replace(".", ""), "array", ctx.getPackageName());
        if (resId == 0) {
          learn_more.setVisibility(View.GONE);
        } else {
          makeVisibleAndClickable(learn_more, R.string.learn_more, new ClickableSpan() {
            @Override
            public void onClick(View v) {
              PopupMenu popup = new PopupMenu(getActivity(), heading);
              popup.setOnMenuItemClickListener(item -> {
                showMoreInfo(item.getItemId(), getResources().getStringArray(resId));
                return true;
              });
              popup.inflate(R.menu.version_info);
              popup.show();
            }
          });
        }
        return row;
      }
    };
    lv.setAdapter(adapter);
/*    if (getArguments().getBoolean(KEY_WITH_IMPORTANT_UPGRADE_INFO)) {
      view.findViewById(R.id.ImportantUpgradeInfoHeading).setVisibility(View.VISIBLE);
      TextView importantUpgradeInfoBody = view.findViewById(R.id.ImportantUpgradeInfoBody);
      importantUpgradeInfoBody.setVisibility(View.VISIBLE);
      importantUpgradeInfoBody.setText(R.string.roadmap_intro);
      TextView importantUpgradeInfoLearnMore = view.findViewById(R.id.ImportantUpgradeInfoLearnMore);
      makeVisibleAndClickable(importantUpgradeInfoLearnMore, R.string.roadmap_particpate, new ClickableSpan() {
        @Override
        public void onClick(View widget) {
         getActivity().startActivity(new Intent(getContext(), RoadmapVoteActivity.class));
        }
      });
    }*/

    AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
        .setTitle(getString(R.string.help_heading_whats_new))
        .setIcon(R.mipmap.ic_myexpenses)
        .setView(view)
        .setNegativeButton(android.R.string.ok, this);
    if (!MyApplication.getInstance().getLicenceHandler().isContribEnabled())
      builder.setPositiveButton(R.string.menu_contrib, this);
    return builder.create();
  }

  void makeVisibleAndClickable(TextView textView, int resId, ClickableSpan clickableSpan) {
    textView.setVisibility(View.VISIBLE);
    Spannable span = Spannable.Factory.getInstance().newSpannable(getString(resId));
    span.setSpan(clickableSpan, 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    textView.setText(span);
    textView.setMovementMethod(LinkMovementMethod.getInstance());
  }

  void showMoreInfo(final int itemId, String[] postIds) {
    String uri = null;
    switch (itemId) {
      case R.id.facebook:
        uri = "https://www.facebook.com/MyExpenses/posts/" + postIds[0];
        break;
      case R.id.google:
        uri = "https://plus.google.com/+MyexpensesMobi/posts/" + postIds[1];
        break;
    }
    Intent i = new Intent(Intent.ACTION_VIEW);
    i.setData(Uri.parse(uri));
    startActivity(i);
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (getActivity() == null) {
      return;
    }
    if (which == AlertDialog.BUTTON_POSITIVE)
      ((MessageDialogListener) getActivity()).dispatchCommand(R.id.CONTRIB_INFO_COMMAND, null);
  }

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
        Timber.e("missing change log entry for version %d", code);
        return null;
      } else {
        String[] changesArray = res.getStringArray(resId);
        resId = res.getIdentifier("contributors_" + nameCondensed, "array", ctx.getPackageName());
        if (resId != 0) {
          String[] contributorArray = res.getStringArray(resId);
          String[] resultArray = new String[changesArray.length];
          for (int i = 0; i < changesArray.length; i++) {
            resultArray[i] = changesArray[i] +
                (TextUtils.isEmpty(contributorArray[i]) ? "" :
                    (" (" + ctx.getString(R.string.contributed_by, contributorArray[i]) + ")"));
          }
          return resultArray;
        }
        return changesArray;
      }
    }
  }
}
