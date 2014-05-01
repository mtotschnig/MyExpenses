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
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.fragment.PlanList;
import org.totschnig.myexpenses.fragment.ContextualActionBarFragment;
import org.totschnig.myexpenses.fragment.TemplatesList;
import org.totschnig.myexpenses.task.TaskExecutionFragment;

import com.android.calendar.CalendarContractCompat.Events;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

public class ManageTemplates extends ProtectedFragmentActivity implements TabListener {
  public enum HelpVariant {
    templates,plans
  }
  public static final int PLAN_INSTANCES_CURSOR = 1;

  public long calledFromCalendarWithId = 0;
  private boolean mTransferEnabled = false;
  ViewPager mViewPager;
  SectionsPagerAdapter mSectionsPagerAdapter;
  int mCurrentPosition = 0;
  
  private int monkey_state = 0;

  @Override
  public boolean onKeyUp (int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_CAMERA) {
      switch (monkey_state) {
      case 0:
        ((PlanList) getSupportFragmentManager().findFragmentByTag(
            mSectionsPagerAdapter.getFragmentName(1)))
          .listFocus();
        return true;
      }
    }
    return super.onKeyUp(keyCode, event);
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    Bundle extras = getIntent().getExtras();
    mTransferEnabled = extras.getBoolean(MyApplication.KEY_TRANSFER_ENABLED,false);

    setContentView(R.layout.viewpager);
    setTitle(R.string.menu_manage_plans);

    // Set up the action bar.
    final ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

    // Create the adapter that will return a fragment for each of the three
    // primary sections of the app.
    mSectionsPagerAdapter = new SectionsPagerAdapter(
        getSupportFragmentManager());

    // Set up the ViewPager with the sections adapter.
    mViewPager = (ViewPager) findViewById(R.id.viewpager);
    mViewPager.setAdapter(mSectionsPagerAdapter);

    // When swiping between different sections, select the corresponding
    // tab. We can also use ActionBar.Tab#select() to do this if we have
    // a reference to the Tab.
    mViewPager
      .setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
          finishActionMode();
          actionBar.setSelectedNavigationItem(position);
          mCurrentPosition = position;
          helpVariant = position == 0 ? HelpVariant.templates : HelpVariant.plans;
        }
      });

    actionBar.addTab(actionBar.newTab()
        .setText(R.string.menu_manage_plans_tab_templates)
        .setTabListener(this));
    actionBar.addTab(actionBar.newTab()
        .setText(R.string.menu_manage_plans_tab_plans)
        .setTabListener(this));

    String uriString = extras.getString(Events.CUSTOM_APP_URI);
    if (uriString != null) {
      List <String> uriPath = Uri.parse(uriString).getPathSegments();
      calledFromCalendarWithId = Long.parseLong(uriPath.get(2));
      mViewPager.setCurrentItem(1);
    }
    helpVariant = HelpVariant.templates;
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.templates, menu);
    super.onCreateOptionsMenu(menu);
    menu.findItem(R.id.CREATE_TRANSFER_COMMAND).setVisible(mTransferEnabled);
    return true;
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    Intent i;
    switch(command) {
    case R.id.CREATE_TRANSACTION_COMMAND:
    case R.id.CREATE_TRANSFER_COMMAND:
      i = new Intent(this, ExpenseEdit.class);
      i.putExtra(MyApplication.KEY_OPERATION_TYPE,
          command == R.id.CREATE_TRANSACTION_COMMAND ? MyExpenses.TYPE_TRANSACTION : MyExpenses.TYPE_TRANSFER);
      i.putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, true);
      i.putExtra(ExpenseEdit.KEY_NEW_PLAN_ENABLED, getNewPlanEnabled());
      startActivity(i);
      return true;
    case R.id.DELETE_COMMAND_DO:
      finishActionMode();
      startTaskExecution(
          TaskExecutionFragment.TASK_DELETE_TEMPLATES,
          (Long[])tag,
          null,
          R.string.progress_dialog_deleting);
      return true;
    case R.id.EDIT_COMMAND:
      finishActionMode();
      i = new Intent(this, ExpenseEdit.class);
      i.putExtra("template_id",((Long)tag));
      i.putExtra(ExpenseEdit.KEY_NEW_PLAN_ENABLED, getNewPlanEnabled());
      //TODO check what to do on Result
      startActivityForResult(i, EDIT_TRANSACTION_REQUEST);
      return true;
    case R.id.CANCEL_CALLBACK_COMMAND:
      finishActionMode();
      return true;
    }
    return super.dispatchCommand(command, tag);
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
    public String getFragmentName(int currentPosition) {
      //http://stackoverflow.com/questions/7379165/update-data-in-listfragment-as-part-of-viewpager
      //would call this function if it were visible
      //return makeFragmentName(R.id.viewpager,currentPosition);
      return "android:switcher:"+R.id.viewpager+":"+getItemId(currentPosition);
    }
  }
  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    switch(taskId) {
    case TaskExecutionFragment.TASK_NEW_FROM_TEMPLATE:
      Integer successCount = (Integer) o;
      String msg = successCount == 0 ?  getString(R.string.save_transaction_error) :
        getResources().getQuantityString(R.plurals.save_transaction_from_template_success, successCount, successCount);
      Toast.makeText(this,msg, Toast.LENGTH_LONG).show();
    }
    PlanList pl = (PlanList) getSupportFragmentManager().findFragmentByTag(
        mSectionsPagerAdapter.getFragmentName(1));
    pl.refresh();
  }
  public boolean getNewPlanEnabled() {
    return MyApplication.getInstance().isContribEnabled ||
        ((PlanList) getSupportFragmentManager().findFragmentByTag(
            mSectionsPagerAdapter.getFragmentName(1)))
          .newPlanEnabled;
  }
  public void finishActionMode() {
    ContextualActionBarFragment f =
    ((ContextualActionBarFragment) getSupportFragmentManager().findFragmentByTag(
        mSectionsPagerAdapter.getFragmentName(mCurrentPosition)));
    if (f!=null) {
      f.finishActionMode();
    }
  }
}
