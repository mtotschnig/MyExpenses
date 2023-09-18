package org.totschnig.myexpenses.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.exception.ExternalStorageNotAvailableException
import org.totschnig.myexpenses.util.AppDirHelper.cacheDir
import org.totschnig.myexpenses.util.AppDirHelper.getContentUriForFile
import org.totschnig.myexpenses.util.AppDirHelper.getFileProviderAuthority
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PictureDirHelper {
    fun getOutputMediaFile(
        fileName: String,
        temp: Boolean,
        checkUnique: Boolean,
        application: MyApplication,
        extension: String = "jpg"
    ) = getOutputMediaFile(
        fileName,
        temp,
        application,
        checkUnique,
        extension
    )

    /**
     * create a File object for storage of picture data
     *
     * @param temp if true the returned file is suitable for temporary storage while
     * the user is editing the transaction if false the file will serve
     * as permanent storage,
     * care is taken that the file does not yet exist
     * @return a file on the external storage
     */
    private fun getOutputMediaFile(
        fileName: String,
        temp: Boolean,
        application: MyApplication,
        checkUnique: Boolean,
        extension: String? = "jpg"
    ) = getOutputMediaFile(
        fileName = fileName,
        mediaStorageDir = (
                if (temp) cacheDir(application) else
                    getPictureDir(application, application.isProtected)
                ) ?: throw ExternalStorageNotAvailableException(),
        checkUnique = checkUnique,
        extension = extension
    )

    private fun getOutputMediaFile(
        fileName: String,
        mediaStorageDir: File,
        checkUnique: Boolean,
        extension: String?
    ): File {
        var postfix = 0
        var result: File
        do {
            result = File(mediaStorageDir, getOutputMediaFileName(fileName, postfix, extension))
            postfix++
        } while (checkUnique && result.exists())
        return result
    }

    val defaultFileName
        get() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())


    @JvmOverloads
    @JvmStatic
    fun getOutputMediaUri(
        temp: Boolean,
        application: MyApplication,
        prefix: String = "",
        fileName: String = prefix + defaultFileName,
        extension: String? = "jpg"
    ): Uri {
        val outputMediaFile = getOutputMediaFile(fileName, temp, application, true, extension)
        return try {
            getContentUriForFile(application, outputMediaFile)
        } catch (e: IllegalArgumentException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !temp) {
                throw NougatFileProviderException(e)
            }
            Uri.fromFile(outputMediaFile)
        }
    }

    fun getPictureUriBase(temp: Boolean, application: MyApplication) =
        getOutputMediaUri(temp, application).toString()
            .substring(
                0, getOutputMediaUri(temp, application).toString().lastIndexOf('/')
            )

    private fun getOutputMediaFileName(base: String, postfix: Int, extension: String?) =
        "$base${
            if (postfix > 0) {
                "_$postfix"
            } else ""
        }${extension?.let { ".$it" } ?: "" }"

    @JvmStatic
    fun getPictureDir(context: Context, secure: Boolean): File? {
        val result: File? = if (secure) {
            File(context.filesDir, "images")
        } else {
            //https://stackoverflow.com/a/43497841/1199911
            ContextCompat.getExternalFilesDirs(context, Environment.DIRECTORY_PICTURES)[0]
        }
        if (result == null) return null
        result.mkdir()
        return if (result.exists()) result else null
    }

    @Throws(IllegalArgumentException::class)
    fun doesPictureExist(context: Context, pictureUri: Uri): Boolean {
        return getFileForUri(context, pictureUri).exists()
    }

    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun getFileForUri(context: Context, pictureUri: Uri): File {
        if ("file" == pictureUri.scheme) {
            return File(pictureUri.path!!)
        }
        Preconditions.checkArgument(
            "authority",
            getFileProviderAuthority(context),
            pictureUri.authority
        )
        val pathSegments = pictureUri.pathSegments
        //TODO create unit test for checking if this logic is in sync with image_path.xml
        return when (val pathDomain = pathSegments[0]) {
            "external-files" -> {
                if (pathSegments[1] != Environment.DIRECTORY_PICTURES) {
                    report(Exception("Access to external-files outside pictures"))
                }
                File(getPictureDir(context, false), pathSegments[2])
            }

            "images" -> File(getPictureDir(context, true), pathSegments[1])

            "cache" -> File(context.cacheDir, pathSegments[1])
            else -> throw IllegalArgumentException(
                String.format(
                    Locale.ROOT,
                    "Unable to handle %s",
                    pathDomain
                )
            )
        }
    }
}