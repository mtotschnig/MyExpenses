package org.totschnig.myexpenses.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import androidx.appcompat.widget.PopupMenu;
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
  protected int getMenuResId() {
    return R.menu.onboarding_ui;
  }

  @Override
  protected void setupMenu() {
    MenuItem menuItem = toolbar.getMenu().findItem(R.id.language);
    View actionView = menuItem.getActionView();
    String uiLanguage = PrefKey.UI_LANGUAGE.getString("default");
    ((TextView) actionView).setText(userLocaleProvider.getUserPreferredLocale().getLanguage());
    actionView.setOnClickListener(v -> {
      final Context context = getContext();
      final PopupMenu subMenu;
      if (context != null) {
        subMenu = new PopupMenu(context, actionView);
        String[] entries = SettingsFragment.getLocaleArray(context);
        for (int i = 0; i < entries.length; i++) {
          subMenu.getMenu().add(Menu.NONE, i, Menu.NONE, entries[i]);
        }
        String[] values = getResources().getStringArray(R.array.pref_ui_language_values);
        subMenu.setOnMenuItemClickListener(item -> {
          String newValue = values[item.getItemId()];
          if (!uiLanguage.equals(newValue)) {
            PrefKey.UI_LANGUAGE.putString(newValue);
            recreate();
          }
          return true;
        });
        subMenu.show();
      }
    });
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
