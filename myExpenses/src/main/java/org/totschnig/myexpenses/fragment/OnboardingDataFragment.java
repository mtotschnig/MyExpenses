package org.totschnig.myexpenses.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.totschnig.myexpenses.R;

public class OnboardingDataFragment extends Fragment {
  public static OnboardingDataFragment newInstance() {
    return new OnboardingDataFragment();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.onboarding_data, container, false);
  }
}
