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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class GrisbiImport extends Activity implements DialogInterface.OnClickListener {
  static final int SOURCES_DIALOG_ID = 1;
  ProgressDialog mProgressDialog;
  private AlertDialog mSourcesDialog;
  //String sourceStr;
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
      "/sdcard/myexpenses/grisbi.xml"
  };
  /**
   * stores the index of the source the user has selected
   */
  public int sourceIndex;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mProgressDialog = new ProgressDialog(this);
    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    mProgressDialog.setProgress(0);
    mProgressDialog.setCancelable(false);
    
    mSourcesDialog = new AlertDialog.Builder(this)
    .setTitle(R.string.dialog_title_select_import_source)
    .setCancelable(false)
    .setSingleChoiceItems(IMPORT_SOURCES, -1, this)
    .setNegativeButton(R.string.button_cancel, this)
    .setNeutralButton(R.string.grisbi_import_button_categories_only,this)
    .setPositiveButton(R.string.grisbi_import_button_categories_and_parties, this)
    .create();
    
    task=(MyAsyncTask)getLastNonConfigurationInstance();
    
    if (task!=null) {
      task.attach(this);
      sourceIndex = task.getSource();
      mProgressDialog.setTitle(task.getTitle());
      mProgressDialog.setMax(task.getMax());
      mProgressDialog.show();
      updateProgress(task.getProgress());
      
      if (task.getStatus() == AsyncTask.Status.FINISHED) {
        markAsDone();
      }
    } else if (savedInstanceState == null) {
      showDialog(SOURCES_DIALOG_ID);
      mSourcesDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
      mSourcesDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
    }
  }
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
    case SOURCES_DIALOG_ID:
      return mSourcesDialog;
      }
    return null;
  }
  @Override
  public void onStop() {
    super.onStop();
    mProgressDialog.dismiss();
  }
  void updateProgress(int progress) {
    mProgressDialog.setProgress(progress);
  }

  void markAsDone() {
    mProgressDialog.dismiss();
    String msg;
    int result = task.getTotalImportedCat();
    if (result != -1) {
      if (task.partiesAreToBeHandledP) {
        msg = getString(R.string.grisbi_import_categories_and_parties_success,result, task.getTotalImportedParty());
      } else {
        msg = getString(R.string.grisbi_import_categories_success,result);
      }
      Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
    task = null;
    finish();
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
    private GrisbiImport activity;
    private int source;
    NodeList categories, sub_categories, parties;
    InputStream catXML;
    Document dom;
    Element root;
    Hashtable<String,Long> Foreign2LocalIdMap;
    private int max;
    int totalImportedCat;
    int totalImportedParty;
    private ExpensesDbAdapter mDbHelper;
    int progress=0;
    String grisbiFileVersion;
    String mainElementName;
    String subElementName;
    /**
     * should we handle parties as well?
     */
    boolean partiesAreToBeHandledP = true;
    /**
     * this is set when we finish one phase (parsing, importing categories, importing parties)
     * so that we can adapt progress dialog in onProgressUpdate
     */
    boolean phaseChangedP = false;
    private String title;

    /**
     * @param context
     * @param source Source for the import
     */
    public MyAsyncTask(GrisbiImport activity,int source) {
      attach(activity);
      this.source = source;
      Foreign2LocalIdMap = new Hashtable<String,Long>();
      totalImportedCat = -1;
      mDbHelper = MyApplication.db();
    }
    public void setTitle(String title) {
      this.title = title;
    }
    public String getTitle() {
      return title;
    }
    public void setPartiesAreToBeHandledP(boolean partiesAreToBeHandledP) {
      this.partiesAreToBeHandledP = partiesAreToBeHandledP;      
    }
    /* (non-Javadoc)
    * loads the XML from the source, parses it, and sets up progress dialog
    * @see android.os.AsyncTask#onPreExecute()
    */
    protected void parseXML() {
      super.onPreExecute();
      String sourceStr = activity.IMPORT_SOURCES[source];
      //the last entry in the array is the custom import from sdcard
      if (source == activity.IMPORT_SOURCES.length -1) {
        try {
          catXML = new FileInputStream(sourceStr);
        } catch (FileNotFoundException e) {
          Toast.makeText(activity, "Could not find file " + sourceStr, Toast.LENGTH_LONG).show();
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
        case 3:
          sourceRes = R.raw.cat_it;
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
        Toast.makeText(activity, "Could not parse file "+sourceStr, Toast.LENGTH_LONG).show();
        cancel(false);
        return;
      } catch (Exception e) {
        Toast.makeText(activity, "An error occured: "+e.getMessage(), Toast.LENGTH_LONG).show();
        cancel(false);
        return;
      }

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
            return;
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
        return;
      }
      parties = root.getElementsByTagName("Party");
    }
    public Integer getSource() {
      return source;
    }
    public Integer getTotalImportedCat() {
      return totalImportedCat;
    }
    public Integer getTotalImportedParty() {
      return totalImportedParty;
    }
    void attach(GrisbiImport activity2) {
      this.activity=activity2;
    }
    void detach() {
      activity=null;
    }
    int getProgress() {
      return(progress);
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
        if (phaseChangedP) {
          activity.mProgressDialog.setTitle(getTitle());
          activity.mProgressDialog.setMax(getMax());
          phaseChangedP = false;
        }
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
      String sourceStr = activity.IMPORT_SOURCES[source];
      parseXML();
      if (isCancelled())
        return(null);
      setTitle(activity.getString(R.string.grisbi_import_categories_loading,sourceStr));
      phaseChangedP = true;
      publishProgress(0);
      totalImportedCat = 0;
      totalImportedParty = 0;
      categories = root.getElementsByTagName(mainElementName);
      sub_categories = root.getElementsByTagName(subElementName);
      setMax(categories.getLength() + sub_categories.getLength());
      activity.mProgressDialog.setMax(getMax());

      if (grisbiFileVersion.equals("0.6.0")) {
        importCatsMain();
        importCatsSub();
      } else {
        importCats050();
      }
      if (partiesAreToBeHandledP)
        setTitle(activity.getString(R.string.grisbi_import_parties_loading,sourceStr));
        importParties();
      return(null);
    }
    private void importParties() {
      phaseChangedP = true;
      setMax(parties.getLength());
      publishProgress(0);
      int count = 0;
      NamedNodeMap atts;
      publishProgress(0);
      for (int i=0;i<parties.getLength();i++){
        count++;
        atts = parties.item(i).getAttributes();
        if (mDbHelper.createPayee(atts.getNamedItem("Na").getNodeValue()) != -1)
          totalImportedParty++;
        if (count % 10 == 0) {
          publishProgress(count);
        }
      }
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
          totalImportedCat++;
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
              totalImportedCat++;
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
            totalImportedCat++;
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
            totalImportedCat++;
          }
        } else {
          Log.w("MyExpenses","could not store sub category " + label);
        }
        if ((start+i) % 10 == 0) {
          publishProgress(start+i);
        }
      }
    }
    int getMax() {
      return max;
    }
    void setMax(int max) {
      this.max = max;
    }
  }

  @Override
  public void onClick(DialogInterface dialog, int id) {
    switch (id) {
    case AlertDialog.BUTTON_POSITIVE:
    case AlertDialog.BUTTON_NEUTRAL:
      String title = getString(R.string.grisbi_import_parsing,IMPORT_SOURCES[sourceIndex]);
      mProgressDialog.setTitle(title);
      mProgressDialog.show();
      task = new MyAsyncTask(GrisbiImport.this,sourceIndex);
      task.setTitle(title);
      task.setPartiesAreToBeHandledP(id == AlertDialog.BUTTON_POSITIVE);
      dismissDialog(SOURCES_DIALOG_ID);
      task.execute();
      return;
    case AlertDialog.BUTTON_NEGATIVE:
      dismissDialog(SOURCES_DIALOG_ID);
      finish();
    default:
      sourceIndex = id;
      //we enable import of categories only for any source
      //categories and parties only for the last custom file
      ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);
      ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
            id == IMPORT_SOURCES.length -1);    
    }
  }
}
