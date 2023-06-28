package org.totschnig.myexpenses.util.io

object FileUtils {
    /**
     * Gets the extension of a file name, like ".png" or ".jpg".
     * @return Extension without the dot("."); "" if there is no extension;
     * null if uri was null.
     * originally based on //https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
     */
    @JvmStatic
    fun getExtension(uri: String): String {
        val dot = uri.lastIndexOf(".")
        return if (dot >= 0) {
            uri.substring(dot + 1)
        } else {
            // No extension.
            ""
        }
    }
}