package org.totschnig.myexpenses.task;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.GrisbiImport;
import org.totschnig.myexpenses.dialog.GrisbiSourcesDialogFragment;
import org.totschnig.myexpenses.util.CategoryTree;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import android.content.res.Resources.NotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;

public class GrisbiImportTask extends AsyncTask<Boolean, Integer, Result> {

  /**
   * 
   */
  private final TaskExecutionFragment taskExecutionFragment;

  public GrisbiImportTask(TaskExecutionFragment taskExecutionFragment, Bundle b) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.withPartiesP = b.getBoolean("withParties");
  }

  String title;
  private int max;
  /**
   * should we handle parties as well?
   */
  boolean withPartiesP;
  /**
   * this is set when we finish one phase (parsing, importing categories,
   * importing parties) so that we can adapt progress dialog in
   * onProgressUpdate
   */
  boolean phaseChangedP = false;
  private CategoryTree catTree;
  private ArrayList<String> partiesList;

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  /**
   * return false upon problem (and sets a result object) or true
   * 
   * @param source2
   */
  protected Result parseXML(String sourceStr) {
    InputStream catXML = null;
    Result result;

    try {
      if (sourceStr
          .equals(GrisbiSourcesDialogFragment.IMPORT_SOURCE_INTERNAL)) {
        try {
          catXML = this.taskExecutionFragment.getResources().openRawResource(
              GrisbiSourcesDialogFragment.defaultSourceResId);
        } catch (NotFoundException e) {
          catXML = this.taskExecutionFragment.getResources().openRawResource(R.raw.cat_en);
        }
      } else {
        catXML = new FileInputStream(sourceStr);
      }
      result = Utils.analyzeGrisbiFileWithSAX(catXML);
      if (result.success) {
        catTree = (CategoryTree) result.extra[0];
        partiesList = (ArrayList<String>) result.extra[1];
      }
    } catch (FileNotFoundException e) {
      result = new Result(false, R.string.parse_error_file_not_found,
          sourceStr);
    } finally {
      if (catXML != null) {
        try {
          catXML.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return result;
  }

  /**
   * made public to allow passing task to
   * {@link Utils#importCats(CategoryTree, GrisbiImportTask)} and
   * {@link Utils#importParties(ArrayList, GrisbiImportTask)}
   * 
   * @param i
   */
  public void publishProgress(Integer i) {
    super.publishProgress(i);
  }

  /*
   * (non-Javadoc) updates the progress dialog
   * 
   * @see android.os.AsyncTask#onProgressUpdate(Progress[])
   */
  protected void onProgressUpdate(Integer... values) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      if (phaseChangedP) {
        ((GrisbiImport) this.taskExecutionFragment.mCallbacks).setProgressMax(getMax());
        ((GrisbiImport) this.taskExecutionFragment.mCallbacks).setProgressTitle(getTitle());
        phaseChangedP = false;
      }
      this.taskExecutionFragment.mCallbacks.onProgressUpdate(values[0]);
    }
  }

  /*
   * (non-Javadoc) reports on success (with total number of imported
   * categories) or failure
   * 
   * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
   */
  @Override
  protected void onPostExecute(Result result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(TaskExecutionFragment.TASK_GRISBI_IMPORT, result);
    }
  }

  /*
   * (non-Javadoc) this is where the bulk of the work is done via calls to
   * {@link #importCatsMain()} and {@link #importCatsSub()} sets up {@link
   * #categories} and {@link #sub_categories}
   * 
   * @see android.os.AsyncTask#doInBackground(Params[])
   */
  @Override
  protected Result doInBackground(Boolean... external) {
    String sourceStr = external[0] ? GrisbiSourcesDialogFragment.IMPORT_SOURCE_EXTERNAL
        : GrisbiSourcesDialogFragment.IMPORT_SOURCE_INTERNAL;
    Result r = parseXML(sourceStr);
    if (!r.success) {
      return r;
    }
    setTitle(this.taskExecutionFragment.getString(R.string.grisbi_import_categories_loading, sourceStr));
    phaseChangedP = true;
    setMax(catTree.getTotal());
    publishProgress(0);

    int totalImportedCat = Utils.importCats(catTree, this);
    if (withPartiesP) {
      setTitle(this.taskExecutionFragment.getString(R.string.grisbi_import_parties_loading, sourceStr));
      phaseChangedP = true;
      setMax(partiesList.size());
      publishProgress(0);

      int totalImportedParty = Utils.importParties(partiesList, this);
      return new Result(true,
          R.string.grisbi_import_categories_and_parties_success,
          String.valueOf(totalImportedCat),
          String.valueOf(totalImportedParty));
    } else {
      return new Result(true, R.string.grisbi_import_categories_success,
          String.valueOf(totalImportedCat));
    }
  }

  int getMax() {
    return max;
  }

  void setMax(int max) {
    this.max = max;
  }
}