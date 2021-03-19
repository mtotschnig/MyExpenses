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

public class FormFieldNotEmptyValidator extends AbstractFormFieldValidator {
	public FormFieldNotEmptyValidator(TextView field) {
		super(field);
	}

	@Override
	public int getMessage() {
		return R.string.validate_error_not_empty;
	}

	@Override
	public boolean isValid() {
		return !fields[0].getText().toString().trim().isEmpty();
	}
}
