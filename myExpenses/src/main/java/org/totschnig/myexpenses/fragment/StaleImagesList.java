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

package org.totschnig.myexpenses.fragment;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

import com.squareup.picasso.Picasso;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ImageViewIntentProvider;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

public class StaleImagesList extends ContextualActionBarFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  SimpleCursorAdapter mAdapter;
  private Cursor mImagesCursor;

  @Inject
  ImageViewIntentProvider imageViewIntentProvider;
  @Inject
  Picasso picasso;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((MyApplication) requireActivity().getApplication()).getAppComponent().inject(this);
  }

  @Override
  public boolean dispatchCommandMultiple(int command,
                                         SparseBooleanArray positions, Long[] itemIds) {
    int taskId = 0, progressMessage = 0;
    switch (command) {
      case R.id.SAVE_COMMAND:
        taskId = TaskExecutionFragment.TASK_SAVE_IMAGES;
        progressMessage = R.string.progress_dialog_saving;
        break;
      case R.id.DELETE_COMMAND:
        taskId = TaskExecutionFragment.TASK_DELETE_IMAGES;
        progressMessage = R.string.progress_dialog_deleting;
        break;
    }
    if (taskId == 0) {
      return super.dispatchCommandMultiple(command, positions, itemIds);
    }
    finishActionMode();
    ((ProtectedFragmentActivity) getActivity()).startTaskExecution(
        taskId,
        itemIds,
        null,
        progressMessage);
    return true;
  }

  @Override
  @SuppressLint("InlinedApi")
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.images_list, container, false);

    final GridView lv = (GridView) v.findViewById(R.id.grid);

    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{DatabaseConstants.KEY_PICTURE_URI};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.image};

    // Now create a simple cursor adapter and set it to display
    mAdapter = new SimpleCursorAdapter(
        getActivity(),
        R.layout.image_view,
        null,
        from,
        to,
        0) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(position, convertView, parent);
      }

      @Override
      public void setViewImage(ImageView v, String value) {
        if (v.getTag() != null && v.getTag().equals(value)) {
          //already dealing with value; nothing to do
          return;
        }
        picasso.load(value).placeholder(R.drawable.empty_photo).fit().into(v);
        v.setTag(value);
        v.setContentDescription(value);
      }
    };
    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mImagesCursor.moveToPosition(position);
        imageViewIntentProvider.startViewIntent(getActivity(),
            Uri.parse(mImagesCursor.getString(
                mImagesCursor.getColumnIndex(DatabaseConstants.KEY_PICTURE_URI))));
      }
    });
    getLoaderManager().initLoader(0, null, this);
    lv.setAdapter(mAdapter);
    registerForContextualActionBar(lv);
    return v;
  }

  @Override
  public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
    CursorLoader cursorLoader = new CursorLoader(getActivity(),
        TransactionProvider.STALE_IMAGES_URI, null, null, null, null);
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
    mImagesCursor = c;
    mAdapter.swapCursor(c);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    mImagesCursor = null;
    mAdapter.swapCursor(null);
  }

  @Override
  protected void inflateHelper(Menu menu, int listId) {
    MenuInflater inflater = getActivity().getMenuInflater();
    inflater.inflate(R.menu.stale_images_context, menu);
  }
}
