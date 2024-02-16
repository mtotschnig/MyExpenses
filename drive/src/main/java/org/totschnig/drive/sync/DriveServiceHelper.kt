/**
 * Copyright 2018 Google LLC
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.totschnig.drive.sync

import android.accounts.Account
import android.content.Context
import com.google.api.client.extensions.android.json.AndroidJsonFactory
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.*
import javax.inject.Inject

/**
 * A utility for performing read/write operations on Drive files via the REST API
 */

const val MIME_TYPE_FOLDER = "application/vnd.google-apps.folder"

class DriveServiceHelper(context: Context, accountName: String) {
    private val mDriveService: Drive

    @Inject
    lateinit var crashHandler: CrashHandler

    init {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, setOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = Account(accountName, "com.google")
        mDriveService = Drive.Builder(
            NetHttpTransport(),
            AndroidJsonFactory(),
            credential
        )
            .setApplicationName(context.getString(R.string.app_name))
            .build()
        DaggerDriveComponent.builder().appComponent(MyApplication.instance.appComponent).build()
            .inject(this);

    }

    @Throws(IOException::class)
    fun createFolder(parent: String, name: String, properties: Map<String, String>?): File {
        return createFile(parent, name, MIME_TYPE_FOLDER, properties)
    }

    fun isFolder(file: File) = file.mimeType.equals(MIME_TYPE_FOLDER)

    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */
    @Throws(IOException::class)
    fun createFile(
        parent: String,
        name: String,
        mimeType: String,
        properties: Map<String, String>?
    ): File {
        val metadata = File()
            .setParents(listOf(parent))
            .setMimeType(mimeType)
            .setAppProperties(properties)
            .setName(name)

        return mDriveService.files().create(metadata).execute()
            ?: throw IOException("Null result when requesting file creation.")
    }

    /**
     * Updates the file identified by `fileId` with the given `name` and `content`.
     */
    @Throws(IOException::class)
    fun saveFile(fileId: String, mimeType: String, content: InputStream) {
        // Convert content to an AbstractInputStreamContent instance.
        val contentStream = InputStreamContent(mimeType, content)

        // Update the metadata and contents.
        try {
            mDriveService.files().update(fileId, null, contentStream).execute()
        } catch (e: GoogleJsonResponseException) {
            throw if (e.statusCode != 403 && e.details?.errors?.getOrNull(0)?.reason == "storageQuotaExceeded") {
                IOException(e.details.message, e)
            } else e.also {
                CrashHandler.report(it)
            }
        }
    }

    @Throws(IOException::class)
    fun setMetadataProperty(fileId: String, key: String, value: String?) {
        val metadata = File().apply {
            appProperties = mapOf(Pair(key, value ?: ""))
        }
        mDriveService.files().update(fileId, metadata).execute()
    }

    @Throws(IOException::class)
    fun listFolders(parent: File? = null, vararg queries: String) = search(
        parent?.id,
        *queries, "mimeType = 'application/vnd.google-apps.folder'"
    )

    @Throws(IOException::class)
    fun listChildren(parent: File) = search(parent.id)

    @Throws(IOException::class)
    private fun search(parentId: String?, vararg queries: String): List<File> {
        val result = mutableListOf<File>()
        var pageToken: String? = null
        val queryList = queries.toMutableList()
        parentId?.let {
            queryList.add("'%s' in parents".format(Locale.ROOT, parentId))
        }
        do {
            val fileList = mDriveService.files().list().apply {
                q = queryList.joinToString(" and ")
                spaces = "drive"
                fields = "nextPageToken, files(id, name, mimeType, appProperties, size)"
                this.pageToken = pageToken
            }.execute()
            pageToken = fileList.nextPageToken
            result.addAll(fileList.files)
        } while (pageToken != null)
        return result
    }

    @Throws(IOException::class)
    fun getFile(fileId: String): File {
        return mDriveService.files().get(fileId).execute()
    }

    @Throws(IOException::class)
    fun getFileByNameAndParent(parent: File, name: String): File? {
        val result = search(parent.id, "name = '%s'".format(Locale.ROOT, name))
        return if (result.isNotEmpty()) result[0] else null
    }

    @Throws(IOException::class)
    fun downloadFile(parent: File, name: String): InputStream {
        getFileByNameAndParent(parent, name)?.let {
            return read(it.id)
        } ?: throw FileNotFoundException()
    }

    @Throws(IOException::class)
    fun delete(fileId: String) {
        mDriveService.files().delete(fileId).execute()
    }

    @Throws(IOException::class)
    fun read(fileId: String): InputStream {
        return mDriveService.files().get(fileId).executeMediaAsInputStream()
    }
}
