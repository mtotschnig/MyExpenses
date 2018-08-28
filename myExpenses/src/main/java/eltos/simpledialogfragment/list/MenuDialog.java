package eltos.simpledialogfragment.list;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.MenuRes;
import android.support.v7.view.menu.MenuBuilder;
import android.view.MenuItem;

import java.util.ArrayList;

public class MenuDialog extends SimpleListDialog {
  public static final String TAG = "MenuDialog.";

  public static MenuDialog build(){
    return new MenuDialog();
  }

  @SuppressLint("RestrictedApi")
  public SimpleListDialog menu(Activity activity, @MenuRes int menuResId) {
    final MenuBuilder menu = new MenuBuilder(activity);
    activity.getMenuInflater().inflate(menuResId, menu);
    final int size = menu.size();
    ArrayList<SimpleListItem> list = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      MenuItem menuItem = menu.getItem(i);
      list.add(new SimpleListItem(menuItem.getTitle().toString(), menuItem.getItemId()));
    }
    return items(list).choiceMode(SINGLE_CHOICE);
  }
}
