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

package org.mariotaku.twidere.preference

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.preference.DialogPreference
import android.support.v7.preference.PreferenceDialogFragmentCompat
import android.support.v7.preference.PreferenceFragmentCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.*
import org.mariotaku.kpreferences.KPreferences
import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.R
import org.mariotaku.twidere.activity.iface.APIEditorActivity
import org.mariotaku.twidere.constant.SharedPreferenceConstants.*
import org.mariotaku.twidere.constant.defaultAPIConfigKey
import org.mariotaku.twidere.fragment.ThemedPreferenceDialogFragmentCompat
import org.mariotaku.twidere.model.ParcelableCredentials
import org.mariotaku.twidere.preference.iface.IDialogPreference
import org.mariotaku.twidere.provider.TwidereDataStore.Accounts
import org.mariotaku.twidere.util.ParseUtils

class DefaultAPIPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyle: Int = R.attr.dialogPreferenceStyle) : DialogPreference(context, attrs, defStyle), Constants, IDialogPreference {

    init {
        dialogLayoutResource = R.layout.layout_api_editor
    }

    override fun displayDialog(fragment: PreferenceFragmentCompat) {
        val df = DefaultAPIPreferenceDialogFragment.newInstance(key)
        df.setTargetFragment(fragment, 0)
        df.show(fragment.fragmentManager, key)
    }

    class DefaultAPIPreferenceDialogFragment : ThemedPreferenceDialogFragmentCompat() {

        private var mEditAPIUrlFormat: EditText? = null
        private var mEditSameOAuthSigningUrl: CheckBox? = null
        private var mEditNoVersionSuffix: CheckBox? = null
        private var mEditConsumerKey: EditText? = null
        private var mEditConsumerSecret: EditText? = null
        private var mEditAuthType: RadioGroup? = null
        private var mButtonOAuth: RadioButton? = null
        private var mButtonxAuth: RadioButton? = null
        private var mButtonBasic: RadioButton? = null
        private var mButtonTwipOMode: RadioButton? = null
        private var mAPIFormatHelpButton: View? = null
        private var mEditNoVersionSuffixChanged: Boolean = false

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val preference = preference
            val dialog = super.onCreateDialog(savedInstanceState)
            dialog.setOnShowListener { dialog ->
                val editDialog = dialog as Dialog
                mEditAPIUrlFormat = editDialog.findViewById(R.id.editApiUrlFormat) as EditText
                mEditAuthType = editDialog.findViewById(R.id.editAuthType) as RadioGroup
                mButtonOAuth = editDialog.findViewById(R.id.oauth) as RadioButton
                mButtonxAuth = editDialog.findViewById(R.id.xauth) as RadioButton
                mButtonBasic = editDialog.findViewById(R.id.basic) as RadioButton
                mButtonTwipOMode = editDialog.findViewById(R.id.twipO) as RadioButton
                mEditSameOAuthSigningUrl = editDialog.findViewById(R.id.editSameOAuthSigningUrl) as CheckBox
                mEditNoVersionSuffix = editDialog.findViewById(R.id.editNoVersionSuffix) as CheckBox
                mEditConsumerKey = editDialog.findViewById(R.id.editConsumerKey) as EditText
                mEditConsumerSecret = editDialog.findViewById(R.id.editConsumerSecret) as EditText
                mAPIFormatHelpButton = editDialog.findViewById(R.id.apiUrlFormatHelp)

                mEditNoVersionSuffix!!.setOnCheckedChangeListener { buttonView, isChecked -> mEditNoVersionSuffixChanged = true }
                mEditAuthType!!.setOnCheckedChangeListener { group, checkedId ->
                    val authType = APIEditorActivity.getCheckedAuthType(checkedId)
                    val isOAuth = authType == ParcelableCredentials.AuthType.OAUTH || authType == ParcelableCredentials.AuthType.XAUTH
                    mEditSameOAuthSigningUrl!!.visibility = if (isOAuth) View.VISIBLE else View.GONE
                    mEditConsumerKey!!.visibility = if (isOAuth) View.VISIBLE else View.GONE
                    mEditConsumerSecret!!.visibility = if (isOAuth) View.VISIBLE else View.GONE
                    if (!mEditNoVersionSuffixChanged) {
                        mEditNoVersionSuffix!!.isChecked = authType == ParcelableCredentials.AuthType.TWIP_O_MODE
                    }
                }
                mAPIFormatHelpButton!!.setOnClickListener { Toast.makeText(context, R.string.api_url_format_help, Toast.LENGTH_LONG).show() }

                if (savedInstanceState != null) {
                    val apiUrlFormat = savedInstanceState.getString(Accounts.API_URL_FORMAT)
                    val authType = savedInstanceState.getInt(Accounts.AUTH_TYPE)
                    val sameOAuthSigningUrl = savedInstanceState.getBoolean(Accounts.SAME_OAUTH_SIGNING_URL)
                    val noVersionSuffix = savedInstanceState.getBoolean(Accounts.NO_VERSION_SUFFIX)
                    val consumerKey = savedInstanceState.getString(Accounts.CONSUMER_KEY)?.trim()
                    val consumerSecret = savedInstanceState.getString(Accounts.CONSUMER_SECRET)?.trim()
                    setValues(apiUrlFormat, authType, sameOAuthSigningUrl, noVersionSuffix, consumerKey, consumerSecret)
                } else {
                    val preferences = KPreferences(preference.sharedPreferences)
                    val defaultAPIConfig = preferences[defaultAPIConfigKey]
                    setValues(defaultAPIConfig.apiUrlFormat, defaultAPIConfig.authType,
                            defaultAPIConfig.isSameOAuthUrl, defaultAPIConfig.isNoVersionSuffix,
                            defaultAPIConfig.consumerKey, defaultAPIConfig.consumerSecret)
                }
            }
            return dialog
        }

        override fun onDialogClosed(positiveResult: Boolean) {
            if (!positiveResult) return
            val preference = preference as DefaultAPIPreference
            val preferences = preference.sharedPreferences

            val apiUrlFormat = ParseUtils.parseString(mEditAPIUrlFormat!!.text)
            val authType = APIEditorActivity.getCheckedAuthType(mEditAuthType!!.checkedRadioButtonId)
            val sameOAuthSigningUrl = mEditSameOAuthSigningUrl!!.isChecked
            val noVersionSuffix = mEditNoVersionSuffix!!.isChecked
            val consumerKey = ParseUtils.parseString(mEditConsumerKey!!.text)
            val consumerSecret = ParseUtils.parseString(mEditConsumerSecret!!.text)
            val editor = preferences.edit()
            if (!TextUtils.isEmpty(consumerKey) && !TextUtils.isEmpty(consumerSecret)) {
                editor.putString(KEY_CONSUMER_KEY, consumerKey)
                editor.putString(KEY_CONSUMER_SECRET, consumerSecret)
            } else {
                editor.remove(KEY_CONSUMER_KEY)
                editor.remove(KEY_CONSUMER_SECRET)
            }
            editor.putString(KEY_API_URL_FORMAT, apiUrlFormat)
            editor.putInt(KEY_AUTH_TYPE, authType)
            editor.putBoolean(KEY_SAME_OAUTH_SIGNING_URL, sameOAuthSigningUrl)
            editor.putBoolean(KEY_NO_VERSION_SUFFIX, noVersionSuffix)
            editor.apply()
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putString(Accounts.API_URL_FORMAT, ParseUtils.parseString(mEditAPIUrlFormat!!.text))
            outState.putInt(Accounts.AUTH_TYPE, APIEditorActivity.getCheckedAuthType(mEditAuthType!!.checkedRadioButtonId))
            outState.putBoolean(Accounts.SAME_OAUTH_SIGNING_URL, mEditSameOAuthSigningUrl!!.isChecked)
            outState.putString(Accounts.CONSUMER_KEY, ParseUtils.parseString(mEditConsumerKey!!.text))
            outState.putString(Accounts.CONSUMER_SECRET, ParseUtils.parseString(mEditConsumerSecret!!.text))
        }

        private fun setValues(apiUrlFormat: String, authType: Int, sameOAuthSigningUrl: Boolean,
                              noVersionSuffix: Boolean, consumerKey: String?, consumerSecret: String?) {
            mEditAPIUrlFormat!!.setText(apiUrlFormat)
            mEditSameOAuthSigningUrl!!.isChecked = sameOAuthSigningUrl
            mEditNoVersionSuffix!!.isChecked = noVersionSuffix
            mEditConsumerKey!!.setText(consumerKey)
            mEditConsumerSecret!!.setText(consumerSecret)

            mButtonOAuth!!.isChecked = authType == ParcelableCredentials.AuthType.OAUTH
            mButtonxAuth!!.isChecked = authType == ParcelableCredentials.AuthType.XAUTH
            mButtonBasic!!.isChecked = authType == ParcelableCredentials.AuthType.BASIC
            mButtonTwipOMode!!.isChecked = authType == ParcelableCredentials.AuthType.TWIP_O_MODE
            if (mEditAuthType!!.checkedRadioButtonId == -1) {
                mButtonOAuth!!.isChecked = true
            }
        }

        companion object {

            fun newInstance(key: String): DefaultAPIPreferenceDialogFragment {
                val df = DefaultAPIPreferenceDialogFragment()
                val args = Bundle()
                args.putString(PreferenceDialogFragmentCompat.ARG_KEY, key)
                df.arguments = args
                return df
            }
        }
    }

}
