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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

//needed for analyzeGrisbiFileWithDOM
/*
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;
*/

import org.totschnig.myexpenses.Utils.CategoryTree;
import org.totschnig.myexpenses.Utils.Result;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

public class GrisbiImport extends Activity implements DialogInterface.OnClickListener {
  static final int SOURCES_DIALOG_ID = 1;
  static final int PARSE_ERROR_FILE_NOT_FOUND = -1;
  static final int PARSE_ERROR_PARSE_EXCEPTION = -2;
  static final int PARSE_ERROR_OTHER_EXCEPTION = -3;
  static final int PARSE_ERROR_GRISBI_VERSION_NOT_DETERMINED = -4;
  static final int PARSE_ERROR_GRISBI_VERSION_NOT_SUPPORTED = -5;
  static final int PARSE_SUCCESS = 1;
  ProgressDialog mProgressDialog;
  private AlertDialog mSourcesDialog;
  //String sourceStr;
  private MyAsyncTask task=null;
  
  /**
   * Choice of sources for importing categories presented to the user.
   * The first four are internal to the app, the fourth one is provided by the user 
   */
  private final String[] IMPORT_SOURCES = {
      "Grisbi (en)", 
      "Grisbi (fr)", 
      "Grisbi (de)", 
      "Grisbi (it)",
      "Grisbi (es)",
      "/sdcard/myexpenses/grisbi.xml"
  };
  /**
   * stores the index of the source the user has selected
   */
  public int sourceIndex;
  
  public static class GrisbiHandler extends DefaultHandler{
    CategoryTree catTree ;
    ArrayList<String> partiesList;
    private StringBuilder builder;
    String grisbiFileVersion;
    String mainElementName6 = "Category",
        subElementName6 = "Sub_category",
        partiesElementName6 = "Party",
        nameAttributeName6 = "Na",
        mainElementName5 = "Categorie",
        subElementName5 = "Sous-categorie",
        partiesElementName5 = "Tiers",
        nameAttributeName5 = "Nom";
    Integer currentMainCategorieId;
    
    class FileVersionNotSupportedException extends SAXException {

      public FileVersionNotSupportedException(String grisbiFileVersion) {
        super(grisbiFileVersion);
      }
      
    }
    
    private boolean checkFileVersion(String version) {
      return version.equals("0.6.0") || version.equals("0.5.0");
    }
    
    
    public Result getResult() {
      if (catTree.getTotal() > 0 || partiesList.size() >0 ) {
        return new Result(true,0,catTree,partiesList);
      } else {
        return new Result(false,R.string.parse_error_no_data_found);
      }
    }
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        super.characters(ch, start, length);
        builder.append(ch, start, length);
    }
    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        super.endElement(uri, localName, name);
        if (localName.equals("Version_fichier") || localName.equals("Version_fichier_categ")){
          String grisbiFileVersion = builder.toString();
          if (!checkFileVersion(grisbiFileVersion)) {
            throw new FileVersionNotSupportedException(grisbiFileVersion);
          }
        }
    }

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        catTree = new CategoryTree("root");
        partiesList = new ArrayList<String>();
        builder = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String name,
            Attributes attributes) throws SAXException {
      String label, id, parent_id;
      super.startElement(uri, localName, name, attributes);
      if (localName.equalsIgnoreCase("General")){
        String grisbiFileVersion = attributes.getValue("File_version");
        if (!checkFileVersion(grisbiFileVersion)) {
          throw new FileVersionNotSupportedException(grisbiFileVersion);
        }
      } else if (localName.equals(mainElementName6)) {
        label = attributes.getValue(nameAttributeName6);
        id = attributes.getValue("Nb");
        if (label != null && id != null) {
          catTree.add(label,Integer.parseInt(id),0);
        }
      } else if (localName.equals(subElementName6)) {
        label = attributes.getValue(nameAttributeName6);
        id =  attributes.getValue("Nb");
        parent_id = attributes.getValue("Nbc");
        if (label != null && id != null && parent_id != null) {
          catTree.add(label, Integer.parseInt(id), Integer.parseInt(parent_id));
        }
      } else if (localName.equals(mainElementName5)) {
        label = attributes.getValue(nameAttributeName5);
        id = attributes.getValue("No");
        if (label != null && id != null) {
          currentMainCategorieId = Integer.parseInt(id);
          catTree.add(label,currentMainCategorieId,0);
        }
      } else if (localName.equals(subElementName5)) {
        label = attributes.getValue(nameAttributeName5);
        id =  attributes.getValue("No");
        if (label != null && id != null) {
          catTree.add(label,  Integer.parseInt(id), currentMainCategorieId);
        }
      } else if  (localName.equals(partiesElementName6)) {
        label = attributes.getValue(nameAttributeName6);
        if (label != null) {
          partiesList.add(label);
        }
      } else if  (localName.equals(partiesElementName5)) {
        label = attributes.getValue(nameAttributeName5);
        if (label != null) {
          partiesList.add(label);
        }
      }
      builder.setLength(0);
    }
  }
  
  public static Result analyzeGrisbiFileWithSAX(InputStream is) {
    GrisbiHandler handler = new GrisbiHandler();
    try {
        Xml.parse(is, Xml.Encoding.UTF_8, handler);
    }  catch (IOException e) {
      // TODO Auto-generated catch block
      return new Result(false,R.string.parse_error_other_exception,e.getMessage());
    } catch (GrisbiHandler.FileVersionNotSupportedException e) {
      return new Result(false,R.string.parse_error_grisbi_version_not_supported,e.getMessage());
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      return new Result(false,R.string.parse_error_parse_exception);
    }
    return handler.getResult();
  }
  
  /**
   * @param is InputStream to be analyzed
   * @return if there is a problem success is set to false and message to a diagnostic message (as an R string integer)
   * if the analysis succeeds, success is true, message is 0, and extras has as first element the
   * CategoryTree, and as second element a string array with payees
   * DOM and SAX implementations are mainly functionally equivalent, with the main
   * difference that the DOM implementation crashes with OutOfMemoryError on big input files
   * the DOM implementation reports parse_error_grisbi_version_not_determined, if the XML file is
   * unrelated to Grisbi, while the SAX implementation reports success and returns empty
   * category tree and payee list
   * 
  */
  /*
  public static Result analyzeGrisbiFileWithDOM(InputStream is) {
    Document dom;
    Element root;
    String grisbiFileVersion = "";
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      dom = builder.parse(is);
    } catch (SAXParseException e) {
      //Log.w("MyExpenses",e.getMessage());
      return new Result(false,R.string.parse_error_parse_exception);
    } catch (Exception e) {
      return new Result(false,R.string.parse_error_other_exception,e.getMessage());
    }

    root = dom.getDocumentElement();
    Node generalNode = root.getElementsByTagName("General").item(0);
    if (generalNode != null) {
      Node versionAttr = generalNode.getAttributes().getNamedItem("File_version");
      if (versionAttr != null) {
        grisbiFileVersion = versionAttr.getNodeValue();
        Log.i("MyExpenses","found Grisbi version " + grisbiFileVersion);
      }
    }
    else {
      Node versionNode = root.getElementsByTagName("Version_fichier").item(0);
      if (versionNode == null) {
        versionNode = root.getElementsByTagName("Version_fichier_categ").item(0);
      }
      if (versionNode != null) {
        //unfortunately we have to retrieve the first text node in order to get at the value
        grisbiFileVersion = versionNode.getChildNodes().item(0).getNodeValue();
        Log.i("MyExpenses","found Grisbi version " + grisbiFileVersion);
      }
      else {
        Log.i("MyExpenses","did not find Grisbi version");
        //we are mapping the existence of Categorie to 0.5.0
        //and Category to 0.6.0
        if (root.getElementsByTagName("Category").getLength() > 0) {
          grisbiFileVersion = "0.6.0";
          Log.i("MyExpenses","assuming Grisbi version 0.6.0");
        }
        else if (root.getElementsByTagName("Categorie").getLength() > 0) {
          grisbiFileVersion = "0.5.0";
          Log.i("MyExpenses","assuming Grisbi version 0.5.0");
        }
        else {
          return new Result(false,R.string.parse_error_grisbi_version_not_determined);
        }
      }
    }
    if (grisbiFileVersion.equals("0.6.0")) {
      return extractFromGrisbi6(root);
    } else if (grisbiFileVersion.equals("0.5.0")) {
      return extractFromGrisbi5(root);
    } else {
      return new Result(false,R.string.parse_error_grisbi_version_not_supported,grisbiFileVersion);
    }
  }
  public static Result extractFromGrisbi6(Element root) {
    String mainElementName = "Category",
        subElementName = "Sub_category",
        partiesElementName = "Party",
        nameAttributeName = "Na";
    NodeList categories = root.getElementsByTagName(mainElementName),
        sub_categories = root.getElementsByTagName(subElementName),
        parties = root.getElementsByTagName(partiesElementName);
    CategoryTree catTree = new CategoryTree("root");
    String label;
    Integer id, parent_id;
    NamedNodeMap atts;
    Node nameAttr, parentAttr, numberAttr;

    //extract main categories
    for (int i=0;i<categories.getLength();i++){
      atts = categories.item(i).getAttributes();
      nameAttr = atts.getNamedItem(nameAttributeName);
      numberAttr = atts.getNamedItem("Nb");
      if (nameAttr == null || numberAttr == null) {
        continue;
      }
      label = nameAttr.getNodeValue();
      id = Integer.parseInt(numberAttr.getNodeValue());
      catTree.add(label,id,0);
    }
    //extract sub categories
    for (int i=0;i<sub_categories.getLength();i++){
      atts = sub_categories.item(i).getAttributes();
      nameAttr = atts.getNamedItem(nameAttributeName);
      numberAttr = atts.getNamedItem("Nb");
      parentAttr = atts.getNamedItem("Nbc");
      if (nameAttr == null || parentAttr == null || numberAttr == null) {
        continue;
      }

      label = nameAttr.getNodeValue();
      id =  Integer.parseInt(numberAttr.getNodeValue());
      parent_id = Integer.parseInt(parentAttr.getNodeValue());
      catTree.add(label, id, parent_id);
      //TODO: for the moment, we do not deal with subcategories,
      //if we were not able to import a matching category
      //should check if the main category exists, but the subcategory is new
    }
    ArrayList<String> partiesList = extractParties(parties,nameAttributeName);
    return new Result(true,0,catTree,partiesList);
  }
  public static Result extractFromGrisbi5(Element root) {
    String mainElementName = "Categorie",
        subElementName = "Sous-categorie",
        partiesElementName = "Tiers",
        nameAttributeName = "Nom";
    NodeList categories = root.getElementsByTagName(mainElementName),
        parties = root.getElementsByTagName(partiesElementName);
    CategoryTree catTree = new CategoryTree("root");
    String label;
    Integer main_id, sub_id;
    NamedNodeMap atts;
    Node nameAttr, numberAttr;
    
    for (int i=0;i<categories.getLength();i++){
      atts = categories.item(i).getAttributes();
      nameAttr = atts.getNamedItem(nameAttributeName);
      numberAttr = atts.getNamedItem("No");
      if (nameAttr == null || numberAttr == null) {
        continue;
      }
      label = nameAttr.getNodeValue();
      main_id =  Integer.parseInt(numberAttr.getNodeValue());
      catTree.add(label,main_id,0);
      NodeList children = categories.item(i).getChildNodes();
      for (int j=0;j<children.getLength();j++){
        Node child = children.item(j);
        String nodeName = child.getNodeName();
        if ((nodeName != null) && (nodeName.equals(subElementName))) {
          atts = child.getAttributes();
          nameAttr = atts.getNamedItem(nameAttributeName);
          numberAttr = atts.getNamedItem("No");
          if (nameAttr == null || numberAttr == null) {
            continue;
          }
          label = nameAttr.getNodeValue();
          sub_id =  Integer.parseInt(numberAttr.getNodeValue());
          catTree.add(label, sub_id, main_id);
        }
      }
    }
    ArrayList<String> partiesList = extractParties(parties,nameAttributeName);
    return new Result(true,0,catTree,partiesList);
  }
  public static ArrayList<String> extractParties(NodeList parties, String nameAttributeName) {
    NamedNodeMap atts;
    Node nameAttr;
    ArrayList<String> result = new ArrayList<String>();
    for (int i=0;i<parties.getLength();i++){
      atts = parties.item(i).getAttributes();
      nameAttr = atts.getNamedItem(nameAttributeName);
      if (nameAttr == null) {
        continue;
      }
      result.add(nameAttr.getNodeValue());
    }
    return result;
  }
  */

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
    Result result = task.getResult();
    msg = getString(result.message,result.extra);
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
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
    InputStream catXML;
    private int max, totalImportedCat, totalImportedParty;
    Result result;
    private ExpensesDbAdapter mDbHelper;
    int progress=0;
    String grisbiFileVersion;
    /**
     * should we handle parties as well?
     */
    boolean withPartiesP = true;
    /**
     * this is set when we finish one phase (parsing, importing categories, importing parties)
     * so that we can adapt progress dialog in onProgressUpdate
     */
    boolean phaseChangedP = false;
    private String title;
    private CategoryTree catTree;
    private ArrayList<String> partiesList;

    /**
     * @param context
     * @param source Source for the import
     */
    public MyAsyncTask(GrisbiImport activity,int source) {
      attach(activity);
      this.source = source;
      mDbHelper = MyApplication.db();
    }
    public void setTitle(String title) {
      this.title = title;
    }
    public String getTitle() {
      return title;
    }
    public void setWithPartiesP(boolean withPartiesP) {
      this.withPartiesP =withPartiesP;      
    }
    /**
     * return false upon problem (and sets a result object) or true
     */
    protected boolean parseXML() {
      String sourceStr = activity.IMPORT_SOURCES[source];
      //the last entry in the array is the custom import from sdcard
      if (source == activity.IMPORT_SOURCES.length -1) {
        try {
          catXML = new FileInputStream(sourceStr);
        } catch (FileNotFoundException e) {
          setResult(new Result(false,R.string.parse_error_file_not_found,sourceStr));
          return false;
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
        case 4:
          sourceRes = R.raw.cat_es;
        }
        catXML = activity.getResources().openRawResource(sourceRes);
      }
      Result result = analyzeGrisbiFileWithSAX(catXML);
      if (result.success) {
        catTree = (CategoryTree) result.extra[0];
        partiesList = (ArrayList<String>) result.extra[1];
        return true;
      } else {
        setResult(result);
        return false;
      }
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
      if (!parseXML())
        return(null);
      setTitle(activity.getString(R.string.grisbi_import_categories_loading,sourceStr));
      phaseChangedP = true;
      publishProgress(0);
      totalImportedCat = 0;
      totalImportedParty = 0;
      setMax(catTree.getTotal());
      activity.mProgressDialog.setMax(getMax());

      importCats();
      if (withPartiesP) {
        setTitle(activity.getString(R.string.grisbi_import_parties_loading,sourceStr));
        importParties();
        setResult(new Result(true,
            R.string.grisbi_import_categories_and_parties_success,
            String.valueOf(getTotalImportedCat()),
            String.valueOf(getTotalImportedParty())
        ));
      } else {
        setResult(new Result(true,
            R.string.grisbi_import_categories_success,
            String.valueOf(getTotalImportedCat())
        ));
      }
        
      return(null);
    }
    private void importParties() {
      phaseChangedP = true;
      setMax(partiesList.size());
      publishProgress(0);
      
      for (int i=0;i<partiesList.size();i++){
        if (mDbHelper.createPayee(partiesList.get(i)) != -1) {
          totalImportedParty++;
        }
        if (i % 10 == 0) {
          publishProgress(i);
        }
      }
    }
    private void importCats() {
      int count = 0;
      String label;
      long main_id, sub_id;

      for (Map.Entry<Integer,CategoryTree> main : catTree.children().entrySet()) {
        CategoryTree mainCat = main.getValue();
        label = mainCat.getLabel();
        count++;
        main_id = mDbHelper.getCategoryId(label, 0);
        if (main_id != -1) {
          Log.i("MyExpenses","category with label" + label + " already defined");
        } else {
          main_id = mDbHelper.createCategory(label,0);
          if (main_id != -1) {
            totalImportedCat++;
            if (count % 10 == 0) {
              publishProgress(count);
            }
          } else {
            //this should not happen
            Log.w("MyExpenses","could neither retrieve nor store main category " + label);
            continue;
          }
        }
        for (Map.Entry<Integer,CategoryTree> sub : mainCat.children().entrySet()) {
          label = sub.getValue().getLabel();
          count++;
          sub_id = mDbHelper.createCategory(label,main_id);
          if (sub_id != -1) {
            totalImportedCat++;
          } else {
            Log.i("MyExpenses","could not store sub category " + label);
          }
          if (count % 10 == 0) {
            publishProgress(count);
          }
        }
      }
    }

    int getMax() {
      return max;
    }
    void setMax(int max) {
      this.max = max;
    }
    public Result getResult() {
      return result;
    }
    public void setResult(Result result) {
      this.result = result;
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
      task.setWithPartiesP(id == AlertDialog.BUTTON_POSITIVE);
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
