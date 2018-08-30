package org.totschnig.myexpenses.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.ui.animation.ExpandAnimation;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ExpansionPanel extends LinearLayout {
  @BindView(R.id.headerIndicator)
  View headerIndicator;
  @BindView(R.id.expansionContent)
  View expansionContent;

  public ExpansionPanel(Context context) {
    super(context);
  }

  public ExpansionPanel(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public ExpansionPanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public ExpansionPanel(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.bind(this);
    updateIndicator();
    headerIndicator.setOnClickListener(v -> {
      final boolean visible = expansionContent.getVisibility() == VISIBLE;
      Animator animator = ObjectAnimator.ofFloat(headerIndicator, View.ROTATION, visible ? 180 : 0);
      animator.start();
      //if android:animateLayoutChanges is true we go with the default animation,
      //which works well, unless we are in a list view
      if (getLayoutTransition() == null) {
        ExpandAnimation expandAni = new ExpandAnimation(expansionContent, 250);
        expansionContent.startAnimation(expandAni);
      } else {
        expansionContent.setVisibility(visible ? GONE : VISIBLE);
      }
    });
  }

  private void updateIndicator() {
    headerIndicator.setRotation(expansionContent.getVisibility() == VISIBLE ? 0 : 180);
  }

  public void setContentVisibility(int visibility) {
    expansionContent.setVisibility(visibility);
    updateIndicator();
  }
}
