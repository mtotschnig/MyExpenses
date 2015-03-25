package org.totschnig.myexpenses.test.provider;

import org.totschnig.myexpenses.provider.DatabaseConstants;

import android.content.ContentValues;

// A utility for converting note data to a ContentValues map.
class CategoryInfo {
    String label;
    Long parentId;
    /*
     * Constructor for a NoteInfo instance. This class helps create a note and
     * return its values in a ContentValues map expected by data model methods.
     * The note's id is created automatically when it is inserted into the data model.
     */
    public CategoryInfo(String label, Long parentId) {
      this.label = label;
      this.parentId = parentId;
    }

    /*
     * Returns a ContentValues instance (a map) for this NoteInfo instance. This is useful for
     * inserting a NoteInfo into a database.
     */
    public ContentValues getContentValues() {
        // Gets a new ContentValues object
        ContentValues v = new ContentValues();

        // Adds map entries for the user-controlled fields in the map
        v.put(DatabaseConstants.KEY_LABEL, label);
        if (parentId != null)
          v.put(DatabaseConstants.KEY_PARENTID, parentId);
        return v;

    }
}