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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.fragment.DbWriteFragment;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public abstract class EditActivity extends ProtectedFragmentActivity implements
    DbWriteFragment.TaskCallbacks {

  protected boolean mIsSaving;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.one, menu);
    super.onCreateOptionsMenu(menu);
    return true;
  }
  @Override
  public boolean dispatchCommand(int command, Object tag) {
    switch(command) {
    case R.id.Confirm:
      if (!mIsSaving) {
        saveState();
      }
      return true;
    }
    return super.dispatchCommand(command, tag);
  }
  protected void saveState() {
    mIsSaving = true;
    startDbWriteTask(false);
  }

  protected void changeEditTextBackground(ViewGroup root) {
    //not needed in HOLO
    if (Build.VERSION.SDK_INT > 10)
      return;
    SharedPreferences settings = MyApplication.getInstance().getSettings();
    if (settings.getString(MyApplication.PREFKEY_UI_THEME_KEY,"dark").equals("dark")) {
      int c = getResources().getColor(R.color.theme_dark_button_color);
      for(int i = 0; i <root.getChildCount(); i++) {
        View v = root.getChildAt(i);
        if(v instanceof EditText) {
          Utils.setBackgroundFilter(v, c);
        } else if(v instanceof ViewGroup) {
          changeEditTextBackground((ViewGroup)v);
        }
      }
    }
  }
  @Override
  public void onPostExecute(Object result) {
    mIsSaving = false;
    super.onPostExecute(result);
  }
}