/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import com.ozdroid.adapter.SimpleCursorTreeAdapter2;

import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.SimpleCursorTreeAdapter.ViewBinder;

public class SelectCategory extends ExpandableListActivity {
    private MyExpandableListAdapter mAdapter;
    private ExpensesDbAdapter mDbHelper;
    private Cursor mGroupCursor;
    private static final int CREATE_MAIN_CAT = Menu.FIRST;
    private static final int CREATE_SUB_CAT = Menu.FIRST+2;
    private static final int SELECT_MAIN_CAT = Menu.FIRST+1;
    private static final int EDIT_CAT = Menu.FIRST+3;
    private static final int DELETE_CAT = Menu.FIRST+4;
    private static final int IMPORT_CAT_ID = Menu.FIRST + 5;
    int mGroupIdColumnIndex;
    ProgressDialog mProgressDialog;
    int mTotalCategories;
    private final String[] ITEMS = {"Grisbi default (en)", "Grisbi default (fr)", "Grisbi default (de)", "/sdcard/myexpenses/categories.xml"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_category);
        setTitle(R.string.select_category);
        // Set up our adapter
        mDbHelper = new ExpensesDbAdapter(this);
        mDbHelper.open();
        mGroupCursor = mDbHelper.fetchCategoryMainUnionTransfer();
        startManagingCursor(mGroupCursor);

        // Cache the ID column index
        mGroupIdColumnIndex = mGroupCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_ROWID);

        // Set up our adapter
        mAdapter = new MyExpandableListAdapter(mGroupCursor,
                this,
                android.R.layout.simple_expandable_list_item_1,
                android.R.layout.simple_expandable_list_item_1,
                new String[]{"label"},
                new int[] {android.R.id.text1},
                new String[] {"label"},
                new int[] {android.R.id.text1});

        setListAdapter(mAdapter);
        registerForContextMenu(getExpandableListView());
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

    	    ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
    	    int type = ExpandableListView
    	            .getPackedPositionType(info.packedPosition);
    	
    	    // Only create a context menu for the group
    	    if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
    	    	menu.add(0,SELECT_MAIN_CAT,0,R.string.select_parent_category);
    	    	menu.add(0,CREATE_SUB_CAT,0,R.string.menu_create_sub_cat);
    	    }
    	    menu.add(0,DELETE_CAT,0,R.string.menu_delete_cat);
    	    menu.add(0,EDIT_CAT,0,R.string.menu_edit_cat);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, CREATE_MAIN_CAT, 0, R.string.menu_create_main_cat);
        menu.add(0, IMPORT_CAT_ID,1,R.string.import_categories);
        return true;
    }
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case CREATE_MAIN_CAT:
            createCat("0");
            return true;
        case IMPORT_CAT_ID:
          importCategories();
          return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int cat_id;
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {         
          Cursor childCursor = (Cursor) mAdapter.getChild(
              ExpandableListView.getPackedPositionGroup(info.packedPosition),
              ExpandableListView.getPackedPositionChild(info.packedPosition)
          );
          cat_id =  childCursor.getInt(childCursor.getColumnIndexOrThrow("_id"));
        } else  {
            cat_id = mGroupCursor.getInt(mGroupIdColumnIndex);
        }
        String label =   ((TextView) info.targetView).getText().toString();
        
    		switch(item.getItemId()) {
    			case SELECT_MAIN_CAT:  	
    	    	Intent intent=new Intent();		    	
    	      intent.putExtra("cat_id", cat_id);
    	      intent.putExtra("label", label);
    	      setResult(RESULT_OK,intent);
    	    	finish();
    	      return true;
    			case CREATE_SUB_CAT:
    				createCat(String.valueOf(cat_id));
    				return true;
    			case EDIT_CAT:
    			  editCat(label,String.valueOf(cat_id));
    			  return true;
    			case DELETE_CAT:
    			  if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP && mDbHelper.getCategoryCountSub(cat_id) > 0) {
    			    Toast.makeText(this,getString(R.string.not_deletable_subcats_exists), Toast.LENGTH_LONG).show();
    			  } else if (mDbHelper.getExpenseCount(cat_id) > 0 ) {
    			    Toast.makeText(this,getString(R.string.not_deletable_mapped_expenses), Toast.LENGTH_LONG).show();
    			  } else {
    			    mDbHelper.deleteCategory(cat_id);
    			    mGroupCursor.requery();
    			  }
        }
    		return false;
    	}

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	mDbHelper.close();
    }
    @Override
    public boolean onChildClick (ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
    	//Log.w("SelectCategory","group = " + groupPosition + "; childPosition:" + childPosition);
    	Intent intent=new Intent();
    	int sub_cat = (int) id;
    	Cursor childCursor = (Cursor) mAdapter.getChild(groupPosition,childPosition);
    	String label =  childCursor.getString(childCursor.getColumnIndexOrThrow("label"));
        intent.putExtra("cat_id",sub_cat);
        intent.putExtra("label", label);
        setResult(RESULT_OK,intent);
    	finish();
    	return true;
    }
    public class MyExpandableListAdapter extends SimpleCursorTreeAdapter2 {
    	
        public MyExpandableListAdapter(Cursor cursor, Context context, int groupLayout,
                int childLayout, String[] groupFrom, int[] groupTo, String[] childrenFrom,
                int[] childrenTo) {
            super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom,
                    childrenTo);
        }
        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            // Given the group, we return a cursor for all the children within that group
        	String parent_id = groupCursor.getString(groupIdColumnIndex);
        	Cursor itemsCursor = mDbHelper.fetchCategorySub(parent_id);
        	startManagingCursor(itemsCursor);
        	return itemsCursor;

        }
    }
    public void createCat(final String parent_id) {
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);
    	alert.setTitle(R.string.create_category);

    	// Set an EditText view to get user input 
    	final EditText input = new EditText(this);
    	alert.setView(input);

    	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    	public void onClick(DialogInterface dialog, int whichButton) {
    	  String value = input.getText().toString();
    	  if (mDbHelper.createCategory(value,parent_id) != -1) {
    		  mGroupCursor.requery();
    		  //mAdapter.notifyDataSetChanged();
    	  } else {
    		  Toast.makeText(SelectCategory.this,getString(R.string.category_already_defined, value), Toast.LENGTH_LONG).show();
    	  }
          //getExpandableListView().invalidateViews();
    	  }
    	});

    	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    	  public void onClick(DialogInterface dialog, int whichButton) {
    		  dialog.dismiss();
    	  }
    	});

    	alert.show();
    }
    public void editCat(String label, final String cat_id) {
      AlertDialog.Builder alert = new AlertDialog.Builder(this);
      alert.setTitle(R.string.edit_category);

      // Set an EditText view to get user input 
      final EditText input = new EditText(this);
      input.setText(label);
      alert.setView(input);

      alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        String value = input.getText().toString();
        if (mDbHelper.updateCategoryLabel(value,cat_id) != -1) {
          mGroupCursor.requery();
          //mAdapter.notifyDataSetChanged();
        } else {
          Toast.makeText(SelectCategory.this,getString(R.string.category_already_defined, value), Toast.LENGTH_LONG).show();
        }
          //getExpandableListView().invalidateViews();
        }
      });

      alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          dialog.dismiss();
        }
      });
      
      alert.show();
    }
    private void importCategories() {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle("Pick a source for import");
      builder.setSingleChoiceItems(ITEMS, -1, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int item) {
            new MyAsyncTask(SelectCategory.this,item).execute();
            dialog.cancel();
          }
      });
      builder.show();
    }
    private class MyAsyncTask extends AsyncTask<Void, Integer, Integer> {
      private Context context;
      private int source;
      NodeList categories;
      NodeList sub_categories;
      InputStream catXML;
      Document dom;
      Hashtable<String,String> Foreign2LocalIdMap;
      int totalImported;

      public MyAsyncTask(Context context,int source) {
        this.context = context;
        this.source = source;
        Foreign2LocalIdMap = new Hashtable<String,String>();
        totalImported = 0; 
      }
      protected void onPreExecute() {
        String sourceStr = ITEMS[source];
        super.onPreExecute();
        //from sdcard
        if (source == 3) {
          try {
            catXML = new FileInputStream(sourceStr);
          } catch (FileNotFoundException e) {
            Toast.makeText(context, "Could not find file "+sourceStr, Toast.LENGTH_LONG).show();
            cancel(false);
            return;
          }
        } else {
          int sourceRes = 0;
          switch(source) {
          case 0:
            sourceRes = R.raw.cat_en;
            break;
          case 1:
            sourceRes = R.raw.cat_fr;
            break;
          case 2:
            sourceRes = R.raw.cat_de;
            break;
          }
          catXML = context.getResources().openRawResource(sourceRes);
        }
        try {
          DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
          DocumentBuilder builder = factory.newDocumentBuilder();
          dom = builder.parse(catXML);
        } catch (SAXParseException e) {
          Log.w("MyExpenses",e.getMessage());
          Toast.makeText(context, "Could not parse file "+sourceStr, Toast.LENGTH_LONG).show();
          cancel(false);
          return;
        } catch (Exception e) {
          Toast.makeText(context, "An error occured: "+e.getMessage(), Toast.LENGTH_LONG).show();
          cancel(false);
          return;
        }
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setTitle(getString(R.string.categories_loading,sourceStr));
        mProgressDialog.setProgress(0);
        mProgressDialog.show();
      }
      protected void onProgressUpdate(Integer... values) {
        mProgressDialog.setProgress(values[0]);
      }
      protected void onPostExecute(Integer result) {
        mProgressDialog.dismiss();
        String msg;
        super.onPostExecute(result);
        if (result == -1) {
          msg = getString(R.string.import_categories_failure);
        } else {
          msg = getString(R.string.import_categories_success,result);
        }
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        mGroupCursor.requery();
      }


      @Override
      protected Integer doInBackground(Void... params) {
        //first we do the parsing
        Element root = dom.getDocumentElement();
        categories = root.getElementsByTagName("Category");
        sub_categories = root.getElementsByTagName("Sub_category");
        mTotalCategories = categories.getLength() + sub_categories.getLength();
        mProgressDialog.setMax(mTotalCategories);

        mDbHelper.open();

        importCatsMain();
        importCatsSub();
        return totalImported;
      }
      private void importCatsMain() {
        int start = 1;
        String label;
        String id;
        long _id;

        for (int i=0;i<categories.getLength();i++){
          NamedNodeMap category = categories.item(i).getAttributes();
          label = category.getNamedItem("Na").getNodeValue();
          id =  category.getNamedItem("Nb").getNodeValue();
          _id = mDbHelper.getCategoryId(label, "0");
          if (_id != -1) {
            Foreign2LocalIdMap.put(id, String.valueOf(_id));
          } else {
            _id = mDbHelper.createCategory(label,"0");
            if (_id != -1) {
              Foreign2LocalIdMap.put(id, String.valueOf(_id));
              totalImported++;
            } else {
              //this should not happen
              Log.w("MyExpenses","could neither retrieve nor store main category " + label);
            }
          }
          if ((start+i) % 10 == 0) {
            publishProgress(start+i);
          }
        }
      }
      private void importCatsSub() {
        int start = categories.getLength() + 1;
        String label;
        //String id;
        String parent_id;
        String mapped_parent_id;
        long _id;
        for (int i=0;i<sub_categories.getLength();i++){
          NamedNodeMap sub_category = sub_categories.item(i).getAttributes();
          label = sub_category.getNamedItem("Na").getNodeValue();
          //id =  sub_category.getNamedItem("Nb").getNodeValue();
          parent_id = sub_category.getNamedItem("Nbc").getNodeValue();
          mapped_parent_id = Foreign2LocalIdMap.get(parent_id);
          //TODO: for the moment, we do not deal with subcategories,
          //if we were not able to import a matching category
          //should check if the main category exists, but the subcategory is new
          if (mapped_parent_id != null) {
            _id = mDbHelper.createCategory(label, Foreign2LocalIdMap.get(parent_id));
            if (_id != -1) {
              totalImported++;
            }
          } else {
            Log.w("MyExpenses","could not store sub category " + label);
          }
          if ((start+i) % 10 == 0) {
            publishProgress(start+i);
          }
        }
      }
    }
  }