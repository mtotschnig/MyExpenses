package org.totschnig.myexpenses.retrofit;

import com.google.gson.annotations.SerializedName;

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
