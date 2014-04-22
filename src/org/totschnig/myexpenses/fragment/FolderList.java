/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
//adapted to My Expenses by Michael Totschnig

package org.totschnig.myexpenses.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.FolderBrowser;
import org.totschnig.myexpenses.dialog.EditTextDialog;

public class FolderList extends ListFragment {

  private final List<FileItem> files = new ArrayList<FileItem>();

  private File selectedFolder;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflator) {
    inflator.inflate(R.menu.folder, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    boolean isWritable = selectedFolder != null && selectedFolder.canWrite();
    boolean hasParent = selectedFolder != null
        && selectedFolder.getParentFile() != null;
    menu.findItem(R.id.CREATE_COMMAND).setVisible(isWritable);
    menu.findItem(R.id.SELECT_COMMAND).setVisible(isWritable);
    menu.findItem(R.id.UP_COMMAND).setVisible(hasParent);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    FolderBrowser ctx = (FolderBrowser) getActivity();
    switch (item.getItemId()) {
    case R.id.SELECT_COMMAND:
      Intent result = new Intent();
      result.putExtra(FolderBrowser.PATH, selectedFolder.getAbsolutePath());
      ctx.setResult(FolderBrowser.RESULT_OK, result);
      ctx.finish();
      break;
    case R.id.CREATE_COMMAND:
      createNewFolder();
      break;
    case R.id.UP_COMMAND:
      browseTo(selectedFolder.getParentFile());
      break;
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
      browseTo(new File(path));
    } else {
      browseToRoot();
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (selectedFolder != null) {
      outState.putString(FolderBrowser.PATH, selectedFolder.getAbsolutePath());
    }
  }

  private void createNewFolder() {
    Bundle args = new Bundle();
    args.putString(EditTextDialog.KEY_DIALOG_TITLE,getString(R.string.menu_create_folder));
    EditTextDialog.newInstance(args)
        .show(getFragmentManager(), "CREATE_FOLDER");
  }

  public void createNewFolder(String name) {
    boolean result = false;
    File newFolder = null;
    try {
      newFolder = new File(selectedFolder, name);
      result = newFolder.mkdirs();
    } catch (Exception e) {
      result = false;
    } finally {
      if (!result) {
        Toast.makeText(getActivity(), R.string.create_new_folder_fail,
            Toast.LENGTH_LONG).show();
      } else if (newFolder.isDirectory()) {
        browseTo(newFolder);
      }
    }
  }

  private void browseToRoot() {
    browseTo(new File("/"));
  }

  private void browseTo(File current) {
    files.clear();
    browse(current);
    setAdapter();
    selectCurrentFolder(current);
  }

  private void selectCurrentFolder(File current) {
    selectedFolder = current;
    ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(
        current.getAbsolutePath());
    getActivity().supportInvalidateOptionsMenu();
  }

  private void browse(File current) {
    File[] files = current.listFiles();
    if (files == null) {
      FolderBrowser ctx = (FolderBrowser) getActivity();
      Toast.makeText(ctx,getString(R.string.external_storage_unavailable), Toast.LENGTH_LONG).show();
      ctx.setResult(Activity.RESULT_CANCELED);
      ctx.finish();
      return;
    }
    Arrays.sort(files);
    for (File file : files) {
      if (isWritableDirectory(file)) {
        this.files.add(new FileItem(file));
      }
    }
  }

  private boolean isWritableDirectory(File file) {
    return file.isDirectory() && file.canRead() && file.canWrite();
  }

  private void setAdapter() {
    ListAdapter adapter = new ArrayAdapter<FileItem>(getActivity(),
        android.R.layout.simple_list_item_1, files);
    setListAdapter(adapter);
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
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
