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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.SparseArray;

import org.acra.ACRA;
import org.json.JSONException;
import org.json.JSONObject;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.HelpDialogFragment;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.licence.LicenceStatus;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.acra.ACRA.LOG_TAG;
import static org.totschnig.myexpenses.util.DistribHelper.getMarketSelfUri;
import static org.totschnig.myexpenses.util.DistribHelper.getVersionInfo;



public class CommonCommands {
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
  private CommonCommands() {
  }

  public static boolean dispatchCommand(ProtectedFragmentActivity ctx, int command, Object tag) {
    Intent i;
    switch (command) {
      case R.id.RATE_COMMAND:
        i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(getMarketSelfUri()));
        if (Utils.isIntentAvailable(ctx, i)) {
          ctx.startActivity(i);
        } else {
          ctx.showSnackbar(R.string.error_accessing_market, Snackbar.LENGTH_LONG);
        }
        return true;
      case R.id.SETTINGS_COMMAND:
        i = new Intent(ctx, MyPreferenceActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (tag != null) {
          i.putExtra(MyPreferenceActivity.KEY_OPEN_PREF_KEY, (String) tag);
        }
        ctx.startActivityForResult(i, ProtectedFragmentActivity.PREFERENCES_REQUEST);
        return true;
      case R.id.FEEDBACK_COMMAND: {
        LicenceHandler licenceHandler = MyApplication.getInstance().getLicenceHandler();
        LicenceStatus licenceStatus = licenceHandler.getLicenceStatus();
        String licenceInfo = "";
        if (licenceStatus != null) {
          licenceInfo = "\nLICENCE: " + licenceStatus.name();
          String purchaseExtraInfo = licenceHandler.getPurchaseExtraInfo();
          if (!TextUtils.isEmpty(purchaseExtraInfo)) {
            licenceInfo += " (" + purchaseExtraInfo + ")";
          }
        }
        i = new Intent(android.content.Intent.ACTION_SEND);
        i.setType("plain/text");
        i.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{MyApplication.FEEDBACK_EMAIL});
        i.putExtra(android.content.Intent.EXTRA_SUBJECT,
            "[" + ctx.getString(R.string.app_name) + "] Feedback"
        );
        String messageBody = String.format(Locale.ROOT,
            "APP_VERSION:%s\nANDROID_VERSION:%s\nBRAND:%s\nMODEL:%s\nCONFIGURATION:%s%s\n\n",
            getVersionInfo(ctx),
            Build.VERSION.RELEASE,
            Build.BRAND,
            Build.MODEL,
            configToJson(ctx.getResources().getConfiguration()),
            licenceInfo);
        i.putExtra(android.content.Intent.EXTRA_TEXT, messageBody);
        if (!Utils.isIntentAvailable(ctx, i)) {
          ctx.showSnackbar(R.string.no_app_handling_email_available, Snackbar.LENGTH_LONG);
        } else {
          ctx.startActivity(i);
        }
        break;
      }
      case R.id.CONTRIB_INFO_COMMAND:
        CommonCommands.showContribDialog(ctx, null, null);
        return true;
      case R.id.WEB_COMMAND:
        i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(ctx.getString(R.string.website)));
        ctx.startActivity(i);
        return true;
      case R.id.HELP_COMMAND:
        i = new Intent(ctx, Help.class);
        i.putExtra(HelpDialogFragment.KEY_VARIANT,
            tag != null ? (Enum<?>) tag : ctx.helpVariant);
        //for result is needed since it allows us to inspect the calling activity
        ctx.startActivityForResult(i, 0);
        return true;
      case R.id.REQUEST_LICENCE_MIGRATION_COMMAND:
        LicenceHandler licenceHandler = MyApplication.getInstance().getLicenceHandler();
        String androidId = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        i = new Intent(android.content.Intent.ACTION_SEND);
        i.setType("plain/text");
        i.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{MyApplication.FEEDBACK_EMAIL});
        i.putExtra(android.content.Intent.EXTRA_SUBJECT,
            "[" + ctx.getString(R.string.app_name) + "] " +  ctx.getString(licenceHandler.getLicenceStatus().getResId()));
        String extraText = String.format(
            "Please send me a new licence key. Current key is %1$s for Android-Id %2$s\nLANGUAGE:%3$s\nVERSION:%4$s",
            PrefKey.LICENCE_LEGACY.getString(null), androidId,
            Locale.getDefault().toString(), DistribHelper.getVersionInfo(ctx));
        i.putExtra(android.content.Intent.EXTRA_TEXT, extraText);
        if (!Utils.isIntentAvailable(ctx, i)) {
          ctx.showSnackbar(R.string.no_app_handling_email_available, Snackbar.LENGTH_LONG);
        } else {
          ctx.startActivity(i);
        }
        return true;
      case android.R.id.home:
        ctx.setResult(FragmentActivity.RESULT_CANCELED);
        ctx.finish();
        return true;
    }
    return false;
  }

  public static void showContribDialog(Activity ctx, @Nullable ContribFeature feature, Serializable tag) {
    Intent i = ContribInfoDialogActivity.getIntentFor(ctx, feature);
    i.putExtra(ContribInfoDialogActivity.KEY_TAG, tag);
    ctx.startActivityForResult(i, ProtectedFragmentActivity.CONTRIB_REQUEST);
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
  private static JSONObject configToJson(@NonNull Configuration conf) {
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
            ACRA.log.w(LOG_TAG, "Could not collect configuration field " + fieldName, e);
          }
        }
      } catch (@NonNull IllegalArgumentException e) {
        ACRA.log.e(LOG_TAG, "Error while inspecting device configuration: ", e);
      } catch (@NonNull IllegalAccessException e) {
        ACRA.log.e(LOG_TAG, "Error while inspecting device configuration: ", e);
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
        } catch (@NonNull IllegalArgumentException e) {
          ACRA.log.w(LOG_TAG, "Error while inspecting device configuration: ", e);
        } catch (@NonNull IllegalAccessException e) {
          ACRA.log.w(LOG_TAG, "Error while inspecting device configuration: ", e);
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
        final SparseArray<String> values = valueArrays.get(fieldName.toUpperCase() + '_');
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
