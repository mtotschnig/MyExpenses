/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
//adapted to My Expenses by Michael Totschnig

package org.totschnig.myexpenses.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.FolderBrowser;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.ListFragment;

public class FolderList extends ListFragment {

  private final List<FileItem> files = new ArrayList<>();

  private File selectedFolder;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.folder, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    boolean isWritable = selectedFolder != null && selectedFolder.canWrite();
    boolean hasReadableParent = selectedFolder != null
        && selectedFolder.getParentFile() != null && selectedFolder.getParentFile().canRead();
    menu.findItem(R.id.CREATE_COMMAND).setVisible(isWritable);
    menu.findItem(R.id.SELECT_COMMAND).setVisible(isWritable);
    menu.findItem(R.id.UP_COMMAND).setVisible(hasReadableParent);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    FolderBrowser ctx = (FolderBrowser) getActivity();
    int itemId = item.getItemId();
    if (itemId == R.id.SELECT_COMMAND) {
      if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
        try {
          //on Kitkat secondary storage is reported as writable by File.canWrite(),
          //although in fact it is not
          File.createTempFile("test", null, selectedFolder).delete();
        } catch (IOException e) {
          ctx.showSnackbar(getString(R.string.app_dir_not_accessible,
              selectedFolder.getPath()), Snackbar.LENGTH_SHORT);
          return super.onOptionsItemSelected(item);
        }
      }
      PrefKey.APP_DIR.putString(Uri.fromFile(selectedFolder).toString());
      ctx.setResult(FolderBrowser.RESULT_OK);
      ctx.finish();
    } else if (itemId == R.id.CREATE_COMMAND) {
      createNewFolder();
    } else if (itemId == R.id.UP_COMMAND) {
      browseTo(selectedFolder.getParentFile());
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    String path = null;
    if (savedInstanceState != null) {
      path = savedInstanceState.getString(FolderBrowser.PATH);
    }
    if (path == null) {
      Intent intent = getActivity().getIntent();
      if (intent != null) {
        path = intent.getStringExtra(FolderBrowser.PATH);
      }
    }
    if (path != null) {
      File current = new File(path);
      if (current.isDirectory()) {
        browseTo(new File(path));
        return;
      }
    }
    browseToRoot();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (selectedFolder != null) {
      outState.putString(FolderBrowser.PATH, selectedFolder.getAbsolutePath());
    }
  }

  private void createNewFolder() {
    Bundle args = new Bundle();
    args.putString(EditTextDialog.KEY_DIALOG_TITLE,getString(R.string.menu_create_folder));
    EditTextDialog.newInstance(args)
        .show(getParentFragmentManager(), "CREATE_FOLDER");
  }

  public void createNewFolder(String name) {
    boolean result = false;
    File newFolder = null;
    try {
      newFolder = new File(selectedFolder, name);
      result = newFolder.mkdirs();
    } catch (Exception e) {
      CrashHandler.report(e);
      result = false;
    } finally {
      if (!result) {
        ((ProtectedFragmentActivity) getActivity()).showSnackbar(R.string.create_new_folder_fail);
      } else if (newFolder.isDirectory()) {
        browseTo(newFolder);
      }
    }
  }

  private void browseToRoot() {
    browseTo(getContext().getExternalFilesDir(null));
  }

  private void browseTo(File current) {
    files.clear();
    browse(current);
    setAdapter();
    selectCurrentFolder(current);
  }

  private void selectCurrentFolder(File current) {
    selectedFolder = current;
    ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(
        current.getAbsolutePath());
    getActivity().supportInvalidateOptionsMenu();
  }

  private void browse(File current) {
    File[] files = current.listFiles();
    if (files == null) {
      FolderBrowser ctx = (FolderBrowser) getActivity();
      ctx.showMessage(R.string.external_storage_unavailable);
      return;
    }
    Arrays.sort(files);
    for (File file : files) {
      if (isReadableDirectory(file)) {
        this.files.add(new FileItem(file));
      }
    }
  }

  private boolean isReadableDirectory(File file) {
    return file.isDirectory() && file.canRead();
  }

  private void setAdapter() {
    ListAdapter adapter = new ArrayAdapter<>(getActivity(),
            android.R.layout.simple_list_item_1, files);
    setListAdapter(adapter);
  }

  @Override
  public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    FileItem selected = files.get(position);
    browseTo(selected.file);
  }

  private static class FileItem {
    private final File file;

    private FileItem(File file) {
      this.file = file;
    }

    @Override
    public String toString() {
      return file.getName();
    }

  }
}
