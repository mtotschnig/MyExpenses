package org.totschnig.myexpenses.activity;


import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.totschnig.myexpenses.R;

public class SimpleImageActivity extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    setContentView(R.layout.simple_image);
    Picasso.get().load(getIntent().getData()).fit().into(((ImageView) findViewById(R.id.imageView)));
  }
}