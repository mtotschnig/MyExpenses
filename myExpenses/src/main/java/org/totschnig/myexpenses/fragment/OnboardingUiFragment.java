package org.totschnig.myexpenses.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.FontSizeAdapter;
import org.totschnig.myexpenses.preference.FontSizeDialogPreference;
import org.totschnig.myexpenses.preference.PrefKey;

public class OnboardingUiFragment extends Fragment {

  private TextView fontSizeDisplayNameTextView;
  private int fontScale;
  private SeekBar fontSizeSeekBar;

  public static OnboardingUiFragment newInstance() {
    return new OnboardingUiFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    MenuItemCompat.setShowAsAction(menu.add("EN"), MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.onboarding_ui, container, false);
    fontSizeDisplayNameTextView = ((TextView) view.findViewById(R.id.font_size_display_name));
    fontScale = PrefKey.UI_FONTSIZE.getInt(0);
    fontSizeSeekBar = (SeekBar) view.findViewById(R.id.font_size);
    fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        updateFontSizeDisplayName(progress);
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        onFontSizeSet();
      }
    });
    fontSizeSeekBar.setProgress(fontScale);
    fontSizeSeekBar.setOnFocusChangeListener((v, hasFocus) -> {
      if (!hasFocus) {
        onFontSizeSet();
      }
    });

    updateFontSizeDisplayName(fontScale);
    SwitchCompat themeSwitch = (SwitchCompat) view.findViewById(R.id.theme);
    boolean isLight = MyApplication.getThemeType().equals(MyApplication.ThemeType.light);
    themeSwitch.setChecked(isLight);
    setContentDescriptonToThemeSwitch(themeSwitch, isLight);
    themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      PrefKey.UI_THEME_KEY.putString(
          (isChecked ? MyApplication.ThemeType.light : MyApplication.ThemeType.dark).name());
      recreate();
    });
    return view;
  }

  private void onFontSizeSet() {
    int newValue = fontSizeSeekBar.getProgress();
    if (fontScale != newValue) {
      PrefKey.UI_FONTSIZE.putInt(newValue);
      recreate();
    }
  }

  private void recreate() {
    ((ProtectedFragmentActivity) getActivity()).recreateBackport();
  }

  private void updateFontSizeDisplayName(int fontScale) {
    fontSizeDisplayNameTextView.setText(FontSizeDialogPreference.getEntry(getActivity(), fontScale));
    FontSizeAdapter.updateTextView(fontSizeDisplayNameTextView, fontScale, getActivity());
  }

  private void setContentDescriptonToThemeSwitch(View themeSwitch, boolean isLight) {
    themeSwitch.setContentDescription(getString(
        isLight ? R.string.pref_ui_theme_light : R.string.pref_ui_theme_dark));
  }

}
