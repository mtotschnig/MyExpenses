package org.totschnig.myexpenses.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.displayName
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Result

data class FileInfo(val name: String, val size: Long) {
    fun format(context: Context) = "$name (${Formatter.formatFileSize(context, size)})"
}

object AppDirHelper {
    /**
     * @return the directory user has configured in the settings, if not configured yet
     * returns [android.content.ContextWrapper.getExternalFilesDir] with argument null
     */
    fun getAppDir(context: Context): Result<DocumentFile> =
        getAppDirFromPref(context).mapCatching {
            it ?: getDefaultAppDir(context)
        }.onFailure { CrashHandler.report(it) }

    @WorkerThread
    fun getAppDirFiles(context: Context): Result<List<FileInfo>> = getAppDir(context).map {
        it.listFiles()
            .filter { (it.length() > 0) && !it.isDirectory }
            .mapNotNull { file ->
                file.name?.let {
                    Triple(it, file.length(), file.lastModified())
                }
            }
            .sortedByDescending { it.third }
            .map { FileInfo(it.first, it.second) }
    }

    fun getAppDirWithDefault(context: Context): Result<Pair<DocumentFile, Boolean>> =
        getAppDirFromPref(context).mapCatching {
            if (it != null)
                it to false
            else
                getDefaultAppDir(context) to true
        }

    private fun getAppDirFromPref(context: Context): Result<DocumentFile?> =
        runCatching {
            context.injector.prefHandler().getString(PrefKey.APP_DIR, null)?.let {
                val pref = Uri.parse(it)
                if ("file" == pref.scheme) {
                    val appDir = File(pref.path!!)
                    if (appDir.mkdir() || appDir.isDirectory) {
                        DocumentFile.fromFile(appDir)
                    } else null
                } else {
                    DocumentFile.fromTreeUri(context, pref)!!
                }
            }
        }


    private fun getDefaultAppDir(context: Context): DocumentFile = context.getExternalFilesDirs(null)
        .filterNotNull()
        .firstOrNull()
        ?.let { DocumentFile.fromFile(it) }
        ?: throw Exception("no not-null value found in getExternalFilesDirs")

    fun cacheDir(context: Context): File = context.cacheDir

    fun newWorkingDirectory(context: Context, base: String): Result<File> {
        val baseDir = cacheDir(context)
        var postfix = 0
        do {
            var name = base
            if (postfix > 0) {
                name += "_$postfix"
            }
            val result = File(baseDir, name)
            if (!result.exists()) {
                return if (result.mkdir()) {
                    Result.success(result)
                } else Result.failure(IOException("Mkdir failed"))

            }
            postfix++
        } while (true)
    }


    /**
     * @return creates a file object in parentDir, with a timestamp appended to
     * prefix as name
     */
    @JvmStatic
    fun timeStampedFile(
        parentDir: DocumentFile,
        prefix: String,
        mimeType: String,
        extension: String
    ): DocumentFile? {
        val now = SimpleDateFormat("yyyMMdd-HHmmss", Locale.US)
            .format(Date())
        val name = "$prefix-$now.$extension"
        return buildFile(
            parentDir, name, mimeType,
            allowExisting = false
        )
    }

    fun buildFile(
        parentDir: DocumentFile,
        fileName: String,
        mimeType: String,
        allowExisting: Boolean
    ): DocumentFile? {
        if (allowExisting) {
            val existingFile = parentDir.findFile(fileName)
            if (existingFile != null) {
                return existingFile
            }
        }
        var result: DocumentFile? = null
        try {
            result = parentDir.createFile(
                //RawDocumentFile adds extension based on mimeType, so call without it to prevent double extension
                if (parentDir.uri.scheme == "file") "*/*" else mimeType,
                fileName
            )
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

    fun newDirectory(parentDir: DocumentFile, base: String): DocumentFile? {
        var postfix = 0
        do {
            var name = base
            if (postfix > 0) {
                name += "_$postfix"
            }
            if (parentDir.findFile(name) == null) {
                return parentDir.createDirectory(name)
            }
            postfix++
        } while (true)
    }

    /**
     * Checks if application directory is writable. Should only be called from background
     * @param context activity or application
     * @return either positive Result or negative Result with problem description
     */
    fun checkAppDir(context: Context): Result<DocumentFile> = getAppDir(context).mapCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val uri = it.uri
            if ("file" == uri.scheme) {
                try {
                    getContentUriForFile(context, File(File(uri.path!!), "test"))
                } catch (e: IllegalArgumentException) {
                    throw localizedThrowable(
                        context,
                        R.string.app_dir_not_compatible_with_nougat,
                        uri.toString()
                    )
                }
            }
        }
        it.takeIf { isWritableDirectory(it) } ?:
        throw localizedThrowable(context, R.string.app_dir_not_accessible, it.displayName)
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