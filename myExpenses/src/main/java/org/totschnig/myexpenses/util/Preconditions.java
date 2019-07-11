package org.totschnig.myexpenses.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static java.lang.String.format;

public class Preconditions {

  @NonNull
  public static <T> T checkNotNull(@Nullable T reference) {
    return dagger.internal.Preconditions.checkNotNull(reference);
  }

  @NonNull
  public static <T> T checkNotNull(@Nullable T reference, String errorMessage) {
    return dagger.internal.Preconditions.checkNotNull(reference, errorMessage);
  }

  public static void checkArgument(boolean expression) {
    if (!expression) {
      throw new IllegalArgumentException();
    }
  }

  public static void checkArgument(String argLabel, Object expected, Object actual) {
    if (!expected.equals(actual)) {
      throw new IllegalArgumentException(String.format("Expected %s to be %s, got %s",
          argLabel, expected, actual));
    }
  }

  public static void checkArgument(boolean expression, @Nullable Object errorMessage) {
    if (!expression) {
      throw new IllegalArgumentException(String.valueOf(errorMessage));
    }
  }

  public static void checkState(boolean expression) {
    if (!expression) {
      throw new IllegalStateException();
    }
  }

  public static void checkState(boolean expression, @Nullable Object errorMessage) {
    if (!expression) {
      throw new IllegalStateException(String.valueOf(errorMessage));
    }
  }

  public static void checkState(boolean expression, String errorMessageTemplate, Object... errorMessageArgs) {
    if (!expression) {
      throw new IllegalStateException(format(errorMessageTemplate, errorMessageArgs));
    }
  }
}