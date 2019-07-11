package org.totschnig.myexpenses.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.View;

import androidx.appcompat.widget.PopupMenu;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class PopupMenuPreference extends Preference {
  private View anchorView;
  public PopupMenuPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public PopupMenuPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public PopupMenuPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public PopupMenuPreference(Context context) {
    super(context);
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {
    super.onBindViewHolder(holder);
    anchorView = holder.itemView;
  }

  public void showPopupMenu(PopupMenu.OnMenuItemClickListener listener, String... items) {
    PopupMenu popup = new PopupMenu(getContext(), anchorView);
    Menu popupMenu = popup.getMenu();
    popup.setOnMenuItemClickListener(listener);
    for (int i = 0; i < items.length; i++) {
      popupMenu.add(Menu.NONE, i, Menu.NONE, items[i]);
    }
    popup.show();
  }
}
