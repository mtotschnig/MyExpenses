package org.totschnig.myexpenses.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.ui.animation.ExpandAnimation;

import androidx.annotation.Nullable;

public class ExpansionPanel extends LinearLayout {
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
}
