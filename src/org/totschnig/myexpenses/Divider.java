package org.totschnig.myexpenses;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class Divider extends View {

  public Divider(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.setBackgroundColor(MyApplication.getCurrentAccountColor());
    // TODO Auto-generated constructor stub
  }
}
