package org.totschnig.myexpenses.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.FontSizeAdapter;
import org.totschnig.myexpenses.preference.FontSizeDialogPreference;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.locale.UserLocaleProvider;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentActivity;
import butterknife.BindView;

public class OnboardingUiFragment extends OnboardingFragment {
  @BindView(R.id.font_size_display_name)
  TextView fontSizeDisplayNameTextView;
  @BindView(R.id.font_size)
  SeekBar fontSizeSeekBar;
  @BindView(R.id.theme)
  SwitchCompat themeSwitch;

  @Inject
  UserLocaleProvider userLocaleProvider;

  private int fontScale;

  public static OnboardingUiFragment newInstance() {
    return new OnboardingUiFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.onboarding_wizzard_ui;
  }

  @Override
  protected void configureView(@NonNull View view, Bundle savedInstanceState) {
    //fontsize
    fontScale = PrefKey.UI_FONTSIZE.getInt(0);
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

    //theme
    boolean isLight =  UiUtils.themeBoolAttr(getContext(), R.attr.isLightTheme);
    themeSwitch.setChecked(isLight);
    setContentDescriptonToThemeSwitch(themeSwitch, isLight);
    themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      PrefKey.UI_THEME_KEY.putString(
          (isChecked ? ProtectedFragmentActivity.ThemeType.light : ProtectedFragmentActivity.ThemeType.dark).name());
      recreate();
    });

    nextButton.setVisibility(View.VISIBLE);
  }

  @Override
  protected CharSequence getTitle() {
    return Utils.getTextWithAppName(getContext(), R.string.onboarding_ui_title);
  }

  private void onFontSizeSet() {
    int newValue = fontSizeSeekBar.getProgress();
    if (fontScale != newValue) {
      PrefKey.UI_FONTSIZE.putInt(newValue);
      recreate();
    }
  }

  private void recreate() {
    final FragmentActivity activity = getActivity();
    if (activity != null) {
      activity.recreate();
    }
  }

  private void updateFontSizeDisplayName(int fontScale) {
    final FragmentActivity activity = getActivity();
    if (activity != null) {
      fontSizeDisplayNameTextView.setText(FontSizeDialogPreference.getEntry(activity, fontScale));
      FontSizeAdapter.updateTextView(fontSizeDisplayNameTextView, fontScale, activity);
    }
  }

  private void setContentDescriptonToThemeSwitch(View themeSwitch, boolean isLight) {
    themeSwitch.setContentDescription(getString(
        isLight ? R.string.pref_ui_theme_light : R.string.pref_ui_theme_dark));
  }

}
