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

import android.content.res.Configuration;
import android.util.SparseArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import timber.log.Timber;



class ConfigurationHelper {
  private static final String SUFFIX_MASK = "_MASK";
  private static final String FIELD_SCREENLAYOUT = "screenLayout";
  private static final String FIELD_UIMODE = "uiMode";
  private static final String FIELD_MNC = "mnc";
  private static final String FIELD_MCC = "mcc";
  private static final String PREFIX_UI_MODE = "UI_MODE_";
  private static final String PREFIX_TOUCHSCREEN = "TOUCHSCREEN_";
  private static final String PREFIX_SCREENLAYOUT = "SCREENLAYOUT_";
  private static final String PREFIX_ORIENTATION = "ORIENTATION_";
  private static final String PREFIX_NAVIGATIONHIDDEN = "NAVIGATIONHIDDEN_";
  private static final String PREFIX_NAVIGATION = "NAVIGATION_";
  private static final String PREFIX_KEYBOARDHIDDEN = "KEYBOARDHIDDEN_";
  private static final String PREFIX_KEYBOARD = "KEYBOARD_";
  private static final String PREFIX_HARDKEYBOARDHIDDEN = "HARDKEYBOARDHIDDEN_";
  private ConfigurationHelper() {
  }

  //#### FROM ACRA ####
  /**
   * Creates a {@link JSONObject} listing all values human readable
   * from the provided Configuration instance.
   *
   * @param conf The Configuration to be described.
   * @return A JSONObject with all fields of the given Configuration,
   * with values replaced by constant names.
   */
  @NonNull
  static JSONObject configToJson(@NonNull Configuration conf) {
    final JSONObject result = new JSONObject();
    final Map<String, SparseArray<String>> valueArrays = getValueArrays();
    for (final Field f : conf.getClass().getFields()) {
      try {
        if (!Modifier.isStatic(f.getModifiers())) {
          final String fieldName = f.getName();
          try {
            if (f.getType().equals(int.class)) {
              result.put(fieldName, getFieldValueName(valueArrays, conf, f));
            } else if (f.get(conf) != null) {
              result.put(fieldName, f.get(conf));
            }
          } catch (JSONException e) {
            Timber.w(e,"Could not collect configuration field %s", fieldName);
          }
        }
      } catch (@NonNull IllegalArgumentException | IllegalAccessException e) {
        Timber.w(e,"Error while inspecting device configuration");
      }
    }
    return result;
  }

  @NonNull
  private static Map<String, SparseArray<String>> getValueArrays() {
    final Map<String, SparseArray<String>> valueArrays = new HashMap<>();
    final SparseArray<String> hardKeyboardHiddenValues = new SparseArray<>();
    final SparseArray<String> keyboardValues = new SparseArray<>();
    final SparseArray<String> keyboardHiddenValues = new SparseArray<>();
    final SparseArray<String> navigationValues = new SparseArray<>();
    final SparseArray<String> navigationHiddenValues = new SparseArray<>();
    final SparseArray<String> orientationValues = new SparseArray<>();
    final SparseArray<String> screenLayoutValues = new SparseArray<>();
    final SparseArray<String> touchScreenValues = new SparseArray<>();
    final SparseArray<String> uiModeValues = new SparseArray<>();

    for (final Field f : Configuration.class.getFields()) {
      if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())) {
        final String fieldName = f.getName();
        try {
          if (fieldName.startsWith(PREFIX_HARDKEYBOARDHIDDEN)) {
            hardKeyboardHiddenValues.put(f.getInt(null), fieldName);
          } else if (fieldName.startsWith(PREFIX_KEYBOARD)) {
            keyboardValues.put(f.getInt(null), fieldName);
          } else if (fieldName.startsWith(PREFIX_KEYBOARDHIDDEN)) {
            keyboardHiddenValues.put(f.getInt(null), fieldName);
          } else if (fieldName.startsWith(PREFIX_NAVIGATION)) {
            navigationValues.put(f.getInt(null), fieldName);
          } else if (fieldName.startsWith(PREFIX_NAVIGATIONHIDDEN)) {
            navigationHiddenValues.put(f.getInt(null), fieldName);
          } else if (fieldName.startsWith(PREFIX_ORIENTATION)) {
            orientationValues.put(f.getInt(null), fieldName);
          } else if (fieldName.startsWith(PREFIX_SCREENLAYOUT)) {
            screenLayoutValues.put(f.getInt(null), fieldName);
          } else if (fieldName.startsWith(PREFIX_TOUCHSCREEN)) {
            touchScreenValues.put(f.getInt(null), fieldName);
          } else if (fieldName.startsWith(PREFIX_UI_MODE)) {
            uiModeValues.put(f.getInt(null), fieldName);
          }
        } catch (@NonNull IllegalArgumentException | IllegalAccessException e) {
          Timber.w(e,"Error while inspecting device configuration");
        }
      }
    }

    valueArrays.put(PREFIX_HARDKEYBOARDHIDDEN, hardKeyboardHiddenValues);
    valueArrays.put(PREFIX_KEYBOARD, keyboardValues);
    valueArrays.put(PREFIX_KEYBOARDHIDDEN, keyboardHiddenValues);
    valueArrays.put(PREFIX_NAVIGATION, navigationValues);
    valueArrays.put(PREFIX_NAVIGATIONHIDDEN, navigationHiddenValues);
    valueArrays.put(PREFIX_ORIENTATION, orientationValues);
    valueArrays.put(PREFIX_SCREENLAYOUT, screenLayoutValues);
    valueArrays.put(PREFIX_TOUCHSCREEN, touchScreenValues);
    valueArrays.put(PREFIX_UI_MODE, uiModeValues);
    return valueArrays;
  }

  /**
   * Retrieve the name of the constant defined in the {@link Configuration}
   * class which defines the value of a field in a {@link Configuration}
   * instance.
   *
   * @param conf The instance of {@link Configuration} where the value is
   *             stored.
   * @param f    The {@link Field} to be inspected in the {@link Configuration}
   *             instance.
   * @return The value of the field f in instance conf translated to its
   * constant name.
   * @throws IllegalAccessException if the supplied field is inaccessible.
   */
  private static Object getFieldValueName(@NonNull Map<String, SparseArray<String>> valueArrays, @NonNull Configuration conf, @NonNull Field f) throws IllegalAccessException {
    final String fieldName = f.getName();
    switch (fieldName) {
      case FIELD_MCC:
      case FIELD_MNC:
        return f.getInt(conf);
      case FIELD_UIMODE:
        return activeFlags(valueArrays.get(PREFIX_UI_MODE), f.getInt(conf));
      case FIELD_SCREENLAYOUT:
        return activeFlags(valueArrays.get(PREFIX_SCREENLAYOUT), f.getInt(conf));
      default:
        final SparseArray<String> values = valueArrays.get(fieldName.toUpperCase(Locale.ROOT) + '_');
        if (values == null) {
          // Unknown field, return the raw int as String
          return f.getInt(conf);
        }

        final String value = values.get(f.getInt(conf));
        if (value == null) {
          // Unknown value, return the raw int as String
          return f.getInt(conf);
        }
        return value;
    }
  }

  /**
   * Some fields contain multiple value types which can be isolated by
   * applying a bitmask. That method returns the concatenation of active
   * values.
   *
   * @param valueNames The array containing the different values and names for this
   *                   field. Must contain mask values too.
   * @param bitfield   The bitfield to inspect.
   * @return The names of the different values contained in the bitfield,
   * separated by '+'.
   */
  @NonNull
  private static String activeFlags(@NonNull SparseArray<String> valueNames, int bitfield) {
    final StringBuilder result = new StringBuilder();

    // Look for masks, apply it an retrieve the masked value
    for (int i = 0; i < valueNames.size(); i++) {
      final int maskValue = valueNames.keyAt(i);
      if (valueNames.get(maskValue).endsWith(SUFFIX_MASK)) {
        final int value = bitfield & maskValue;
        if (value > 0) {
          if (result.length() > 0) {
            result.append('+');
          }
          result.append(valueNames.get(value));
        }
      }
    }
    return result.toString();
  }
}
