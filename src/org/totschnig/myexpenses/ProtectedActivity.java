package org.totschnig.myexpenses;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ProtectedActivity extends Activity {
  private boolean locked = false;
  private Dialog pwDialog;
  @Override
  protected void onPause() {
    super.onPause();
    if (locked)
      pwDialog.dismiss();
    else {
      MyApplication app = MyApplication.getInstance();
      if (!(app.passwordHash.equals("")))
      app.mLastPause = System.nanoTime();
    }
  }
  @Override
  protected void onResume() {
    super.onResume();
    MyApplication app = MyApplication.getInstance();
    if (!app.passwordHash.equals("") && System.nanoTime() - app.mLastPause > 5000) {
      locked = true;
      LayoutInflater li = LayoutInflater.from(this);
      View view = li.inflate(R.layout.password_check, null);
      final EditText input = (EditText) view.findViewById(R.id.password);
      final TextView error = (TextView) view.findViewById(R.id.passwordInvalid);
      view.findViewById(R.id.POSITIVE_BUTTON).setVisibility(View.INVISIBLE);
      view.findViewById(R.id.NEGATIVE_BUTTON).setVisibility(View.INVISIBLE);
      pwDialog = new AlertDialog.Builder(this)
        .setTitle("Enter your password")
        .setView(view)
        .setCancelable(false)
        .create();
      Button btn = (Button) view.findViewById(R.id.NEUTRAL_BUTTON);
      btn.setText(android.R.string.ok);
      btn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          String value = input.getText().toString();
          if (Utils.md5(value).equals(MyApplication.getInstance().passwordHash)) {
            input.setText("");
            locked = false;
            pwDialog.dismiss();
          } else {
            error.setText("Password invalid. Try again");
          }
        }
      });
      pwDialog.show();
      WindowManager.LayoutParams lp = pwDialog.getWindow().getAttributes();  
      lp.dimAmount=1.0f;  
      pwDialog.getWindow().setAttributes(lp);
    }
  }
}
