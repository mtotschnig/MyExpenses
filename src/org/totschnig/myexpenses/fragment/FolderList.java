/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package org.totschnig.myexpenses.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.totschnig.myexpenses.R;

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
    public void onActivityCreated(Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);
//        selectButton = (Button)findViewById(R.id.selectButton);
//        selectButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent result = new Intent();
//                result.putExtra(PATH, selectedFolder.getAbsolutePath());
//                setResult(RESULT_OK, result);
//                finish();
//            }
//        });
//
//        createButton = (Button)findViewById(R.id.createButton);
//        createButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                createNewFolder();
//            }
//        });
        
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
                Toast.makeText(getActivity(), "create_new_folder_fail", Toast.LENGTH_LONG);
            }
            browseTo(selectedFolder);
        }
    }

    private void browseToRoot() {
        browseTo(new File("/"));
    }

    private void browseTo(File current) {
        files.clear();
        upOneLevel(current);
        browse(current);
        setAdapter();
        selectCurrentFolder(current);
    }

    private void selectCurrentFolder(File current) {
        boolean isWritable = current.canWrite();
        //selectButton.setEnabled(isWritable);
        //createButton.setEnabled(isWritable);
        selectedFolder = isWritable ? current : null;
        getActivity().setTitle(current.getAbsolutePath());
    }

    private void upOneLevel(File current) {
        File parent = current.getParentFile();
        if (parent != null) {
            files.add(new OnLevelUp(parent));
        }
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

    private static class OnLevelUp extends FileItem {

        private OnLevelUp(File file) {
            super(file);
        }

        @Override
        public String toString() {
            return "..";
        }
    }
}
