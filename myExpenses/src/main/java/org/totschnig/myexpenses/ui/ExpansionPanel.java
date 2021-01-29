package org.totschnig.myexpenses.ui;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.ui.animation.ExpandAnimation;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class ExpansionPanel extends LinearLayout {
  private int contentVisibility;
  private boolean isMeasured;
  ExpansionHandle headerIndicator;
  View expansionContent;
  @Nullable
  View expansionTrigger;

  public void setListener(@Nullable Listener listener) {
    this.listener = listener;
  }

  @Nullable
  Listener listener;

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
    expansionTrigger = findViewById(R.id.expansionTrigger);
    expansionContent = findViewById(R.id.expansionContent);
    headerIndicator = findViewById(R.id.headerIndicator);
    updateIndicator();
    if (hasNoDefaultTransition()) {
      expansionContent.getViewTreeObserver().addOnGlobalLayoutListener(
          new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
              expansionContent.getViewTreeObserver().removeGlobalOnLayoutListener(this);
              isMeasured = true;
              if (contentVisibility == GONE) {
                final int height = expansionContent.getHeight();
                final LayoutParams layoutParams = (LayoutParams) expansionContent.getLayoutParams();
                layoutParams.bottomMargin = -height;
                expansionContent.setLayoutParams(layoutParams);
              }
              expansionContent.setVisibility(contentVisibility);
              updateIndicator();
            }
          });
    }
    View trigger = expansionTrigger != null ? expansionTrigger : headerIndicator;
    trigger.setOnClickListener(v -> {
      final boolean visible = expansionContent.getVisibility() == VISIBLE;
      headerIndicator.rotate(visible, listener);
      //if android:animateLayoutChanges is true we go with the default animation,
      //which works well, unless we are in a list view
      if (hasNoDefaultTransition()) {
        ExpandAnimation expandAni = new ExpandAnimation(expansionContent, 250);
        expansionContent.startAnimation(expandAni);
      } else {
        expansionContent.setVisibility(visible ? GONE : VISIBLE);
      }
    });
  }

  private boolean hasNoDefaultTransition() {
    return getLayoutTransition() == null;
  }

  private void updateIndicator() {
    headerIndicator.setExpanded(expansionContent.getVisibility() == VISIBLE);
  }

  public void setContentVisibility(int visibility) {
    this.contentVisibility = visibility;
    if (isMeasured) {
      expansionContent.setVisibility(visibility);
      final LayoutParams layoutParams = (LayoutParams) expansionContent.getLayoutParams();
      layoutParams.bottomMargin = visibility == VISIBLE ? 0 : -expansionContent.getHeight();
      expansionContent.setLayoutParams(layoutParams);
    }
  }
}
