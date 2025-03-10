package org.totschnig.myexpenses.util

import android.content.Context
import android.text.TextUtils
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.fileName
import org.totschnig.myexpenses.provider.getBackupDataStoreFile
import org.totschnig.myexpenses.provider.getBackupDbFile
import org.totschnig.myexpenses.provider.getBackupPrefFile
import org.totschnig.myexpenses.util.crypt.EncryptionHelper
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {
    const val PICTURES = "Pictures"
    @Throws(IOException::class, GeneralSecurityException::class)
    fun zipBackup(
        context: Context,
        cacheDir: File?,
        destZipFile: DocumentFile,
        password: String?,
        lenientMode: Boolean,
    ) {
        val resolver = context.contentResolver
        val out = resolver.openOutputStream(destZipFile.uri)
        val zip = ZipOutputStream(
            if (TextUtils.isEmpty(password)) out else EncryptionHelper.encrypt(
                out!!, password
            )
        )
        addFileToZip("", getBackupDbFile(cacheDir), zip)
        addFileToZip("", getBackupPrefFile(cacheDir), zip)
        val backupDataStoreFile = getBackupDataStoreFile(cacheDir)
        if (backupDataStoreFile.exists()) {
            addFileToZip("", backupDataStoreFile, zip)
        }
        try {
            resolver
                .query(
                    TransactionProvider.ATTACHMENTS_URI,
                    arrayOf(KEY_ROWID, KEY_URI),
                    null,
                    null,
                    null
                )
        } catch (e: Exception) {
            if (lenientMode) null else throw e
        }?.use {
                it.asSequence.forEach { cursor ->
                    val rowId = cursor.getLong(0)
                    val uri = cursor.getString(1).toUri()
                    val fileName = "${rowId}_${uri.fileName(context)}"
                    try {
                        resolver.openInputStream(uri)?.use { inputStream ->
                            addInputStreamToZip(
                                "$PICTURES/$fileName",
                                inputStream,
                                zip
                            )
                        }
                    } catch (_: FileNotFoundException) {
                        //File has been removed
                        //Timber.e(e);
                    }
                }
            }
        zip.flush()
        zip.close()
    }

    /*
   * recursively add files to the zip files
   */
    @Throws(IOException::class)
    private fun addFileToZip(
        path: String, srcFile: File,
        zip: ZipOutputStream,
    ) {
        FileInputStream(srcFile).use {
            val filePath = path + (if (path == "") "" else "/") + srcFile.name
            addInputStreamToZip(filePath, it, zip)
        }
    }

    @Throws(IOException::class)
    private fun addInputStreamToZip(
        path: String, inputStream: InputStream,
        zip: ZipOutputStream,
    ) {
        val buf = ByteArray(1024)
        var len: Int
        zip.putNextEntry(ZipEntry(path))
        while (inputStream.read(buf).also { len = it } > 0) {
            zip.write(buf, 0, len)
        }
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    fun unzip(fileIn: InputStream?, dirOut: File, password: String?) {
        val zin = ZipInputStream(
            if (TextUtils.isEmpty(password)) fileIn else EncryptionHelper.decrypt(
                fileIn,
                password
            )
        )
        while (true) {
            val ze = zin.nextEntry ?: break
            Timber.v("Unzipping %s", ze.name)
            val newFile = File(dirOut, ze.name)
            val canonicalPath = newFile.canonicalPath
            if (!canonicalPath.startsWith(dirOut.canonicalPath)) {
                throw SecurityException("Path Traversal Vulnerability")
            }
            newFile.parentFile.mkdirs()
            if (ze.isDirectory) {
                newFile.mkdir()
            } else {
                val fout = FileOutputStream(newFile)
                val startTime = System.currentTimeMillis()
                val buffer = ByteArray(1024)
                var count: Int
                while (zin.read(buffer).also { count = it } != -1) {
                    fout.write(buffer, 0, count)
                }
                val endTime = System.currentTimeMillis()
                Timber.d("That took %d milliseconds", endTime - startTime)
                zin.closeEntry()
                fout.close()
            }
        }
        zin.close()
    }
}