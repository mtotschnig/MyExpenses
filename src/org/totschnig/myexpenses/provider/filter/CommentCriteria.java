package org.totschnig.myexpenses.provider.filter;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import android.os.Parcel;
import android.os.Parcelable;

public class CommentCriteria extends TextCriteria {
  public CommentCriteria(String searchString) {
    super(MyApplication.getInstance().getString(R.string.comment),DatabaseConstants.KEY_COMMENT,searchString);
  }
  public CommentCriteria(Parcel in) {
   super(in);
  }
  public static final Parcelable.Creator<CommentCriteria> CREATOR = new Parcelable.Creator<CommentCriteria>() {
    public CommentCriteria createFromParcel(Parcel in) {
        return new CommentCriteria(in);
    }

    public CommentCriteria[] newArray(int size) {
        return new CommentCriteria[size];
    }
  };
  public static CommentCriteria fromStringExtra(String extra) {
    return new CommentCriteria(extra);
  }
}
