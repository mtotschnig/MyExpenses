package org.totschnig.myexpenses.retrofit;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.Keep;

@Keep
public class Issue {
  @SerializedName("title")
  private String title;

  @SerializedName("number")
  private int number;

  public String getTitle() {
    return title;
  }

  public int getNumber() {
    return number;
  }
}
