package org.totschnig.myexpenses.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

/**
 * CAUTION: this could break, if the id of the message text view
 * is changed
 */
public class ScrollableProgressDialog extends ProgressDialog {

  public ScrollableProgressDialog(Context context) {
    super(context);
  }
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((TextView) this.findViewById(android.R.id.message)).setMovementMethod(new ScrollingMovementMethod());
  }
}
