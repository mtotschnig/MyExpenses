package org.totschnig.myexpenses.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.OnboardingActivity;
import org.totschnig.myexpenses.databinding.OnboardingWizzardBinding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public abstract class OnboardingFragment extends Fragment {
  private OnboardingWizzardBinding binding;
  View nextButton;
  protected Toolbar toolbar;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    binding = OnboardingWizzardBinding.inflate(inflater, container, false);
    configureNavigation(binding.getRoot(), inflater);
    binding.onboardingContent.setLayoutResource(getLayoutResId());
    bindView(binding.onboardingContent.inflate());
    binding.setupWizardLayout.setHeaderText(getTitle());
    binding.setupWizardLayout.setIllustration(R.drawable.bg_setup_header, R.drawable.bg_header_horizontal_tile);
    return binding.getRoot();
  }

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    configureView(savedInstanceState);
  }

  protected abstract CharSequence getTitle();

  private void configureNavigation(View content, LayoutInflater inflater) {
    final ViewGroup navParent = (ViewGroup) content.findViewById(R.id.suw_layout_navigation_bar)
        .getParent();
    View customNav = inflater.inflate(R.layout.onboarding_navigation, navParent, false);
    toolbar = customNav.findViewById(R.id.onboarding_menu);
    final int menuResId = getMenuResId();
    if (menuResId != 0) {
      toolbar.inflateMenu(menuResId);
      setupMenu();
    }
    nextButton = customNav.findViewById(getNavigationButtonId());
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

  protected int getNavigationButtonId() {
    return R.id.suw_navbar_next;
  }

  protected abstract void configureView(@Nullable Bundle savedInstanceState);

  protected abstract int getLayoutResId();

  abstract void bindView(@NonNull View view);

  protected int getMenuResId() {
    return 0;
  }

  protected void onNextButtonClicked() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof OnboardingActivity) {
      ((OnboardingActivity) activity).navigate_next();
    }
  }

  protected void setupMenu() {
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
