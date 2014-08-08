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
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
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
        ((TextView) row.findViewById(R.id.versionInfoName)).setText(version.name);
        String[] changes = version.getChanges(ctx);
        ((TextView) row.findViewById(R.id.versionInfoChanges))
          .setText(changes != null ? ("- " + TextUtils.join("\n- ",changes)) : "");
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
    public VersionInfo(int code, String name) {
      super();
      this.code = code;
      this.name = name;
    }
    public String[] getChanges(Context ctx) {
      Resources res= ctx.getResources();
      int resId = res.getIdentifier("whats_new_"+name.replace(".", ""), "array", ctx.getPackageName());//new based on name
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
