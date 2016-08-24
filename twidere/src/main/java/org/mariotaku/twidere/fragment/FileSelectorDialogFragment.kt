/*
 * 				Twidere - Twitter client for Android
 * 
 *  Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.res.Resources
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.AsyncTaskLoader
import android.support.v4.content.Loader
import android.support.v4.content.res.ResourcesCompat
import android.text.TextUtils.TruncateAt
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.TextView
import org.mariotaku.twidere.R
import org.mariotaku.twidere.adapter.ArrayAdapter
import org.mariotaku.twidere.constant.IntentConstants.*
import org.mariotaku.twidere.fragment.iface.ISupportDialogFragmentCallback
import org.mariotaku.twidere.util.ThemeUtils
import java.io.File
import java.util.*
import java.util.regex.Pattern

class FileSelectorDialogFragment : BaseDialogFragment(), LoaderCallbacks<List<File>>, OnClickListener, OnItemClickListener {

    private var mAdapter: FilesAdapter? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val args = arguments
        loaderManager.initLoader(0, args, this)
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        val a = activity
        if (a is Callback) {
            a.onCancelled(this)
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                val a = activity
                if (isPickDirectory && a is Callback) {
                    a.onFilePicked(currentDirectory!!)
                }
            }//                dismiss();
            DialogInterface.BUTTON_NEGATIVE -> {
                val a = activity
                if (a is Callback) {
                    a.onCancelled(this)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mAdapter = FilesAdapter(activity)
        val builder = AlertDialog.Builder(activity)
        builder.setAdapter(mAdapter, this)
        builder.setTitle(R.string.pick_file)
        builder.setNegativeButton(android.R.string.cancel, this)
        if (isPickDirectory) {
            builder.setPositiveButton(android.R.string.ok, this)
        }
        val dialog = builder.create()
        val listView = dialog.listView
        listView.onItemClickListener = this
        return dialog
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<List<File>> {
        val extensions = args.getStringArray(EXTRA_FILE_EXTENSIONS)
        val path = args.getString(EXTRA_PATH)
        var currentDir: File? = if (path != null) File(path) else getExternalStorageDirectory()
        if (currentDir == null) {
            currentDir = File("/")
        }
        arguments.putString(EXTRA_PATH, currentDir.absolutePath)
        return FilesLoader(activity, currentDir, extensions)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        val a = activity
        if (a is Callback) {
            a.onDismissed(this)
        }
    }

    override fun onItemClick(view: AdapterView<*>, child: View, position: Int, id: Long) {
        val file = mAdapter!!.getItem(position) ?: return
        if (file.isDirectory) {
            val args = arguments
            args.putString(EXTRA_PATH, file.absolutePath)
            loaderManager.restartLoader(0, args, this)
        } else if (file.isFile && !isPickDirectory) {
            val a = activity
            if (a is Callback) {
                a.onFilePicked(file)
            }
            dismiss()
        }
    }

    override fun onLoaderReset(loader: Loader<List<File>>) {
        mAdapter!!.setData(null, null)
    }

    override fun onLoadFinished(loader: Loader<List<File>>, data: List<File>) {
        val currentDir = currentDirectory
        if (currentDir != null) {
            mAdapter!!.setData(currentDir, data)
            if (currentDir.parent == null) {
                setTitle("/")
            } else {
                setTitle(currentDir.name)
            }
        }
    }

    private val currentDirectory: File?
        get() {
            val args = arguments
            val path = args.getString(EXTRA_PATH)
            return if (path != null) File(path) else null
        }

    private val isPickDirectory: Boolean
        get() {
            val args = arguments
            val action = args?.getString(EXTRA_ACTION)
            return INTENT_ACTION_PICK_DIRECTORY == action
        }

    private fun setTitle(title: CharSequence) {
        val dialog = dialog ?: return
        dialog.setTitle(title)
    }

    interface Callback : ISupportDialogFragmentCallback {

        fun onFilePicked(file: File)
    }

    private class FilesAdapter(context: Context) : ArrayAdapter<File>(context, android.R.layout.simple_list_item_1) {

        private val mPadding: Int
        private val mActionIconColor: Int
        private val mResources: Resources

        private var mCurrentPath: File? = null

        init {
            mResources = context.resources
            mActionIconColor = if (!ThemeUtils.isLightTheme(context)) 0xffffffff.toInt() else 0xc0333333.toInt()
            mPadding = (4 * mResources.displayMetrics.density).toInt()
        }

        override fun getItemId(position: Int): Long {
            return getItem(position).hashCode().toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val text = (if (view is TextView) view else view.findViewById(android.R.id.text1)) as TextView
            val file = getItem(position)
            if (file == null || text == null) return view
            if (mCurrentPath != null && file == mCurrentPath!!.parentFile) {
                text.text = ".."
            } else {
                text.text = file.name
            }
            text.setSingleLine(true)
            text.ellipsize = TruncateAt.MARQUEE
            text.setPadding(mPadding, mPadding, position, mPadding)
            val icon = ResourcesCompat.getDrawable(mResources,
                    if (file.isDirectory) R.drawable.ic_folder else R.drawable.ic_file, null)!!
            icon.mutate()
            icon.setColorFilter(mActionIconColor, PorterDuff.Mode.SRC_ATOP)
            text.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
            return view
        }

        fun setData(current: File?, data: List<File>?) {
            mCurrentPath = current
            clear()
            if (data != null) {
                addAll(data)
            }
        }

    }

    private class FilesLoader(context: Context, private val path: File?, private val extensions: Array<String>?) : AsyncTaskLoader<List<File>>(context) {
        private val extensions_regex: Pattern?

        init {
            extensions_regex = if (extensions != null)
                Pattern.compile(extensions.joinToString("|"), Pattern.CASE_INSENSITIVE)
            else
                null
        }

        override fun loadInBackground(): List<File> {
            if (path == null || !path.isDirectory) return emptyList()
            val listed_files = path.listFiles() ?: return emptyList()
            val dirs = ArrayList<File>()
            val files = ArrayList<File>()
            for (file in listed_files) {
                if (!file.canRead() || file.isHidden) {
                    continue
                }
                if (file.isDirectory) {
                    dirs.add(file)
                } else if (file.isFile) {
                    val name = file.name
                    val idx = name.lastIndexOf(".")
                    if (extensions == null || extensions.size == 0 || idx == -1 || idx > -1 && extensions_regex!!.matcher(name.substring(idx + 1)).matches()) {
                        files.add(file)
                    }
                }
            }
            Collections.sort(dirs, NAME_COMPARATOR)
            Collections.sort(files, NAME_COMPARATOR)
            val list = ArrayList<File>()
            val parent = path.parentFile
            if (path.parentFile != null) {
                list.add(parent)
            }
            list.addAll(dirs)
            list.addAll(files)
            return list
        }

        override fun onStartLoading() {
            forceLoad()
        }

        override fun onStopLoading() {
            cancelLoad()
        }

        companion object {

            private val NAME_COMPARATOR = Comparator<java.io.File> { file1, file2 ->
                val loc = Locale.getDefault()
                file1.name.toLowerCase(loc).compareTo(file2.name.toLowerCase(loc))
            }
        }
    }

}
