/*
 * Copyright 2012 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.totschnig.myexpenses.util.form;

import android.content.Context;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import static org.totschnig.myexpenses.util.MoreUiUtilsKt.findParentWithTypeRecursively;

public abstract class AbstractFormFieldValidator {
  protected Context context;
  protected TextView[] fields;

  public AbstractFormFieldValidator(TextView... fields) {
    this.context = fields[0].getContext();
    this.fields = fields;
  }

  public void clear() {
    for (TextView field : fields) {
      setError(field, null);
    }
  }

  public boolean validate() {
    boolean valid = isValid();

    for (TextView field : fields) {
      if (!valid) {
        String error = (String) field.getError();
        if (error == null) {
          error = "";
        } else {
          error += "\n\n";
        }

        Object[] formatArgs = getMessageFormatArgs();
        int message = getMessage();
        error += (formatArgs == null ? context.getString(message) :
            context.getString(message, formatArgs));

        setError(field, error);
      }
    }

    return valid;
  }

  protected abstract int getMessage();

  protected Object[] getMessageFormatArgs() {
    return null;
  }

  protected abstract boolean isValid();

  private void setError(TextView field, String error) {
    TextInputLayout container = findParentWithTypeRecursively(field, TextInputLayout.class) ;
    if (container != null) {
      container.setError(error);
    } else {
      field.setError(error);
    }
  }
}
