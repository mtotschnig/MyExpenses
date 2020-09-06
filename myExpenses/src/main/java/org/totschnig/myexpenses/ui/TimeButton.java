package org.totschnig.myexpenses.ui;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.util.AttributeSet;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import androidx.annotation.NonNull;
import icepick.State;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;

public class TimeButton extends ButtonWithDialog {
  @State
  LocalTime time;

  DateTimeFormatter timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);

  public TimeButton(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void update() {
    requireTime();
    setText(time.format(timeFormatter));
  }

  @NonNull
  public LocalTime getTime() {
    requireTime();
    return time;
  }

  public void setTime(LocalTime time) {
    this.time = time;
    update();
  }

  @Override
  public Dialog onCreateDialog() {
    requireTime();
    TimePickerDialog.OnTimeSetListener timeSetListener =
        (view, hourOfDay, minute) -> {
          if (time.get(HOUR_OF_DAY) != hourOfDay ||
              time.get(MINUTE_OF_HOUR) != minute) {
            setTime(LocalTime.of(hourOfDay, minute));
            getHost().onValueSet(this);
          }
        };
    return new TimePickerDialog(getContext(),
        timeSetListener,
        time.getHour(),
        time.getMinute(),
        android.text.format.DateFormat.is24HourFormat(getContext())
    );
  }

  private void requireTime() {
    if (time == null) {
      time = LocalTime.now();
    }
  }
}
