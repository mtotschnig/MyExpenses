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

import java.util.ArrayList;
import java.util.List;

public class FormValidator {
	private List<AbstractFormFieldValidator> fieldValidators = new ArrayList<>();

	public FormValidator() {
	}

	public void add(AbstractFormFieldValidator fieldValidator) {
		fieldValidators.add(fieldValidator);
	}

	public boolean validate() {
		for (AbstractFormFieldValidator validator : fieldValidators) {
			validator.clear();
		}
		
		boolean valid = true;
		for (AbstractFormFieldValidator validator : fieldValidators) {
			if (!validator.validate()) {
				valid = false;
			}
		}

		return valid;
	}
}
