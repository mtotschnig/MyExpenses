/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.example.qberticus.quickactions.BetterPopupWindow;
import org.totschnig.myexpenses.activity.MyExpenses;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;

public class ButtonBar extends LinearLayout  {
  //this is where we add the buttons
  int orientation;
  static int buttonBackgroundNormalId;
  static int buttonBackgroundPopupId;


  public ButtonBar(Context context, AttributeSet attrs) {
    super(context,attrs);
    orientation = getResources().getConfiguration().orientation;
    Resources.Theme theme = getContext().getTheme();
    TypedValue backgroundId = new TypedValue();
    theme.resolveAttribute(R.attr.buttonBackgroundNormal, backgroundId, true);
    buttonBackgroundNormalId = backgroundId.resourceId;
    theme.resolveAttribute(R.attr.buttonBackgroundPopup, backgroundId, true);
    buttonBackgroundPopupId = backgroundId.resourceId;
  }
  public MenuButton addButton(int text,int drawable,int id) {
    LayoutInflater inflater = LayoutInflater.from(getContext());
    MenuButton b = (MenuButton) inflater.inflate(R.layout.button, this, false);
    b.setBackgroundResourceKeepPadding(buttonBackgroundNormalId);
    b.setText(text);
    b.setId(id);
    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
      b.setCompoundDrawablesWithIntrinsicBounds(drawable,0, 0,0);
    } else {
      b.setCompoundDrawablesWithIntrinsicBounds(0, drawable,0, 0);
    }
    addView(b);
    b.setOnClickListener((MyExpenses) getContext());
    b.setOnLongClickListener((MyExpenses) getContext());
    return b;
  }
  /**
   * encapsulates an Action to be executed from the button if long clicked
   * this Action can be bound either to a textview in the popup window
   * or to a dialog if there is not enough place in the popup window
   */
  public static class Action implements Serializable {
    private static final long serialVersionUID = 1L;
    public int id;
    public String text;
    public Object tag;
    public Action(int id, String text, Object tag) {
      this.id = id;
      this.text = text;
      this.tag = tag;
    }
  }
  public static class MenuButton extends Button {
    
    private ArrayList<Action> mItems;
    private BetterPopupWindow dw;
    private Comparator<Button> comparator;

    public MenuButton(Context context, AttributeSet attrs) {
      super(context,attrs);
      mItems = new ArrayList<Action>();
    }
    /**
     * @param text the resource id for retrieving the label for the entry
     * @param id is used for retrieving the command to be dispatched
     * @return the TextView added
     * adds an entry to the menu
     */
    public void addItem(int text,int id, Object tag) {
      addItem(getContext().getString(text),id,tag);
    }
    /**
     * adds an entry to the menu
     * @param text the label for the entry
     * @param id is used for retrieving the command to be dispatched
     * @param tag extra information the dispatched command will need
     */
    public void addItem(String text,int id, Object tag) {
      if (mItems.isEmpty()) {
        setBackgroundResourceKeepPadding(buttonBackgroundPopupId);
      }
      mItems.add(new Action(id,text,tag));
    }
    public void addItem(int text,int id) {
      addItem(text,id,null);
    }
    public void addItem(String text,int id) {
       addItem(text,id,null);
    }
    //setBackgroundResource
    private void setBackgroundResourceKeepPadding(int res) {
      int bottom = getPaddingBottom();
      int top = getPaddingTop();
      int right = getPaddingRight();
      int left = getPaddingLeft();
      setBackgroundResource(res);
      setPadding(left, top, right, bottom); 
    }
    
    public void clearMenu() {
      mItems.clear();
      dw = null;
      setBackgroundResourceKeepPadding(buttonBackgroundNormalId);
    }
    public void setComparator(Comparator<Button> comparator) {
      this.comparator  = comparator;
    }
    
    
    /**
     * @param height how much place do we have for the popup window
     * @return
     */
    public BetterPopupWindow getMenu(final int height) {
      final int size = mItems.size();
      if (size == 0)
        return null;
      if (dw == null) {
        dw = new BetterPopupWindow(this) {
          @Override
          protected void onCreate() {
            int heightLeft = height;
            int heightNeeded = 0;
            int count = 0;
            Button tv;
            ArrayList<Button> buttons = new  ArrayList<Button>();
            MyExpenses context = (MyExpenses) getContext();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            LinearLayout root = (LinearLayout) inflater.inflate(R.layout.popup_menu, null);
            for(Iterator<Action> i = mItems.iterator();i.hasNext();)
            {
              if (heightNeeded > heightLeft && (size - count) > 1 ) {
                //Log.i("BetterPopupWindow","Out of space: stopping");
                tv = new Button(context,null,R.attr.menuItemStyle);
                tv.setId(R.id.MORE_ACTION_COMMAND);
                tv.setText("More...");
                //we transmit the remaining items to the more command as tag
                ArrayList<Action> remainingItems = new ArrayList<Action>();
                while (i.hasNext()) {
                  remainingItems.add(i.next());
                }
                //remainingItems are always sorted alphabetically
                Collections.sort(remainingItems,new Comparator<Action>() {
                  public int compare(Action a, Action b) {
                    return a.text.compareToIgnoreCase(b.text);
                  }
                });
                tv.setTag(remainingItems);
                //tv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
                tv.setOnClickListener(context);
                buttons.add(tv);
                //root.addView(tv,0);
                break;
              }
              Action action = i.next();
              tv = new Button(context,null,R.attr.menuItemStyle);
              tv.setId(action.id);
              tv.setText(action.text);
              tv.setTag(action.tag);
              //tv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
              //we measure only the first item which is always added
              if (heightLeft == height){
                tv.measure(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT);
                heightNeeded = tv.getMeasuredHeight();
              }
              heightLeft -= heightNeeded;
              //Log.i("BetterPopupWindow","Height Left is now: " + heightLeft);
              tv.setOnClickListener(context);
              tv.setOnLongClickListener(context);
              //tv.setFocusableInTouchMode(true);
              buttons.add(tv);
              count++;
            }
            if (comparator != null) {
              Collections.sort(buttons,comparator);
            }
            for(Iterator<Button> i = buttons.iterator();i.hasNext();) {
              root.addView(i.next(),0);
            }
            // set the inflated view as what we want to display
            this.setContentView(root);
          }
        };
      }
      return dw;
    }
  }
}
