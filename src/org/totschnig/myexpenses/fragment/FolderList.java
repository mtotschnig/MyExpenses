/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
//adapted to My Expenses by Michael Totschnig

package org.totschnig.myexpenses.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
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

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 12/23/11 12:53 AM
 *
 */
public class FolderList extends ListFragment {

    public static final String PATH = "PATH";
    
    private final List<FileItem> files = new ArrayList<FileItem>();

    private File selectedFolder;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
  }
    @Override
    public void onCreateOptionsMenu(Menu menu,MenuInflater inflator) {
      inflator.inflate(R.menu.folder, menu);
    }
    
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
      boolean isWritable = selectedFolder.canWrite();
      menu.findItem(R.id.CREATE_COMMAND).setVisible(isWritable);
      menu.findItem(R.id.SELECT_COMMAND).setVisible(isWritable);
      File parent = selectedFolder.getParentFile();
      menu.findItem(R.id.UP_COMMAND).setVisible(parent!=null);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      FolderBrowser ctx = (FolderBrowser) getActivity();
      switch (item.getItemId()) {
      case R.id.SELECT_COMMAND:
        Intent result = new Intent();
        result.putExtra(PATH, selectedFolder.getAbsolutePath());
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
        if (!browseToCurrentFolder()) {
            browseToRoot();
        }
    }

    private boolean browseToCurrentFolder() {
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            String path = intent.getStringExtra(PATH);
            if (path != null) {
                browseTo(new File(path));
                return true;
            }
        }
        return false;
    }

    private void createNewFolder() {
        final EditText editText = new EditText(getActivity());
        Dialog d = new AlertDialog.Builder(getActivity())
                .setTitle("create_new_folder_title")
                .setView(editText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        createNewFolder(editText.getText().toString());
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create();
        d.show();
    }
    
    private void createNewFolder(String name) {
        boolean result = false;
        try {
            result = new File(selectedFolder, name).mkdirs();
        } catch (Exception e) {
            result = false;
        } finally {
            if (!result) {
                Toast.makeText(getActivity(), "create_new_folder_fail", Toast.LENGTH_LONG).show();
            }
            browseTo(selectedFolder);
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
        getActivity().setTitle(current.getAbsolutePath());
        getActivity().supportInvalidateOptionsMenu();
    }

    private void browse(File current) {
        File[] files = current.listFiles();
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
        ListAdapter adapter = new ArrayAdapter<FileItem>(getActivity(), android.R.layout.simple_list_item_1, files);
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
