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

package org.totschnig.myexpenses.activity;

import java.util.List;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.TemplateDetailFragment;
import org.totschnig.myexpenses.fragment.PlanList;
import org.totschnig.myexpenses.fragment.TaskExecutionFragment;
import org.totschnig.myexpenses.fragment.TemplatesList;
import org.totschnig.myexpenses.model.Transaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.android.calendar.CalendarContractCompat.Events;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ManageTemplates extends ProtectedFragmentActivity implements TabListener {

  public static final int DELETE_TEMPLATE = Menu.FIRST;
  public static final int CREATE_INSTANCE_EDIT = Menu.FIRST +1;
  public static final int EDIT_TEMPLATE = Menu.FIRST +2;
  public static final int CREATE_INSTANCE_SAVE = Menu.FIRST +3;
  public static final int PLAN_INSTANCES_CURSOR = 1;

  public boolean calledFromCalendar;
  private boolean mTransferEnabled = false;
  ViewPager mViewPager;
  SectionsPagerAdapter mSectionsPagerAdapter;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
      setTheme(MyApplication.getThemeId());
      super.onCreate(savedInstanceState);
      Bundle extras = getIntent().getExtras();
      mTransferEnabled = extras.getBoolean("transferEnabled",false);
      String uriString = extras.getString(Events.CUSTOM_APP_URI);
      if (uriString != null) {
        calledFromCalendar = true;
        List <String> uriPath = Uri.parse(uriString).getPathSegments();
        //mAccountId = Long.parseLong(uriPath.get(1));
        TemplateDetailFragment.newInstance(Long.parseLong(uriPath.get(2)))
          .show(getSupportFragmentManager(), "TEMPLATE_DETAIL");
      }
      setContentView(R.layout.manage_templates);
      setTitle(R.string.menu_manage_plans);

    // Set up the action bar.
    final ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

    // Create the adapter that will return a fragment for each of the three
    // primary sections of the app.
    mSectionsPagerAdapter = new SectionsPagerAdapter(
        getSupportFragmentManager());

    // Set up the ViewPager with the sections adapter.
    mViewPager = (ViewPager) findViewById(R.id.pager);
    mViewPager.setAdapter(mSectionsPagerAdapter);

    // When swiping between different sections, select the corresponding
    // tab. We can also use ActionBar.Tab#select() to do this if we have
    // a reference to the Tab.
    mViewPager
        .setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
          @Override
          public void onPageSelected(int position) {
            actionBar.setSelectedNavigationItem(position);
          }
        });

    actionBar.addTab(actionBar.newTab()
        .setText("Templates")
        .setTabListener(this));
    actionBar.addTab(actionBar.newTab()
        .setText("Plans")
        .setTabListener(this));
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.templates, menu);
    super.onCreateOptionsMenu(menu);
    menu.findItem(R.id.INSERT_TRANSFER_COMMAND).setVisible(mTransferEnabled);
    return true;
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    switch(command) {
    case R.id.INSERT_TA_COMMAND:
    case R.id.INSERT_TRANSFER_COMMAND:
      Intent intent = new Intent(this, ExpenseEdit.class);
      intent.putExtra("operationType",
          command == R.id.INSERT_TA_COMMAND ? MyExpenses.TYPE_TRANSACTION : MyExpenses.TYPE_TRANSFER);
      intent.putExtra("newTemplate", true);
      startActivity(intent);
      return true;
    case R.id.DELETE_COMMAND_DO:
      FragmentManager fm = getSupportFragmentManager();
      fm.beginTransaction()
        .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_DELETE_TEMPLATE,(Long)tag, null), "ASYNC_TASK")
        .commit();
      return true;
    }
    return super.dispatchCommand(command, tag);
   }
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.add(0,EDIT_TEMPLATE,0,R.string.menu_edit);
    menu.add(0,DELETE_TEMPLATE,0,R.string.menu_delete);
  }
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    Intent intent;
    switch(item.getItemId()) {
    case EDIT_TEMPLATE:
      Intent i = new Intent(this, ExpenseEdit.class);
      i.putExtra("template_id", info.id);
      //TODO check what to do on Result
      startActivityForResult(i, MyExpenses.ACTIVITY_EDIT);
      return true;
    case DELETE_TEMPLATE:
      MessageDialogFragment.newInstance(
          R.string.dialog_title_warning_delete_template,
          R.string.warning_delete_template,
          new MessageDialogFragment.Button(android.R.string.yes, R.id.DELETE_COMMAND_DO, info.id),
          null,
          MessageDialogFragment.Button.noButton())
        .show(getSupportFragmentManager(),"DELETE_ACCOUNT");
      return true;
    case CREATE_INSTANCE_EDIT:
      intent = new Intent(this, ExpenseEdit.class);
      intent.putExtra("template_id", info.id);
      intent.putExtra("instantiate", true);
      startActivity(intent);
      return true;
    case CREATE_INSTANCE_SAVE:
      //TODO strict mode
      if (Transaction.getInstanceFromTemplate(info.id).save() == null)
        Toast.makeText(getBaseContext(),getString(R.string.save_transaction_error), Toast.LENGTH_LONG).show();
      else
        Toast.makeText(getBaseContext(),getString(R.string.save_transaction_from_template_success), Toast.LENGTH_LONG).show();
      return true;
    }
    return super.onContextItemSelected(item);
  }
  @Override
  public void onTabSelected(Tab tab, FragmentTransaction ft) {
    mViewPager.setCurrentItem(tab.getPosition());
  }
  @Override
  public void onTabUnselected(Tab tab, FragmentTransaction ft) {
  }
  @Override
  public void onTabReselected(Tab tab, FragmentTransaction ft) {
  }
  public class SectionsPagerAdapter extends FragmentPagerAdapter {

    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      // getItem is called to instantiate the fragment for the given page.
      switch (position){
      case 0:
        return new TemplatesList();
      case 1:
        return new PlanList();
      }
      return null;
    }

    @Override
    public int getCount() {
      // Show 3 total pages.
      return 2;
    }
  }
}
