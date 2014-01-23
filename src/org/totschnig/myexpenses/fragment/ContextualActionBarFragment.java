package org.totschnig.myexpenses.fragment;

import java.util.Locale;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;


/**
 * @author Michael Totschnig
 *  provide helper functionality to create a CAB for a ListView
 *  below HoneyComb a context menu is used instead
 */
public class ContextualActionBarFragment extends Fragment {
  private int menuResource;
  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    String className = this.getClass().getSimpleName().toLowerCase(Locale.US);
    String resourceName = className+"_context";
    menuResource = getResources().getIdentifier(resourceName, "menu", activity.getPackageName());
  }
  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    if (!getUserVisibleHint())
      return false;
    int itemId = item.getItemId();
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    if (item.getGroupId()==R.id.MenuGroupSingleOnly) {
      return dispatchCommandSingle(itemId,info);
    } else {
      SparseBooleanArray sba = new SparseBooleanArray();
      sba.put(info.position, true);
      return dispatchCommandMultiple(itemId,sba,new Long[]{info.id});
    }
  }
  public boolean dispatchCommandSingle(int command, AdapterContextMenuInfo info) {
    ProtectedFragmentActivity ctx = (ProtectedFragmentActivity) getActivity();
    return ctx.dispatchCommand(command, info);
  }
  public boolean dispatchCommandMultiple(int command, SparseBooleanArray positions,Long[]itemIds) {
    ProtectedFragmentActivity ctx = (ProtectedFragmentActivity) getActivity();
    //we send only the positions to the default dispatch command mechanism,
    //but subclasses can provide a method that handles the itemIds
    return ctx.dispatchCommand(command, positions);
  }
  private void inflateHelper(Menu menu) {
    MenuInflater inflater = getActivity().getMenuInflater();
    inflater.inflate(R.menu.common_context,menu);
    if (menuResource!=0)
      inflater.inflate(menuResource, menu);
  }
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    inflateHelper(menu);
    super.onCreateContextMenu(menu, v, menuInfo);
  }
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public void registerForContextualActionBar(final ListView lv) {
    if (Build.VERSION.SDK_INT >= 11) {
      lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
      lv.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position,
                                              long id, boolean checked) {
          int count = lv.getCheckedItemCount();
          mode.setTitle(String.valueOf(count));
          mode.getMenu().setGroupVisible(R.id.MenuGroupSingleOnly,count==1);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
          inflateHelper(menu);
          int count = lv.getCheckedItemCount();
          mode.setTitle(String.valueOf(count));
          menu.setGroupVisible(R.id.MenuGroupSingleOnly,count==1);
          return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
          // TODO Auto-generated method stub
          return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
          int itemId = item.getItemId();
          SparseBooleanArray checkedItemPositions = lv.getCheckedItemPositions();
          boolean result = false;
          if (checkedItemPositions != null) {
            if (item.getGroupId()==R.id.MenuGroupSingleOnly) {
              for (int i=0; i<checkedItemPositions.size(); i++) {
                if (checkedItemPositions.valueAt(i)) {
                  int position = checkedItemPositions.keyAt(i);
                  long id = lv.getItemIdAtPosition(position);
                  View v = lv.getChildAt(position);
                  result = dispatchCommandSingle(itemId,new AdapterContextMenuInfo(v,position,id));
                }
              }
            } else {
              long[] itemIdsPrim = lv.getCheckedItemIds();
              int itemIdsLength = itemIdsPrim.length;
              Long[] itemIdsObj = new Long[itemIdsLength];
              for(int i = 0; i < itemIdsLength; ++i){
                itemIdsObj[i] = itemIdsPrim[i];
             }
              result = dispatchCommandMultiple(
                  itemId,
                  checkedItemPositions,
                  itemIdsObj);
            }
          }
          mode.finish();
          return result;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
          // TODO Auto-generated method stub
          
        }
      });
    } else {
      registerForContextMenu(lv);
    }
  }
}
