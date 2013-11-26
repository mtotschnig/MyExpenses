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


import java.text.SimpleDateFormat;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class TemplateDetailFragment extends DialogFragment implements OnClickListener {
  Template mTemplate;
  SimpleCursorAdapter mAdapter;
  
  public static final TemplateDetailFragment newInstance(Long id) {
    TemplateDetailFragment dialogFragment = new TemplateDetailFragment();
    Bundle bundle = new Bundle();
    bundle.putSerializable("id", id);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Bundle bundle = getArguments();
    //TODO strict mode violation
    mTemplate = Template.getInstanceFromDb(bundle.getLong("id"));
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Context wrappedCtx = DialogUtils.wrapContext2(getActivity());
    final LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.template_detail, null);
    //title
    ((TextView) view.findViewById(R.id.Title)).setText(mTemplate.title);
    if ((mTemplate.catId != null && mTemplate.catId > 0) ||
        mTemplate.transfer_peer != null)
      ((TextView) view.findViewById(R.id.Category)).setText(mTemplate.label);
    else
      view.findViewById(R.id.CategoryRow).setVisibility(View.GONE);
    //amount
    ((TextView) view.findViewById(R.id.Amount)).setText(Utils.formatCurrency(mTemplate.amount));
    //comment
    if (!mTemplate.comment.equals(""))
      ((TextView) view.findViewById(R.id.Comment)).setText(mTemplate.comment);
    else
      view.findViewById(R.id.CommentRow).setVisibility(View.GONE);
    //payee
    if (!mTemplate.payee.equals(""))
      ((TextView) view.findViewById(R.id.Payee)).setText(mTemplate.payee);
    else
      view.findViewById(R.id.PayeeRow).setVisibility(View.GONE);
    //Method
    if (mTemplate.methodId != null)
      ((TextView) view.findViewById(R.id.Method)).setText(PaymentMethod.getInstanceFromDb(mTemplate.methodId).getDisplayLabel());
    else
      view.findViewById(R.id.MethodRow).setVisibility(View.GONE);
    return new AlertDialog.Builder(getActivity())
      .setTitle(R.string.template)
      .setView(view)
      .setNegativeButton(android.R.string.ok,this)
      .setPositiveButton(R.string.menu_edit,this)
      .setNeutralButton(R.string.menu_apply, this)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    ManageTemplates ctx = (ManageTemplates) getActivity();
    switch(which) {
    case AlertDialog.BUTTON_POSITIVE:
      Intent i = new Intent(ctx, ExpenseEdit.class);
      i.putExtra("template_id", mTemplate.id);
      ctx.startActivityForResult(i, MyExpenses.ACTIVITY_EDIT);
      break;
    case AlertDialog.BUTTON_NEUTRAL:
      ctx.applyTemplate(mTemplate.id);
    case AlertDialog.BUTTON_NEGATIVE:
      if (ctx.calledFromCalendar) {
        ctx.setResult(Activity.RESULT_OK);
        ctx.finish();
      }
    }
  }
}
