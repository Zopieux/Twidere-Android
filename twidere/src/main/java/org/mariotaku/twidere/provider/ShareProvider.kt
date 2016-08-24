package org.mariotaku.twidere.provider

import android.Manifest
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore.MediaColumns
import android.support.v4.content.ContextCompat

import org.apache.commons.lang3.ArrayUtils

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Created by mariotaku on 16/4/4.
 */
class ShareProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        var projection = projection
        try {
            val file = getFile(uri) ?: return null
            if (projection == null) {
                projection = COLUMNS
            }
            val cursor = MatrixCursor(projection, 1)
            val values = arrayOfNulls<Any>(projection.size)
            writeValue(projection, values, MediaColumns.DATA, file.absolutePath)
            cursor.addRow(values)
            return cursor
        } catch (e: IOException) {
            return null
        }

    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r") throw IllegalArgumentException()
        val file = getFile(uri)
        return ParcelFileDescriptor.open(file,
                ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun writeValue(columns: Array<String>, values: Array<Any?>, column: String, value: Any) {
        val idx = ArrayUtils.indexOf(columns, column)
        if (idx != ArrayUtils.INDEX_NOT_FOUND) {
            values[idx] = value
        }
    }

    @Throws(FileNotFoundException::class)
    private fun getFile(uri: Uri): File? {
        val lastPathSegment = uri.lastPathSegment ?: throw FileNotFoundException(uri.toString())
        return File(getFilesDir(context), lastPathSegment)
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    companion object {
        val COLUMNS = arrayOf(MediaColumns.DATA, MediaColumns.DISPLAY_NAME, MediaColumns.SIZE, MediaColumns.MIME_TYPE)

        fun getFilesDir(context: Context): File? {
            var cacheDir: File? = context.cacheDir
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                val externalCacheDir = context.externalCacheDir
                if (externalCacheDir != null && externalCacheDir.canWrite()) {
                    cacheDir = externalCacheDir
                }
            }
            if (cacheDir == null) return null
            return File(cacheDir, "shared_files")
        }

        fun getUriForFile(context: Context, authority: String, file: File): Uri? {
            val filesDir = getFilesDir(context) ?: return null
            if (filesDir != file.parentFile) return null
            return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).appendPath(file.name).build()
        }

        fun clearTempFiles(context: Context): Boolean {
            val externalCacheDir = context.externalCacheDir ?: return false
            val files = externalCacheDir.listFiles()
            for (file in files) {
                if (file.isFile) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete()
                }
            }
            return true
        }
    }
}
