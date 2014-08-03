package org.totschnig.myexpenses.provider.filter;

import android.os.Parcel;
import android.os.Parcelable;

public class TextCriteria extends Criteria {
  private String searchString;
  public TextCriteria(String title, String columnName, String searchString) {
    super(columnName, WhereFilter.Operation.LIKE,
        "%" +
            searchString
              .replace(WhereFilter.LIKE_ESCAPE_CHAR, WhereFilter.LIKE_ESCAPE_CHAR+ WhereFilter.LIKE_ESCAPE_CHAR)
              .replace("%", WhereFilter.LIKE_ESCAPE_CHAR+"%")
              .replace("_", WhereFilter.LIKE_ESCAPE_CHAR+"_") +
        "%");
    this.searchString = searchString;
    this.title = title;
  }
  public TextCriteria(Parcel in) {
   super(in);
   searchString = in.readString();
  }
  @Override
  public String prettyPrint() {
    return prettyPrintInternal(searchString);
  }
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(searchString);
  }
  public static final Parcelable.Creator<TextCriteria> CREATOR = new Parcelable.Creator<TextCriteria>() {
    public TextCriteria createFromParcel(Parcel in) {
        return new TextCriteria(in);
    }

    public TextCriteria[] newArray(int size) {
        return new TextCriteria[size];
    }
};
}
