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

import java.util.ArrayList;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class VersionDialogFragment extends CommitSafeDialogFragment implements OnClickListener {
  public static final VersionDialogFragment newInstance(int from) {
    VersionDialogFragment dialogFragment = new VersionDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putInt("from", from);
    dialogFragment.setArguments(bundle);
    dialogFragment.setCancelable(false);
    return dialogFragment;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Bundle bundle = getArguments();
    final Activity ctx  = (Activity) getActivity();
    LayoutInflater li = LayoutInflater.from(ctx);
    int from = bundle.getInt("from");
    Resources res = getResources();
    int[] versionCodes = res.getIntArray(R.array.version_codes);
    String[] versionNames = res.getStringArray(R.array.version_names);
    final ArrayList<VersionInfo> versions = new ArrayList<VersionInfo>();
    for (int i=0;i<versionCodes.length;i++) {
      int code = versionCodes[i];
      if (from >= code) {
        break;
      }
      versions.add(new VersionInfo(code, versionNames[i]));
    }
    View view = li.inflate(R.layout.versiondialog, null);
    final ListView lv = (ListView) view.findViewById(R.id.list);
    ArrayAdapter<VersionInfo> adapter = new ArrayAdapter<VersionInfo>(ctx,
        R.layout.version_row, R.id.versionInfoName, versions) {

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout row = (LinearLayout) super.getView(position, convertView, parent);
        VersionInfo version = versions.get(position);
        final TextView heading = (TextView) row.findViewById(R.id.versionInfoName);
        heading.setText(version.name);
        String[] changes = version.getChanges(ctx);
        ((TextView) row.findViewById(R.id.versionInfoChanges))
          .setText(changes != null ? ("- " + TextUtils.join("\n- ",changes)) : "");

        TextView learn_more = (TextView) row.findViewById(R.id.versionInfoLearnMore);

        final Resources res= ctx.getResources();
        final int resId = res.getIdentifier("version_more_info_"+version.nameCondensed.replace(".", ""), "array", ctx.getPackageName());
        if (resId ==0) {
          learn_more.setVisibility(View.GONE);
        } else {
          learn_more.setVisibility(View.VISIBLE);
          learn_more.setTag(resId);
          Spannable span = Spannable.Factory.getInstance().newSpannable(res.getString(R.string.learn_more));
          span.setSpan(new ClickableSpan() {
              @Override
              public void onClick(View v) {
                PopupMenu popup = new PopupMenu(getActivity(), heading);
                // This activity implements OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                  @Override
                  public boolean onMenuItemClick(MenuItem item) {
                    String[] postIds = res.getStringArray(resId);
                    String uri = null;
                    switch(item.getItemId()) {
                    case R.id.facebook:
                      uri = "https://www.facebook.com/MyExpenses/posts/" + postIds[0];
                      break;
                    case R.id.google:
                      uri = "https://plus.google.com/116736113799210525299/posts/" + postIds[1];
                      break;
                    }
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(uri));
                    startActivity(i);
                    return true;
                  }

                });
                popup.inflate(R.menu.version_info);
                popup.show();
              } }, 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
          learn_more.setText(span);
          learn_more.setMovementMethod(LinkMovementMethod.getInstance());
        }
        return row;
      }
    };
    lv.setAdapter(adapter);
    if (MyApplication.getInstance().showImportantUpgradeInfo) {
      view.findViewById(R.id.ImportantUpgradeInfoHeading).setVisibility(View.VISIBLE);
      view.findViewById(R.id.ImportantUpgradeInfoBody).setVisibility(View.VISIBLE);
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
      .setTitle(getString(R.string.help_heading_whats_new))
      .setIcon(R.drawable.myexpenses)
      .setView(view)
      .setNegativeButton(android.R.string.ok, this);
    if (!MyApplication.getInstance().isContribEnabled())
      builder.setPositiveButton( R.string.menu_contrib, this);
    return builder.create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (getActivity()==null) {
      return;
    }
    if (which == AlertDialog.BUTTON_POSITIVE)
      ((MessageDialogListener) getActivity()).dispatchCommand(R.id.CONTRIB_INFO_COMMAND,null);
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
      Resources res= ctx.getResources();
      int resId = res.getIdentifier("whats_new_"+nameCondensed, "array", ctx.getPackageName());//new based on name
      if (resId == 0) {
        resId = res.getIdentifier("whats_new_"+code, "array", ctx.getPackageName());//legacy based on code
      }
      if (resId == 0) {
        Log.e(MyApplication.TAG, "missing change log entry for version " + code);
        return null;
      } else {
        return res.getStringArray(resId);
      }
    }
  }
}
