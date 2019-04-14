package org.totschnig.myexpenses.ui;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.widget.CalendarView;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.Utils;

import java.util.Locale;

import icepick.State;

/**
 * A button that opens DateDialog, and stores the date in its state
 */
public class DateButton extends ButtonWithDialog {
  @State
  LocalDate date;
  private DateTimeFormatter dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);

  public Dialog onCreateDialog() {
    requireDate();
    boolean brokenSamsungDevice = isBrokenSamsungDevice();
    final Context base = getContext();
    @SuppressLint("InlinedApi")
    Context context = brokenSamsungDevice ?
        new ContextThemeWrapper(base,
            base instanceof ProtectedFragmentActivity && ((ProtectedFragmentActivity) base).getThemeType() == ProtectedFragmentActivity.ThemeType.dark ?
                android.R.style.Theme_Holo_Dialog : android.R.style.Theme_Holo_Light_Dialog) :
        base;
    int yearOld = date.getYear();
    int monthOld = date.getMonthValue() - 1;
    int dayOld = date.getDayOfMonth();
    /**
     * listens on changes in the date dialog and sets the date on the button
     */
    DatePickerDialog.OnDateSetListener mDateSetListener =
        (view, year, monthOfYear, dayOfMonth) -> {
          if (yearOld != year ||
              monthOld != monthOfYear  ||
              dayOld != dayOfMonth) {
            setDate(LocalDate.of(year, monthOfYear + 1, dayOfMonth));
            getHost().onValueSet(this);
          }
        };
    DatePickerDialog datePickerDialog = new DatePickerDialog(context, mDateSetListener,
        yearOld, monthOld, dayOld);
    if (brokenSamsungDevice) {
      datePickerDialog.setTitle("");
      datePickerDialog.updateDate(yearOld, monthOld, dayOld);
    }
    if (PrefKey.GROUP_WEEK_STARTS.isSet()) {
      int startOfWeek = Utils.getFirstDayOfWeekFromPreferenceWithFallbackToLocale(Locale.getDefault());
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        datePickerDialog.getDatePicker().setFirstDayOfWeek(startOfWeek);
      } else {
        try {
          setFirstDayOfWeek(datePickerDialog, startOfWeek);
        } catch (UnsupportedOperationException e) {/*Nothing left to do*/}
      }
    }

    return datePickerDialog;
  }

  private void requireDate() {
    if (date == null) {
      date = LocalDate.now();
    }
  }

  private void setFirstDayOfWeek(DatePickerDialog datePickerDialog, int startOfWeek) {
    CalendarView calendarView = datePickerDialog.getDatePicker().getCalendarView();
    calendarView.setFirstDayOfWeek(startOfWeek);
  }

  private static boolean isBrokenSamsungDevice() {
    return (Build.MANUFACTURER.equalsIgnoreCase("samsung")
        && isBetweenAndroidVersions(
        Build.VERSION_CODES.LOLLIPOP,
        Build.VERSION_CODES.LOLLIPOP_MR1));
  }

  private static boolean isBetweenAndroidVersions(int min, int max) {
    return Build.VERSION.SDK_INT >= min && Build.VERSION.SDK_INT <= max;
  }

  public void setDate(LocalDate localDate) {
    date = localDate;
    update();
  }

  @NonNull
  public LocalDate getDate() {
    requireDate();
    return date;
  }

  public DateButton(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void update() {
    requireDate();
    setText(date.format(dateFormat));
  }
}
