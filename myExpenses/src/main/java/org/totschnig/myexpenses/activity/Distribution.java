package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.DistributionFragment;
import org.totschnig.myexpenses.model.Grouping;

import androidx.annotation.NonNull;

public class Distribution extends CategoryActivity<DistributionFragment> {
  public static final String ACTION_DISTRIBUTION = "DISTRIBUTION";
  private GestureDetector mDetector;
  private static final int SWIPE_MIN_DISTANCE = 120;
  private static final int SWIPE_MAX_OFF_PATH = 250;
  private static final int SWIPE_THRESHOLD_VELOCITY = 100;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    DisplayMetrics dm = getResources().getDisplayMetrics();

    final int REL_SWIPE_MIN_DISTANCE = (int) (SWIPE_MIN_DISTANCE * dm.densityDpi / 160.0f);
    final int REL_SWIPE_MAX_OFF_PATH = (int) (SWIPE_MAX_OFF_PATH * dm.densityDpi / 160.0f);
    final int REL_SWIPE_THRESHOLD_VELOCITY = (int) (SWIPE_THRESHOLD_VELOCITY * dm.densityDpi / 160.0f);
    mDetector = new GestureDetector(this,
        new GestureDetector.SimpleOnGestureListener() {
          @Override
          public boolean onFling(MotionEvent e1, MotionEvent e2,
                                 float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;
            if (Math.abs(e1.getY() - e2.getY()) > REL_SWIPE_MAX_OFF_PATH)
              return false;
            if (e1.getX() - e2.getX() > REL_SWIPE_MIN_DISTANCE
                && Math.abs(velocityX) > REL_SWIPE_THRESHOLD_VELOCITY) {
              mListFragment.forward();
              return true;
            } else if (e2.getX() - e1.getX() > REL_SWIPE_MIN_DISTANCE
                && Math.abs(velocityX) > REL_SWIPE_THRESHOLD_VELOCITY) {
              mListFragment.back();
              return true;
            }
            return false;
          }
        });
  }

  @NonNull
  @Override
  public String getAction() {
    return ACTION_DISTRIBUTION;
  }

  @Override
  protected int getContentView() {
    return R.layout.activity_distribution;
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    if (mDetector != null)
      if (!mListFragment.getGrouping().equals(Grouping.NONE) && mDetector.onTouchEvent(event)) {
        return true;
      }
    // Be sure to call the superclass implementation
    return super.dispatchTouchEvent(event);
  }
}
