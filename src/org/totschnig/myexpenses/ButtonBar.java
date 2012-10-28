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

import java.util.ArrayList;
import java.util.Iterator;

import org.example.qberticus.quickactions.BetterPopupWindow;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ButtonBar extends LinearLayout  {
  //this is where we add the buttons
  int orientation;

  public ButtonBar(Context context, AttributeSet attrs) {
    super(context,attrs);
    orientation = getResources().getConfiguration().orientation;
  }
  public MenuButton addButton(int text,int drawable,int id) {
    LayoutInflater inflater = LayoutInflater.from(getContext());
    MenuButton b = (MenuButton) inflater.inflate(R.layout.button, this, false);
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
  public static class MenuButton extends Button {
    
    private ArrayList<TextView> mItems;
    private BetterPopupWindow dw;

    public MenuButton(Context context, AttributeSet attrs) {
      super(context,attrs);
      mItems = new ArrayList<TextView>();
    }
    /**
     * @param text the resource id for retrieving the label for the entry
     * @param id is used for retrieving the command to be dispatched
     * @return the TextView added
     * adds an entry to the menu
     */
    public TextView addItem(int text,int id) {
      return addItem(getContext().getString(text),id);
    }
    /**
     * @param text the label for the entry
     * @param id is used for retrieving the command to be dispatched
     * @return the TextView added
     * adds an entry to the menu
     */
    public TextView addItem(String text,int id) {
      MyExpenses context = (MyExpenses) getContext();
      TextView tv = new TextView(context);
      tv.setId(id);
      tv.setText(text);
      tv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
      tv.setBackgroundResource(android.R.drawable.menuitem_background);
      tv.setOnClickListener(context);
      mItems.add(tv);
      return tv;
    }
    
    public void clearMenu() {
      mItems.clear();
      dw = null;
    }
    
    public BetterPopupWindow getMenu() {
      if (mItems.size() == 0)
        return null;
      if (dw == null) {
        dw = new BetterPopupWindow(this) {
          @Override
          protected void onCreate() {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            ViewGroup root = (ViewGroup) inflater.inflate(R.layout.popup_menu, null);
            for(Iterator<TextView> i = mItems.iterator();i.hasNext();)
            {
            root.addView(i.next());
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
