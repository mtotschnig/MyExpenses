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
import org.w3c.dom.Node;
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

/**
 * SelectCategory activity allows to select categories while editing a transaction
 * and also managing (creating, deleting, importing)
 * @author Michael Totschnig
 *
 */
public class SelectCategory extends ExpandableListActivity {
    private MyExpandableListAdapter mAdapter;
    private ExpensesDbAdapter mDbHelper;
    private Cursor mGroupCursor;
    /**
     * create a new main category
     */
    private static final int CREATE_MAIN_CAT = Menu.FIRST;
    /**
     * create a new sub category
     */
    private static final int CREATE_SUB_CAT = Menu.FIRST+2;
    /**
     * return the main cat to the calling activity
     */
    private static final int SELECT_MAIN_CAT = Menu.FIRST+1;
    /**
     * edit the category label
     */
    private static final int EDIT_CAT = Menu.FIRST+3;
    /**
     * delete the category after checking if
     * there are mapped transactions or subcategories
     */
    private static final int DELETE_CAT = Menu.FIRST+4;
    /**
     * calls the category import
     */
    private static final int IMPORT_CAT_ID = Menu.FIRST + 5;
    int mGroupIdColumnIndex;
    ProgressDialog mProgressDialog;
    String sourceStr;
    private MyAsyncTask task=null;
    
    /**
     * Choice of sources for importing categories presented to the user.
     * The first four are internal to the app, the fourth one is provided by the user 
     */
    private final String[] IMPORT_SOURCES = {
        "Grisbi default (en)", 
        "Grisbi default (fr)", 
        "Grisbi default (de)", 
        "Grisbi default (it)", 
        "/sdcard/myexpenses/categories.xml"
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_category);
        setTitle(R.string.select_category);
        // Set up our adapter
        mDbHelper = MyApplication.db();
        mGroupCursor = mDbHelper.fetchCategoryMain();
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
        
        mProgressDialog = new ProgressDialog(SelectCategory.this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setProgress(0);
        
        task=(MyAsyncTask)getLastNonConfigurationInstance();
        
        if (task!=null) {
          task.attach(this);
          sourceStr = IMPORT_SOURCES[task.getSource()];
          mProgressDialog.setTitle(getString(R.string.categories_loading,sourceStr));
          mProgressDialog.setMax(task.getTotalCategories());
          mProgressDialog.show();
          updateProgress(task.getProgress());
          
          if (task.getStatus() == AsyncTask.Status.FINISHED) {
            markAsDone();
          }
        }
    }
    @Override
    public void onStop() {
      super.onStop();
      mProgressDialog.dismiss();
    }
    void updateProgress(int progress) {
      mProgressDialog.setProgress(progress);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

    	    ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
    	    int type = ExpandableListView
    	            .getPackedPositionType(info.packedPosition);
    	
    	    // Menu entries relevant only for the group
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
        menu.add(0, CREATE_MAIN_CAT, 0, R.string.menu_create_main_cat)
            .setIcon(android.R.drawable.ic_menu_add)
            .setAlphabeticShortcut('a');
        menu.add(0, IMPORT_CAT_ID,1,R.string.import_categories)
            .setIcon(R.drawable.squiggle)
            .setAlphabeticShortcut('b');
        return true;
    }
    
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case CREATE_MAIN_CAT:
            createCat(0);
            return true;
        case IMPORT_CAT_ID:
          importCategories();
          return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        long cat_id;
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {         
          Cursor childCursor = (Cursor) mAdapter.getChild(
              ExpandableListView.getPackedPositionGroup(info.packedPosition),
              ExpandableListView.getPackedPositionChild(info.packedPosition)
          );
          cat_id =  childCursor.getLong(childCursor.getColumnIndexOrThrow("_id"));
        } else  {
          cat_id = mGroupCursor.getLong(mGroupIdColumnIndex);
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
    				createCat(cat_id);
    				return true;
    			case EDIT_CAT:
    			  editCat(label,cat_id);
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
    /* (non-Javadoc)
     * return the sub cat to the calling activity 
     * @see android.app.ExpandableListActivity#onChildClick(android.widget.ExpandableListView, android.view.View, int, int, long)
     */
    @Override
    public boolean onChildClick (ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
    	//Log.w("SelectCategory","group = " + groupPosition + "; childPosition:" + childPosition);
    	Intent intent=new Intent();
    	long sub_cat = id;
    	Cursor childCursor = (Cursor) mAdapter.getChild(groupPosition,childPosition);
    	String label =  childCursor.getString(childCursor.getColumnIndexOrThrow("label"));
        intent.putExtra("cat_id",sub_cat);
        intent.putExtra("label", label);
        setResult(RESULT_OK,intent);
    	finish();
    	return true;
    }
    /**
     * Mapping the categories table into the ExpandableList
     * @author Michael Totschnig
     *
     */
    public class MyExpandableListAdapter extends SimpleCursorTreeAdapter2 {
    	
        public MyExpandableListAdapter(Cursor cursor, Context context, int groupLayout,
                int childLayout, String[] groupFrom, int[] groupTo, String[] childrenFrom,
                int[] childrenTo) {
            super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom,
                    childrenTo);
        }
        /* (non-Javadoc)
         * returns a cursor with the subcategories for the group
         * @see android.widget.CursorTreeAdapter#getChildrenCursor(android.database.Cursor)
         */
        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            // Given the group, we return a cursor for all the children within that group
        	long parent_id = groupCursor.getLong(mGroupIdColumnIndex);
        	Cursor itemsCursor = mDbHelper.fetchCategorySub(parent_id);
        	startManagingCursor(itemsCursor);
        	return itemsCursor;

        }
    }
    
    /**
     * presents AlertDialog for adding a new category
     * if label is already used, shows an error
     * @param parent_id
     */
    public void createCat(final long parent_id) {
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
    /**
     * presents AlertDialog for editing an existing category
     * if label is already used, shows an error
     * @param label
     * @param cat_id
     */
    public void editCat(String label, final long cat_id) {
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
    
    /**
     * Presents dialog for selecting an import source
     * and executes the import as an asyncTask
     */
    private void importCategories() {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(R.string.dialog_title_select_import_source);
      builder.setSingleChoiceItems(IMPORT_SOURCES, -1, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int item) {
            sourceStr = IMPORT_SOURCES[item];
            mProgressDialog.setTitle(getString(R.string.categories_loading,sourceStr));
            mProgressDialog.show();
            task = new MyAsyncTask(SelectCategory.this,item);
            task.execute();
            dialog.cancel();
          }
      });
      builder.show();
    }
    void markAsDone() {
      mProgressDialog.dismiss();
      String msg;
      int result = task.getTotalImported();
      if (result != -1) {
        msg = getString(R.string.import_categories_success,result);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        mGroupCursor.requery();
      }
      task = null;
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
      if (task != null)
        task.detach();
      return(task);
    }
    
    /**
     * This AsyncTaks has no input upon execution 
     * report an integer for showing a progress update
     * and gives no result
     * @author Michael Totschnig
     *
     */
    static class MyAsyncTask extends AsyncTask<Void, Integer, Void> {
      private SelectCategory activity;
      private int source;
      NodeList categories;
      NodeList sub_categories;
      InputStream catXML;
      Document dom;
      Element root;
      Hashtable<String,Long> Foreign2LocalIdMap;
      private int totalCategories;
      int totalImported;
      private ExpensesDbAdapter mDbHelper;
      int progress=0;
      String grisbiFileVersion;
      String mainElementName;
      String subElementName;

      /**
       * @param context
       * @param source Source for the import
       */
      public MyAsyncTask(SelectCategory activity,int source) {
        attach(activity);
        this.source = source;
        Foreign2LocalIdMap = new Hashtable<String,Long>();
        totalImported = -1;
        mDbHelper = MyApplication.db();
      }
      public Integer getSource() {
        return source;
      }
      public Integer getTotalImported() {
        return totalImported;
      }
      void attach(SelectCategory activity) {
        this.activity=activity;
      }
      void detach() {
        activity=null;
      }
      int getProgress() {
        return(progress);
      }
      /* (non-Javadoc)
       * loads the XML from the source, parses it, and sets up progress dialog
       * @see android.os.AsyncTask#onPreExecute()
       */
      protected void onPreExecute() {
        super.onPreExecute();
        //from sdcard
        if (source == 3) {
          try {
            catXML = new FileInputStream(activity.sourceStr);
          } catch (FileNotFoundException e) {
            Toast.makeText(activity, "Could not find file "+activity.sourceStr, Toast.LENGTH_LONG).show();
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
          catXML = activity.getResources().openRawResource(sourceRes);
        }
        try {
          DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
          DocumentBuilder builder = factory.newDocumentBuilder();
          dom = builder.parse(catXML);
        } catch (SAXParseException e) {
          Log.w("MyExpenses",e.getMessage());
          Toast.makeText(activity, "Could not parse file "+activity.sourceStr, Toast.LENGTH_LONG).show();
          cancel(false);
          return;
        } catch (Exception e) {
          Toast.makeText(activity, "An error occured: "+e.getMessage(), Toast.LENGTH_LONG).show();
          cancel(false);
          return;
        }
        //first we do the parsing
        root = dom.getDocumentElement();
        Node generalNode = root.getElementsByTagName("General").item(0);
        if (generalNode != null) {
          grisbiFileVersion = generalNode.getAttributes().getNamedItem("File_version").getNodeValue();
          Log.i("MyExpenses","found Grisbi version" + grisbiFileVersion);
        }
        else {
          Node versionNode = root.getElementsByTagName("Version_fichier_categ").item(0);
          if (versionNode != null) {
            //unfortunately we have to retrieve the first text node in order to get at the value
            grisbiFileVersion = versionNode.getChildNodes().item(0).getNodeValue();
            Log.i("MyExpenses","found Grisbi version" + grisbiFileVersion);
          }
          else {
            Log.i("MyExpenses","did not find Grisbi version, assuming 0.6.0");
            //we are mapping the existence of Categorie to 0.5.0
            //and Category to 0.6.0
            if (root.getElementsByTagName("Category").getLength() > 0)
              grisbiFileVersion = "0.6.0";
            else if (root.getElementsByTagName("Categorie").getLength() > 0)
              grisbiFileVersion = "0.5.0";
            else {
              Toast.makeText(activity, "Unable to determine Grisbi file version", Toast.LENGTH_LONG).show();
              cancel(false);
            }
              
          }
        }
        if (grisbiFileVersion.equals("0.6.0")) {
          mainElementName = "Category";
          subElementName = "Sub_category";
        } else if (grisbiFileVersion.equals("0.5.0")) {
          mainElementName = "Categorie";
          subElementName = "Sous-categorie";
        } else {
          Toast.makeText(activity, "Unsupported Grisbi File Version: "+grisbiFileVersion, Toast.LENGTH_LONG).show();
          cancel(false);
        }
      }
      protected void onCancelled() {
        if (activity==null) {
          Log.w("MyAsyncTask", "onCancelled() skipped -- no activity");
        } else {
          activity.markAsDone();
        }
      }
      /* (non-Javadoc)
       * updates the progress dialog
       * @see android.os.AsyncTask#onProgressUpdate(Progress[])
       */
      protected void onProgressUpdate(Integer... values) {
        progress = values[0];
        if (activity==null) {
          Log.w("MyAsyncTask", "onProgressUpdate() skipped -- no activity");
        }
        else {
          activity.updateProgress(progress);
        }
      }
      /* (non-Javadoc)
       * reports on success (with total number of imported categories) or failure
       * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
       */
      protected void onPostExecute(Void unused) {
        if (activity==null) {
          Log.w("MyAsyncTask", "onPostExecute() skipped -- no activity");
        }
        else {
          activity.markAsDone();
        }
      }


      /* (non-Javadoc)
       * this is where the bulk of the work is done via calls to {@link #importCatsMain()}
       * and {@link #importCatsSub()}
       * sets up {@link #categories} and {@link #sub_categories}
       * @see android.os.AsyncTask#doInBackground(Params[])
       */
      @Override
      protected Void doInBackground(Void... params) {
        totalImported = 0;
        categories = root.getElementsByTagName(mainElementName);
        sub_categories = root.getElementsByTagName(subElementName);
        totalCategories = categories.getLength() + sub_categories.getLength();
        activity.mProgressDialog.setMax(totalCategories);

        if (grisbiFileVersion.equals("0.6.0")) {
          importCatsMain();
          importCatsSub();
        } else {
          importCats050();
        }
        return(null);
      }
      private void importCats050() {
        int count = 0;
        String label;
        long main_id, sub_id;
        NamedNodeMap atts;
        
        for (int i=0;i<categories.getLength();i++){
          count++;
          atts = categories.item(i).getAttributes();
          label = atts.getNamedItem("Nom").getNodeValue();
          main_id = mDbHelper.createCategory(label,0);
          if (main_id != -1) {
            totalImported++;
          } else {
            //this should not happen
            Log.w("MyExpenses","could neither retrieve nor store main category " + label);
            continue;
          }
          if (count % 10 == 0) {
            publishProgress(count);
          }
          NodeList children = categories.item(i).getChildNodes();
          for (int j=0;j<children.getLength();j++){
            Node child = children.item(j);
            String nodeName = child.getNodeName();
            if ((nodeName != null) & (nodeName.equals(subElementName))) {
              count++;
              atts = child.getAttributes();
              label = atts.getNamedItem("Nom").getNodeValue();
              sub_id = mDbHelper.createCategory(label,main_id);
              if (sub_id != -1) {
                totalImported++;
              } else {
                //this should not happen
                Log.w("MyExpenses","could neither retrieve nor store main category " + label);
              }
              if (count % 10 == 0) {
                publishProgress(count);
              }
            }
          }
        }
      }
      /**
       * iterates over {@link #categories}
       * maintains a map of the ids found in the XML and the ones inserted in the database
       * needed for mapping subcats to their parents in {@link #importCatsSub()}
       */
      private void importCatsMain() {
        int start = 1;
        String label;
        String id;
        long _id;

        for (int i=0;i<categories.getLength();i++){
          NamedNodeMap category = categories.item(i).getAttributes();
          label = category.getNamedItem("Na").getNodeValue();
          id =  category.getNamedItem("Nb").getNodeValue();
          _id = mDbHelper.getCategoryId(label, 0);
          if (_id != -1) {
            Foreign2LocalIdMap.put(id, _id);
          } else {
            _id = mDbHelper.createCategory(label,0);
            if (_id != -1) {
              Foreign2LocalIdMap.put(id, _id);
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
      /**
       * iterates over {@link #sub_categories}
       */
      private void importCatsSub() {
        int start = categories.getLength() + 1;
        String label;
        //String id;
        String parent_id;
        Long mapped_parent_id;
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
            _id = mDbHelper.createCategory(label, mapped_parent_id);
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
      int getTotalCategories() {
        return totalCategories;
      }
      void setTotalCategories(int totalCategories) {
        this.totalCategories = totalCategories;
      }
    }
  }