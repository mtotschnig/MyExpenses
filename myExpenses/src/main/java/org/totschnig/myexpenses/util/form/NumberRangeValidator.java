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

import android.widget.TextView;

import org.totschnig.myexpenses.R;

public class NumberRangeValidator extends AbstractFormFieldValidator {
  private final int min;
  private final int max;

  public NumberRangeValidator(TextView field, int min, int max) {
    super(field);
    this.min = min;
    this.max = max;
  }

  @Override
  public int getMessage() {
    return R.string.validation_error_number_out_of_range;
  }

  @Override
  protected Object[] getMessageFormatArgs() {
    return new Object[]{min, max};
  }

  @Override
  public boolean isValid() {
    try {
      int input = Integer.parseInt(fields[0].getText().toString().trim());
      return input >= min && input <= max;
    } catch (NumberFormatException e) {
      return false;
    }

  }
}
