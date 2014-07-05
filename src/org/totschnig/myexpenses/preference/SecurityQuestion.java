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

package org.totschnig.myexpenses.preference;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.Utils;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

public class SecurityQuestion extends DialogPreference  {
  private EditText question,answer;
  public SecurityQuestion(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setDialogLayoutResource(R.layout.security_question);
    }
     
    public SecurityQuestion(Context context, AttributeSet attrs) {
    super(context, attrs);
    setDialogLayoutResource(R.layout.security_question);
    }
    @Override
    protected void onBindDialogView(View view) {
      question = (EditText) view.findViewById(R.id.question);
      answer = (EditText) view.findViewById(R.id.answer);
      super.onBindDialogView(view);
      question.setText(getPersistedString(""));
   }
    @Override
    protected void onDialogClosed(boolean positiveResult) {
      super.onDialogClosed(positiveResult);
      if (positiveResult) {
        persistString(question.getText().toString());
        Editor editor = getEditor();
        editor.putString(MyApplication.PrefKey.SECURITY_ANSWER.getKey(), Utils.md5(answer.getText().toString()));
        editor.commit();
      }
    }
}
