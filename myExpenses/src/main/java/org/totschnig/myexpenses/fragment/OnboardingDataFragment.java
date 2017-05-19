package org.totschnig.myexpenses.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.totschnig.myexpenses.R;

public class OnboardingDataFragment extends Fragment {

  private View moreOptionsContainer;

  public static OnboardingDataFragment newInstance() {
    return new OnboardingDataFragment();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.onboarding_data, container, false);
    //lead
    ((TextView) view.findViewById(R.id.onboarding_lead)).setText(R.string.onboarding_data_title);

    //hide Calculator
    view.findViewById(R.id.Calculator).setVisibility(View.GONE);

    moreOptionsContainer = view.findViewById(R.id.MoreOptionsContainer);
    return view;
  }

  public void showMoreOptions(View view) {
    view.setVisibility(View.GONE);
    moreOptionsContainer.setVisibility(View.VISIBLE);
  }
}
