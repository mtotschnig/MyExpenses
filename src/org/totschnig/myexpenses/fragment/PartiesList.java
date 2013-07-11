package org.totschnig.myexpenses.fragment;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.TransactionProvider;

import com.actionbarsherlock.app.SherlockFragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class PartiesList extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  SimpleCursorAdapter mAdapter;
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.parties_list, null, false);
    
    final ListView lv = (ListView) v.findViewById(R.id.list);
    lv.setItemsCanFocus(false);
    lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    //((TextView) findViewById(android.R.id.empty)).setText(R.string.no_parties);
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{"name"};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{android.R.id.text1};

    // Now create a simple cursor adapter and set it to display
    mAdapter = new SimpleCursorAdapter(getActivity(), 
        android.R.layout.simple_list_item_1, null, from, to,0);

    getLoaderManager().initLoader(0, null, this);
    lv.setAdapter(mAdapter);
    lv.setEmptyView(v.findViewById(R.id.empty));
    registerForContextMenu(lv);
/*    Button deleteButton = (Button) v.findViewById(R.id.deleteItems);
    deleteButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        int cntChoice = lv.getCount();
        SparseBooleanArray sparseBooleanArray = lv.getCheckedItemPositions();

        for(int i = 0; i < cntChoice; i++){
          if(sparseBooleanArray.get(i)) {
            Payee.delete(lv.getAdapter().getItemId(i));
          }
        }
      }
    });
*/    return v;
  }
  @Override
  public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
    CursorLoader cursorLoader = new CursorLoader(getActivity(),
        TransactionProvider.PAYEES_URI, null, null,null, null);
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
    mAdapter.swapCursor(c);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    mAdapter.swapCursor(null);
  }
}
