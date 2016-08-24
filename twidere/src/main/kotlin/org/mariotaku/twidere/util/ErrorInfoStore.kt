package org.mariotaku.twidere.util

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

import org.mariotaku.twidere.R
import org.mariotaku.twidere.model.UserKey


/**
 * Created by mariotaku on 16/1/31.
 */
class ErrorInfoStore(application: Application) {

    private val mPreferences: SharedPreferences

    init {
        mPreferences = application.getSharedPreferences("error_info", Context.MODE_PRIVATE)
    }

    operator fun get(key: String): Int {
        return mPreferences.getInt(key, 0)
    }

    operator fun get(key: String, extraId: String): Int {
        return get(key + "_" + extraId)
    }

    operator fun get(key: String, extraId: UserKey): Int {
        val host = extraId.host
        if (host == null) {
            return get(key, extraId.id)
        } else {
            return get(key + "_" + extraId.id + "_" + host)
        }
    }

    fun put(key: String, code: Int) {
        mPreferences.edit().putInt(key, code).apply()
    }

    fun put(key: String, extraId: String, code: Int) {
        put(key + "_" + extraId, code)
    }

    fun put(key: String, extraId: UserKey, code: Int) {
        val host = extraId.host
        if (host == null) {
            put(key, extraId.id, code)
        } else {
            put(key + "_" + extraId.id + "_" + host, code)
        }
    }

    fun remove(key: String, extraId: String) {
        remove(key + "_" + extraId)
    }

    fun remove(key: String, extraId: UserKey) {
        val host = extraId.host
        if (host == null) {
            remove(key, extraId.id)
        } else {
            remove(key + "_" + extraId.id + "_" + host)
        }
    }

    fun remove(key: String) {
        mPreferences.edit().remove(key).apply()
    }

    class DisplayErrorInfo(code: Int, icon: Int, message: String) {
        var code: Int = 0
            internal set
        var icon: Int = 0
            internal set
        var message: String
            internal set

        init {
            this.code = code
            this.icon = icon
            this.message = message
        }
    }

    companion object {

        val KEY_DIRECT_MESSAGES = "direct_messages"
        val KEY_INTERACTIONS = "interactions"
        val KEY_HOME_TIMELINE = "home_timeline"
        val KEY_ACTIVITIES_BY_FRIENDS = "activities_by_friends"

        val CODE_NO_DM_PERMISSION = 1
        val CODE_NO_ACCESS_FOR_CREDENTIALS = 2
        val CODE_NETWORK_ERROR = 3

        fun getErrorInfo(context: Context, code: Int): DisplayErrorInfo? {
            when (code) {
                CODE_NO_DM_PERMISSION -> {
                    return DisplayErrorInfo(code, R.drawable.ic_info_error_generic,
                            context.getString(R.string.error_no_dm_permission))
                }
                CODE_NO_ACCESS_FOR_CREDENTIALS -> {
                    return DisplayErrorInfo(code, R.drawable.ic_info_error_generic,
                            context.getString(R.string.error_no_access_for_credentials))
                }
                CODE_NETWORK_ERROR -> {
                    return DisplayErrorInfo(code, R.drawable.ic_info_error_generic,
                            context.getString(R.string.network_error))
                }
            }
            return null
        }
    }
}
