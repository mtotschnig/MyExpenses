package org.totschnig.myexpenses.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.setupwizardlib.SetupWizardLayout;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.activity.SplashActivity;
import org.totschnig.myexpenses.adapter.FontSizeAdapter;
import org.totschnig.myexpenses.preference.FontSizeDialogPreference;
import org.totschnig.myexpenses.preference.PrefKey;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OnboardingUiFragment extends OnboardingFragment {
  @BindView(R.id.font_size_display_name)
  TextView fontSizeDisplayNameTextView;
  @BindView(R.id.font_size)
  SeekBar fontSizeSeekBar;
  @BindView(R.id.setup_wizard_layout)
  SetupWizardLayout setupWizardLayout;

  private int fontScale;

  public static OnboardingUiFragment newInstance() {
    return new OnboardingUiFragment();
  }

  @Override
  protected void onNextButtonClicked() {
    ((SplashActivity) getActivity()).navigate_next();
  }

  @Override
  protected void createMenu(Toolbar toolbar) {
    toolbar.inflateMenu(R.menu.onboarding_ui);
    MenuItem menuItem = toolbar.getMenu().findItem(R.id.language);
    View actionView = MenuItemCompat.getActionView(menuItem);
    String uiLanguage = PrefKey.UI_LANGUAGE.getString("default");
    ((TextView) actionView).setText(MyApplication.getInstance().getUserPreferedLocale().getLanguage());
    actionView.setOnClickListener(v -> {
      final PopupMenu subMenu = new PopupMenu(getActivity(), actionView);
      String[] entries = SettingsFragment.getLocaleArray(getContext());
      for (int i = 0; i < entries.length; i++) {
        subMenu.getMenu().add(Menu.NONE, i, Menu.NONE, entries[i]);
      }
      subMenu.setOnMenuItemClickListener(item -> {
        String[] values = getResources().getStringArray(R.array.pref_ui_language_values);
        String newValue = values[item.getItemId()];
        if (!uiLanguage.equals(newValue)) {
          PrefKey.UI_LANGUAGE.putString(newValue);
          recreate();
        }
        return true;
      });
      subMenu.show();
    });
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.onboarding_wizzard_1, container, false);
    ButterKnife.bind(this, view);

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
    SwitchCompat themeSwitch = (SwitchCompat) view.findViewById(R.id.theme);
    boolean isLight = MyApplication.getThemeType().equals(MyApplication.ThemeType.light);
    themeSwitch.setChecked(isLight);
    setContentDescriptonToThemeSwitch(themeSwitch, isLight);
    themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      PrefKey.UI_THEME_KEY.putString(
          (isChecked ? MyApplication.ThemeType.light : MyApplication.ThemeType.dark).name());
      recreate();
    });

    //lead
    setupWizardLayout.setHeaderText(R.string.onboarding_ui_title);
    setupWizardLayout.setIllustration(R.drawable.bg_setup_header, R.drawable.bg_header_horizontal_tile);

    configureNavigation(view, inflater, R.id.suw_navbar_next);

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
