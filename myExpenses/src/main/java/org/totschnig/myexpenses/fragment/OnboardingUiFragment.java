package org.totschnig.myexpenses.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
    inflater.inflate(R.menu.onboarding_ui, menu);
    MenuItem menuItem = menu.findItem(R.id.language);
    View actionView = MenuItemCompat.getActionView(menuItem);
    String uiLanguage = PrefKey.UI_LANGUAGE.getString("default");
    ((TextView) actionView).setText(MyApplication.getInstance().getUserPreferedLocale().getLanguage());
    actionView.setOnClickListener(v -> {
      final PopupMenu subMenu = new PopupMenu(getActivity(), actionView);
      String[] entries = getResources().getStringArray(R.array.pref_ui_language_entries);
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
    View view = inflater.inflate(R.layout.onboarding_ui, container, false);

    //fontsize
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
    ((TextView) view.findViewById(R.id.onboarding_lead)).setText(R.string.onboarding_ui_title);
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
