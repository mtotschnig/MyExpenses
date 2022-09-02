package org.totschnig.myexpenses.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Result

object AppDirHelper {
    /**
     * @return the directory user has configured in the settings, if not configured yet
     * returns [android.content.ContextWrapper.getExternalFilesDir] with argument null
     */
    @JvmStatic
    fun getAppDir(context: Context): DocumentFile? {
        val prefString = PrefKey.APP_DIR.getString(null)
        if (prefString != null) {
            val pref = Uri.parse(prefString)
            if ("file" == pref.scheme) {
                val appDir = File(pref.path!!)
                if (appDir.mkdir() || appDir.isDirectory) {
                    return DocumentFile.fromFile(appDir)
                }
            } else {
                return DocumentFile.fromTreeUri(context, pref)
            }
        }
        val externalFilesDir = context.getExternalFilesDirs(null).filterNotNull().firstOrNull()
        return if (externalFilesDir != null) {
            DocumentFile.fromFile(externalFilesDir)
        } else {
            CrashHandler.report(Exception("no not-null value found in getExternalFilesDirs"))
            null
        }
    }

    @JvmStatic
    fun cacheDir(context: Context): File {
        val external = ContextCompat.getExternalCacheDirs(context)[0]
        return if (external != null && external.canWrite()) external else context.cacheDir
    }

    /**
     * @return creates a file object in parentDir, with a timestamp appended to
     * prefix as name, if the file already exists it appends a numeric
     * postfix
     */
    @JvmStatic
    fun timeStampedFile(
        parentDir: DocumentFile, prefix: String,
        mimeType: String, addExtension: String?
    ): DocumentFile? {
        val now = SimpleDateFormat("yyyMMdd-HHmmss", Locale.US)
            .format(Date())
        var name = "$prefix-$now"
        if (addExtension != null) {
            name += ".$addExtension"
        }
        return buildFile(parentDir, name, mimeType, false, false)
    }

    fun buildFile(
        parentDir: DocumentFile, fileName: String,
        mimeType: String, allowExisting: Boolean,
        supplementExtension: Boolean
    ): DocumentFile? {
        //createFile supplements extension on known mimeTypes, if the mime type is not known, we take care of it
        val supplementedFilename = String.format(
            Locale.ROOT,
            "%s.%s",
            fileName,
            mimeType.split("/".toRegex()).toTypedArray()[1]
        )
        var fileNameToCreate = fileName
        if (supplementExtension) {
            val extensionFromMimeType =
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (extensionFromMimeType == null) {
                fileNameToCreate = supplementedFilename
            }
        }
        if (allowExisting) {
            val existingFile = parentDir.findFile(supplementedFilename)
            if (existingFile != null) {
                return existingFile
            }
        }
        var result: DocumentFile? = null
        try {
            result = parentDir.createFile(mimeType, fileNameToCreate)
            if (result == null || !result.canWrite()) {
                val message =
                    if (result == null) "createFile returned null" else "createFile returned unwritable file"
                CrashHandler.report(
                    Throwable(message), mapOf(
                        "mimeType" to mimeType,
                        "name" to fileName,
                        "parent" to parentDir.uri.toString()
                    )
                )
            }
        } catch (e: SecurityException) {
            CrashHandler.report(e)
        }
        return result
    }

    fun newDirectory(parentDir: DocumentFile, base: String?): DocumentFile? {
        var postfix = 0
        do {
            var name = base
            if (postfix > 0) {
                name += "_$postfix"
            }
            if (parentDir.findFile(name!!) == null) {
                return parentDir.createDirectory(name)
            }
            postfix++
        } while (true)
    }

    /**
     * Helper Method to Test if external Storage is Available from
     * http://www.ibm.com/developerworks/xml/library/x-androidstorage/index.html
     */
    val isExternalStorageAvailable: Boolean
        get() = Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()

    /**
     * Chechs is application directory is writable. Should only be called from background
     * @param context activity or application
     * @return either positive Result or negative Result with problem description
     */
    @JvmStatic
    fun checkAppDir(context: Context): Result<DocumentFile> {
        if (!isExternalStorageAvailable) {
            return Result.failure(context, R.string.external_storage_unavailable)
        }
        val appDir = getAppDir(context)
            ?: return Result.failure(context, R.string.io_error_appdir_null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val uri = appDir.uri
            if ("file" == uri.scheme) {
                try {
                    getContentUriForFile(context, File(File(uri.path!!), "test"))
                } catch (e: IllegalArgumentException) {
                    return Result.failure(
                        context,
                        R.string.app_dir_not_compatible_with_nougat,
                        uri.toString()
                    )
                }
            }
        }
        return if (isWritableDirectory(appDir)) Result.success(appDir) else Result.failure(
            context,
            R.string.app_dir_not_accessible, FileUtils.getPath(context, appDir.uri)
        )
    }

    fun isWritableDirectory(appDir: DocumentFile): Boolean {
        return appDir.exists() && appDir.isDirectory && appDir.canWrite()
    }

    @JvmStatic
    fun ensureContentUri(uri: Uri, context: Context): Uri = when (uri.scheme) {
        "file" -> try {
            getContentUriForFile(context, File(uri.path!!))
        } catch (e: IllegalArgumentException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                throw NougatFileProviderException(e)
            }
            uri
        }
        "content" -> uri
        else -> {
            CrashHandler.report(
                IllegalStateException(
                    String.format(
                        "Unable to handle scheme of uri %s", uri
                    )
                )
            )
            uri
        }
    }

    @JvmStatic
    fun getContentUriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, getFileProviderAuthority(context), file)

    @JvmStatic
    fun getFileProviderAuthority(context: Context): String =
        context.packageName + ".fileprovider"
}