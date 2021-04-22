package org.totschnig.myexpenses.model

import android.content.ContentResolver
import android.net.Uri
import org.totschnig.myexpenses.viewmodel.data.Tag

interface IAccount: IModel {

    fun saveTags(tags: List<Tag>?, contentResolver: ContentResolver): Boolean
}