package org.totschnig.myexpenses.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.totschnig.myexpenses.R;

import androidx.annotation.IdRes;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

abstract class OnboardingFragment extends Fragment {
  protected View nextButton;
  protected Toolbar toolbar;
  protected void configureNavigation(View content, LayoutInflater inflater, @IdRes int buttonToShow) {
    final ViewGroup navParent = (ViewGroup) content.findViewById(R.id.suw_layout_navigation_bar)
        .getParent();
    View customNav = LayoutInflater.from(navParent.getContext()).inflate(R.layout.onboarding_navigation,
        navParent, false);
    toolbar = customNav.findViewById(R.id.onboarding_menu);
    toolbar.inflateMenu(getMenuResId());
    setupMenu();
    nextButton = customNav.findViewById(buttonToShow);
    nextButton.setOnClickListener(v -> onNextButtonClicked());

    // Swap our custom navigation bar into place
    for (int i = 0; i < navParent.getChildCount(); i++) {
      if (navParent.getChildAt(i).getId() == R.id.suw_layout_navigation_bar) {
        navParent.removeViewAt(i);
        navParent.addView(customNav, i);
        break;
      }
    }
  }

  protected abstract int getMenuResId();

  protected abstract void onNextButtonClicked();

  protected abstract void setupMenu();
}
