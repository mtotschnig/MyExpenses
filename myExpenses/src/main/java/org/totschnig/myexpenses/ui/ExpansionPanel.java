package org.totschnig.myexpenses.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.ui.animation.ExpandAnimation;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ExpansionPanel extends LinearLayout {

  private static final int ROTATION_EXPANDED = 0;
  private static final int ROTATION_COLLAPSED = 180;

  public interface Listener {
    void onExpansionStateChanged(boolean expanded);
  }
  private int contentVisibility;
  private boolean isMeasured;
  @BindView(R.id.headerIndicator)
  View headerIndicator;
  @BindView(R.id.expansionContent)
  View expansionContent;
  @Nullable
  @BindView(R.id.expansionTrigger)
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
    ButterKnife.bind(this);
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
      Animator animator = ObjectAnimator.ofFloat(headerIndicator, View.ROTATION, visible ? ROTATION_COLLAPSED : ROTATION_EXPANDED);
      animator.addListener(new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
          updateIndicatorContentDescritpion();
          if (listener != null) {
            listener.onExpansionStateChanged(!visible);
          }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
      });
      animator.start();
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
    headerIndicator.setRotation(expansionContent.getVisibility() == VISIBLE ? ROTATION_EXPANDED : ROTATION_COLLAPSED);
    updateIndicatorContentDescritpion();
  }

  private void updateIndicatorContentDescritpion() {
    headerIndicator.setContentDescription(
        getResources().getString(headerIndicator.getRotation() == ROTATION_EXPANDED ?
            R.string.content_description_collapse : R.string.content_description_expand));
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
