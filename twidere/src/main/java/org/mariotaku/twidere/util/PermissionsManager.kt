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

package org.mariotaku.twidere.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Process
import android.text.TextUtils
import android.text.TextUtils.isEmpty
import org.apache.commons.lang3.ArrayUtils
import org.mariotaku.ktextension.containsAll
import org.mariotaku.twidere.BuildConfig
import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.TwidereConstants.*
import java.util.*

class PermissionsManager(private val mContext: Context) : Constants {

    private val mPreferences: SharedPreferencesWrapper
    private val mPackageManager: PackageManager

    init {
        mPreferences = SharedPreferencesWrapper.getInstance(mContext, PERMISSION_PREFERENCES_NAME, Context.MODE_PRIVATE)
        mPackageManager = mContext.packageManager
    }

    fun accept(packageName: String?, permissions: Array<String>?): Boolean {
        if (packageName == null || permissions == null) return false
        val editor = mPreferences.edit()
        editor.putString(packageName, permissions.joinToString("|"))
        editor.apply()
        return true
    }

    fun checkCallingPermission(vararg requiredPermissions: String): Boolean {
        return checkPermission(Binder.getCallingUid(), *requiredPermissions)
    }

    fun checkPermission(uid: Int, vararg requiredPermissions: String): Boolean {
        if (requiredPermissions == null || requiredPermissions.size == 0) return true
        if (Process.myUid() == uid) return true
        if (checkSignature(uid)) return true
        val pname = getPackageNameByUid(uid) ?: return false
        val permissions = getPermissions(pname)
        return permissions.containsAll(requiredPermissions)
    }

    fun checkPermission(packageName: String, vararg requiredPermissions: String): Boolean {
        if (requiredPermissions == null || requiredPermissions.size == 0) return true
        if (mContext.packageName == packageName) return true
        if (checkSignature(packageName)) return true
        val permissions = getPermissions(packageName)
        return permissions.containsAll(requiredPermissions)
    }

    fun checkSignature(uid: Int): Boolean {
        val packageNameByUid = getPackageNameByUid(uid) ?: return false
        return checkSignature(packageNameByUid)
    }

    fun checkSignature(pname: String): Boolean {
        if (mContext.packageName == pname) return true
        if (BuildConfig.DEBUG) return false
        return mPackageManager.checkSignatures(pname, mContext.packageName) == PackageManager.SIGNATURE_MATCH
    }

    fun deny(packageName: String?): Boolean {
        if (packageName == null) return false
        val editor = mPreferences.edit()
        editor.putString(packageName, PERMISSION_DENIED)
        return editor.commit()

    }

    val all: Map<String, String>
        get() {
            val map = HashMap<String, String>()
            for ((key, value) in mPreferences.all) {
                if (value is String) {
                    map.put(key, value)
                }
            }
            return map
        }

    fun getPackageNameByUid(uid: Int): String? {
        return mPackageManager.getPackagesForUid(uid)?.firstOrNull()
    }

    fun getPermissions(uid: Int): Array<String> {
        val packageNameByUid = getPackageNameByUid(uid) ?: return emptyArray()
        return getPermissions(packageNameByUid)
    }

    fun getPermissions(packageName: String): Array<String> {
        val permissionsString = mPreferences.getString(packageName, null)
        if (isEmpty(permissionsString)) return emptyArray()
        if (permissionsString!!.contains(PERMISSION_DENIED)) return PERMISSIONS_DENIED
        return permissionsString.split("|").dropLastWhile(String::isEmpty).toTypedArray()
    }

    fun revoke(packageName: String?): Boolean {
        if (packageName == null) return false
        val editor = mPreferences.edit()
        editor.remove(packageName)
        return editor.commit()
    }

    fun isDenied(packageName: String): Boolean {
        return ArrayUtils.contains(getPermissions(packageName), PERMISSION_DENIED)
    }

    companion object {

        private val PERMISSIONS_DENIED = arrayOf(PERMISSION_DENIED)

        fun hasPermissions(permissions: Array<String>, vararg requiredPermissions: String): Boolean {
            return permissions.containsAll(requiredPermissions)
        }

        fun isPermissionValid(permissionsString: String): Boolean {
            return TextUtils.isEmpty(permissionsString)
        }

        fun isPermissionValid(vararg permissions: String): Boolean {
            return permissions != null && permissions.size != 0
        }

        fun parsePermissions(permissionsString: String): Array<String> {
            if (isEmpty(permissionsString)) return emptyArray()
            return permissionsString.split(SEPARATOR_PERMISSION_REGEX.toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        }
    }
}
