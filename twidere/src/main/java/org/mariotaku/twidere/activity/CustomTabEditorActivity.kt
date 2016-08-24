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

package org.mariotaku.twidere.activity

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.PorterDuff.Mode
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.FragmentActivity
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.*
import com.rengwuxian.materialedittext.MaterialEditText
import org.mariotaku.twidere.R
import org.mariotaku.twidere.TwidereConstants.*
import org.mariotaku.twidere.adapter.AccountsSpinnerAdapter
import org.mariotaku.twidere.adapter.ArrayAdapter
import org.mariotaku.twidere.annotation.CustomTabType
import org.mariotaku.twidere.constant.IntentConstants.*
import org.mariotaku.twidere.constant.SharedPreferenceConstants.KEY_DISPLAY_PROFILE_IMAGE
import org.mariotaku.twidere.constant.SharedPreferenceConstants.KEY_NAME_FIRST
import org.mariotaku.twidere.fragment.BaseDialogFragment
import org.mariotaku.twidere.model.*
import org.mariotaku.twidere.model.CustomTabConfiguration.ExtraConfiguration
import org.mariotaku.twidere.model.tab.argument.TabArguments
import org.mariotaku.twidere.model.tab.argument.TextQueryArguments
import org.mariotaku.twidere.model.tab.argument.UserArguments
import org.mariotaku.twidere.model.tab.argument.UserListArguments
import org.mariotaku.twidere.util.*
import org.mariotaku.twidere.util.CustomTabUtils.findTabIconKey
import org.mariotaku.twidere.util.CustomTabUtils.getTabConfiguration
import org.mariotaku.twidere.util.CustomTabUtils.getTabTypeName
import java.text.Collator
import java.util.*

class CustomTabEditorActivity : BaseActivity(), OnClickListener {

    private var mPreferences: SharedPreferences? = null

    private var mAccountsAdapter: AccountsSpinnerAdapter? = null
    private var mTabIconsAdapter: CustomTabIconsAdapter? = null

    private var mAccountContainer: View? = null
    private var mSecondaryFieldContainer: View? = null
    private var mExtraConfigurationsContainer: View? = null
    private var mTabIconSpinner: Spinner? = null
    private var mAccountSpinner: Spinner? = null
    private var mEditTabName: MaterialEditText? = null
    private var mSecondaryFieldLabel: TextView? = null
    private var mExtraConfigurationsContent: LinearLayout? = null

    private var mTabId: Long = 0
    private var mTabConfiguration: CustomTabConfiguration? = null
    private var mSecondaryFieldValue: Any? = null
    private val mExtrasBundle = Bundle()

    private val mOnExtraConfigurationClickListener = OnClickListener { v ->
        val tag = v.tag
        if (tag is ExtraConfiguration) {
            when (tag.type) {
                CustomTabConfiguration.ExtraConfiguration.Type.BOOLEAN -> {
                    val checkBox = v.findViewById(android.R.id.checkbox) as CheckBox
                    checkBox.toggle()
                    mExtrasBundle.putBoolean(tag.key, checkBox.isChecked)
                }
                else -> {
                }
            }
        }
    }

    override fun onClick(v: View) {
        val conf = mTabConfiguration
        val value = mSecondaryFieldValue
        val accountKey = accountKey
        when (v.id) {
            R.id.secondary_field -> {
                if (conf == null) return
                when (conf.secondaryFieldType) {
                    CustomTabConfiguration.FIELD_TYPE_USER -> {
                        val intent = Intent(this, UserListSelectorActivity::class.java)
                        intent.action = INTENT_ACTION_SELECT_USER
                        intent.putExtra(EXTRA_ACCOUNT_KEY, accountKey)
                        startActivityForResult(intent, REQUEST_SELECT_USER)
                    }
                    CustomTabConfiguration.FIELD_TYPE_USER_LIST -> {
                        val intent = Intent(this, UserListSelectorActivity::class.java)
                        intent.action = INTENT_ACTION_SELECT_USER_LIST
                        intent.putExtra(EXTRA_ACCOUNT_KEY, accountKey)
                        startActivityForResult(intent, REQUEST_SELECT_USER_LIST)
                    }
                    CustomTabConfiguration.FIELD_TYPE_TEXT -> {
                        val title = conf.secondaryFieldTitle
                        SecondaryFieldEditTextDialogFragment.show(this, ParseUtils.parseString(value),
                                getString(if (title > 0) title else R.string.content))
                    }
                }
            }
            R.id.save -> {
                if (isEditMode) {
                    if (mTabId < 0) return
                    val data = Intent()
                    data.putExtra(EXTRA_NAME, tabName)
                    data.putExtra(EXTRA_ICON, tabIconKey)
                    data.putExtra(EXTRA_ID, mTabId)
                    data.putExtra(EXTRA_EXTRAS, InternalParseUtils.bundleToJSON(mExtrasBundle))
                    setResult(Activity.RESULT_OK, data)
                    finish()
                } else {
                    if (conf == null) return
                    val accountIdRequired = conf.accountRequirement == CustomTabConfiguration.ACCOUNT_REQUIRED
                    val noAccountId = conf.accountRequirement == CustomTabConfiguration.ACCOUNT_NONE
                    val secondaryFieldRequired = conf.secondaryFieldType != CustomTabConfiguration.FIELD_TYPE_NONE
                    val accountIdInvalid = accountKey == null
                    val secondaryFieldInvalid = mSecondaryFieldValue == null
                    if (accountIdRequired && accountIdInvalid) {
                        Toast.makeText(this, R.string.no_account_selected, Toast.LENGTH_SHORT).show()
                        return
                    } else if (secondaryFieldRequired && secondaryFieldInvalid) {
                        Toast.makeText(this, getString(R.string.name_not_set, mSecondaryFieldLabel!!.text), Toast.LENGTH_SHORT).show()
                        return
                    }
                    val data = Intent()
                    val args = CustomTabUtils.newTabArguments(tabType!!)
                    if (args != null) {
                        if (!noAccountId) {
                            if (accountKey == null) {
                                args.accountKeys = null
                            } else {
                                args.accountKeys = arrayOf(accountKey)
                            }
                        }
                        if (secondaryFieldRequired) {
                            addSecondaryFieldValueToArguments(args)
                        }
                    }
                    data.putExtra(EXTRA_TYPE, tabType)
                    data.putExtra(EXTRA_NAME, tabName)
                    data.putExtra(EXTRA_ICON, tabIconKey)
                    data.putExtra(EXTRA_ARGUMENTS, JsonSerializer.serialize(args))
                    data.putExtra(EXTRA_EXTRAS, InternalParseUtils.bundleToJSON(mExtrasBundle))
                    setResult(Activity.RESULT_OK, data)
                    finish()
                }
            }
        }
    }

    protected val tabName: String
        get() = ParseUtils.parseString(mEditTabName!!.text)

    override fun onContentChanged() {
        super.onContentChanged()
        mAccountContainer = findViewById(R.id.account_container)
        mSecondaryFieldContainer = findViewById(R.id.secondary_field_container)
        mExtraConfigurationsContainer = findViewById(R.id.extra_configurations_container)
        mEditTabName = findViewById(R.id.tab_name) as MaterialEditText
        mSecondaryFieldLabel = findViewById(R.id.secondary_field_label) as TextView
        mTabIconSpinner = findViewById(R.id.tab_icon_spinner) as Spinner
        mAccountSpinner = findViewById(R.id.account_spinner) as Spinner
        mExtraConfigurationsContent = findViewById(R.id.extra_configurations_content) as LinearLayout
    }

    fun setExtraFieldSelectText(view: View, text: Int) {
        val text1 = view.findViewById(android.R.id.text1) as TextView
        val text2 = view.findViewById(android.R.id.text2) as TextView
        val icon = view.findViewById(android.R.id.icon) as ImageView
        text1.visibility = View.VISIBLE
        text2.visibility = View.GONE
        icon.visibility = View.GONE
        text1.setText(text)
    }

    fun setExtraFieldView(view: View, value: Any) {
        val text1 = view.findViewById(android.R.id.text1) as TextView
        val text2 = view.findViewById(android.R.id.text2) as TextView
        val icon = view.findViewById(android.R.id.icon) as ImageView
        val displayProfileImage = mPreferences!!.getBoolean(KEY_DISPLAY_PROFILE_IMAGE, true)
        val displayName = mPreferences!!.getBoolean(KEY_NAME_FIRST, true)
        text1.visibility = View.VISIBLE
        text2.visibility = View.VISIBLE
        icon.visibility = if (displayProfileImage) View.VISIBLE else View.GONE
        if (value is ParcelableUser) {
            text1.text = userColorNameManager.getUserNickname(value.key, value.name)
            text2.text = String.format("@%s", value.screen_name)
            if (displayProfileImage) {
                mediaLoader.displayProfileImage(icon, value)
            }
        } else if (value is ParcelableUserList) {
            val createdBy = userColorNameManager.getDisplayName(value, displayName)
            text1.text = value.name
            text2.text = getString(R.string.created_by, createdBy)
            if (displayProfileImage) {
                mediaLoader.displayProfileImage(icon, value.user_profile_image_url)
            }
        } else if (value is CharSequence) {
            text2.visibility = View.GONE
            icon.visibility = View.GONE
            text1.text = value
        }
    }

    fun setSecondaryFieldValue(value: Any) {
        mSecondaryFieldValue = value
        setExtraFieldView(mSecondaryFieldContainer, value)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode != Activity.RESULT_OK) return
        when (requestCode) {
            REQUEST_SELECT_USER -> {
                setSecondaryFieldValue(data.getParcelableExtra<Parcelable>(EXTRA_USER))
            }
            REQUEST_SELECT_USER_LIST -> {
                setSecondaryFieldValue(data.getParcelableExtra<Parcelable>(EXTRA_USER_LIST))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val intent = intent
        val type = tabType
        val conf = getTabConfiguration(type)
        if (type == null || conf == null) {
            finish()
            return
        }
        mTabId = intent.getLongExtra(EXTRA_ID, -1)
        setTitle(if (isEditMode) R.string.edit_tab else R.string.add_tab)
        setContentView(R.layout.activity_custom_tab_editor)
        mEditTabName!!.setFloatingLabelText(getTabTypeName(this, type))
        mTabIconsAdapter = CustomTabIconsAdapter(this)
        mTabIconsAdapter!!.setData(getIconMap())
        mAccountsAdapter = AccountsSpinnerAdapter(this)
        mAccountSpinner!!.adapter = mAccountsAdapter
        mTabIconSpinner!!.adapter = mTabIconsAdapter
        val iconKey: String
        if (savedInstanceState != null) {
            mExtrasBundle.putAll(savedInstanceState.getBundle(EXTRA_EXTRAS))
        }
        if (!isEditMode) {
            mTabConfiguration = conf
            val hasSecondaryField = conf.secondaryFieldType != CustomTabConfiguration.FIELD_TYPE_NONE
            val accountIdNone = conf.accountRequirement == CustomTabConfiguration.ACCOUNT_NONE
            mAccountContainer!!.visibility = if (accountIdNone) View.GONE else View.VISIBLE
            mSecondaryFieldContainer!!.visibility = if (hasSecondaryField) View.VISIBLE else View.GONE
            val accountIdRequired = conf.accountRequirement == CustomTabConfiguration.ACCOUNT_REQUIRED
            if (!accountIdRequired) {
                mAccountsAdapter!!.add(ParcelableAccount.dummyCredentials())
            }
            val officialKeyOnly = intent.getBooleanExtra(EXTRA_OFFICIAL_KEY_ONLY, false)
            mAccountsAdapter!!.addAll(DataStoreUtils.getCredentialsList(this, false, officialKeyOnly))
            mAccountsAdapter!!.setDummyItemText(R.string.activated_accounts)
            when (conf.secondaryFieldType) {
                CustomTabConfiguration.FIELD_TYPE_USER -> {
                    mSecondaryFieldLabel!!.setText(R.string.user)
                    setExtraFieldSelectText(mSecondaryFieldContainer, R.string.select_user)
                }
                CustomTabConfiguration.FIELD_TYPE_USER_LIST -> {
                    mSecondaryFieldLabel!!.setText(R.string.user_list)
                    setExtraFieldSelectText(mSecondaryFieldContainer, R.string.select_user_list)
                }
                CustomTabConfiguration.FIELD_TYPE_TEXT -> {
                    mSecondaryFieldLabel!!.setText(R.string.content)
                    setExtraFieldSelectText(mSecondaryFieldContainer, R.string.input_text)
                }
            }
            if (conf.secondaryFieldTitle != 0) {
                mSecondaryFieldLabel!!.setText(conf.secondaryFieldTitle)
            }
            iconKey = findTabIconKey(conf.defaultIcon)
            mEditTabName!!.setText(mTabConfiguration!!.defaultTitle)
        } else {
            if (mTabId < 0) {
                finish()
                return
            }
            mAccountContainer!!.visibility = View.GONE
            mSecondaryFieldContainer!!.visibility = View.GONE
            iconKey = intent.getStringExtra(EXTRA_ICON)
            mEditTabName!!.setText(intent.getStringExtra(EXTRA_NAME))
            if (savedInstanceState == null && intent.hasExtra(EXTRA_EXTRAS)) {
                val extras = CustomTabUtils.parseTabExtras(type, intent.getStringExtra(EXTRA_EXTRAS))
                extras?.copyToBundle(mExtrasBundle)
            }
        }
        val selection = mTabIconsAdapter!!.getIconPosition(iconKey)
        mTabIconSpinner!!.setSelection(if (selection > 0) selection else 0)
        val inflater = layoutInflater
        val extraConfigurations = conf.extraConfigurations
        if (extraConfigurations == null || extraConfigurations.size == 0) {
            mExtraConfigurationsContainer!!.visibility = View.GONE
        } else {
            mExtraConfigurationsContainer!!.visibility = View.VISIBLE
            for (config in extraConfigurations) {
                val hasCheckBox = config.type == ExtraConfiguration.Type.BOOLEAN
                val view = inflater.inflate(R.layout.list_item_extra_config, mExtraConfigurationsContent, false)
                val title = view.findViewById(android.R.id.title) as TextView
                val checkBox = view.findViewById(android.R.id.checkbox) as CheckBox
                title.setText(config.titleRes)
                checkBox.visibility = if (hasCheckBox) View.VISIBLE else View.GONE
                if (hasCheckBox) {
                    checkBox.isChecked = mExtrasBundle.getBoolean(config.key, config.defaultBoolean())
                }
                view.tag = config
                view.setOnClickListener(mOnExtraConfigurationClickListener)
                mExtraConfigurationsContent!!.addView(view)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(EXTRA_EXTRAS, mExtrasBundle)
    }

    //noinspection ResourceType
    val tabType: String?
        @CustomTabType
        get() = intent.getStringExtra(EXTRA_TYPE)

    private fun addFieldValueToArguments(value: Any?, args: TabArguments?) {
        val conf = mTabConfiguration
        if (value == null || args == null || conf == null) return
        if (value is ParcelableUser && args is UserArguments) {
            args.setUserKey(value.key)
        } else if (value is ParcelableUserList && args is UserListArguments) {
            args.listId = value.id
        } else if (value is CharSequence) {
            if (args is TextQueryArguments) {
                args.query = value.toString()
            }
        }
    }

    private fun addSecondaryFieldValueToArguments(args: TabArguments) {
        val value = mSecondaryFieldValue
        addFieldValueToArguments(value, args)
    }

    private val accountKey: UserKey?
        get() {
            val pos = mAccountSpinner!!.selectedItemPosition
            if (mAccountSpinner!!.count > pos && pos >= 0) {
                val credentials = mAccountsAdapter!!.getItem(pos)
                return credentials.account_key
            }
            return null
        }

    private val tabIconKey: String?
        get() {
            val pos = mTabIconSpinner!!.selectedItemPosition
            if (mTabIconsAdapter!!.count > pos && pos >= 0)
                return mTabIconsAdapter!!.getItem(pos).key
            return null
        }

    private val isEditMode: Boolean
        get() = INTENT_ACTION_EDIT_TAB == intent.action

    class SecondaryFieldEditTextDialogFragment : BaseDialogFragment(), DialogInterface.OnClickListener {
        private var mEditText: EditText? = null

        override fun onClick(dialog: DialogInterface, which: Int) {
            val activity = activity
            if (activity is CustomTabEditorActivity) {
                activity.setSecondaryFieldValue(ParseUtils.parseString(mEditText!!.text))
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val args = arguments
            val context = activity
            val builder = AlertDialog.Builder(context)
            builder.setTitle(args.getString(EXTRA_TITLE))
            builder.setPositiveButton(android.R.string.ok, this)
            builder.setNegativeButton(android.R.string.cancel, null)
            val view = FrameLayout(activity)
            mEditText = EditText(activity)
            val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT)
            lp.leftMargin = lp.topMargin = lp.bottomMargin = lp.rightMargin = resources.getDimensionPixelSize(
                    R.dimen.element_spacing_normal)
            view.addView(mEditText, lp)
            builder.setView(view)
            mEditText!!.setText(args.getString(EXTRA_TEXT))
            return builder.create()
        }

        companion object {
            private val FRAGMENT_TAG_EDIT_SECONDARY_FIELD = "edit_secondary_field"

            fun show(activity: FragmentActivity, text: String,
                     title: String): SecondaryFieldEditTextDialogFragment {
                val f = SecondaryFieldEditTextDialogFragment()
                val args = Bundle()
                args.putString(EXTRA_TEXT, text)
                args.putString(EXTRA_TITLE, title)
                f.arguments = args
                f.show(activity.supportFragmentManager, FRAGMENT_TAG_EDIT_SECONDARY_FIELD)
                return f
            }
        }
    }

    internal class CustomTabIconsAdapter(context: Context) : ArrayAdapter<Entry<String, Int>>(context, R.layout.spinner_item_custom_tab_icon) {

        private val mResources: Resources
        private val mIconColor: Int

        init {
            setDropDownViewResource(R.layout.list_item_two_line_small)
            mResources = context.resources
            mIconColor = ThemeUtils.getThemeForegroundColor(context)
        }

        override fun getDropDownView(position: Int, convertView: View, parent: ViewGroup): View {
            val view = super.getDropDownView(position, convertView, parent)
            view.findViewById(android.R.id.text2).visibility = View.GONE
            val text1 = view.findViewById(android.R.id.text1) as TextView
            val item = getItem(position)
            val value = item.value
            if (value > 0) {
                val key = item.key
                val name = key.substring(0, 1).toUpperCase(Locale.US) + key.substring(1, key.length)
                text1.setText(name)
            } else {
                text1.setText(R.string.customize)
            }
            bindIconView(position, item, view)
            return view
        }

        fun getIconPosition(key: String?): Int {
            if (key == null) return -1
            var i = 0
            val j = count
            while (i < j) {
                if (key == getItem(i).key) return i
                i++
            }
            return -1
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            bindIconView(position, getItem(position), view)
            return view
        }

        fun setData(map: Map<String, Int>?) {
            clear()
            if (map == null) return
            addAll(map.entries)
            sort(LocationComparator(mResources))
        }

        private fun bindIconView(position: Int, item: Entry<String, Int>, view: View) {
            val icon = view.findViewById(android.R.id.icon) as ImageView
            icon.setColorFilter(mIconColor, Mode.SRC_ATOP)
            val value = item.value
            if (value > 0) {
                icon.setImageResource(item.value)
            } else {
                icon.setImageDrawable(null)
            }
        }

        private class LocationComparator internal constructor(res: Resources) : Comparator<Entry<String, Int>> {
            private val mCollator: Collator

            init {
                mCollator = Collator.getInstance(res.configuration.locale)
            }

            override fun compare(object1: Entry<String, Int>, object2: Entry<String, Int>): Int {
                if (object1.value <= 0) return Integer.MAX_VALUE
                if (object2.value <= 0) return Integer.MIN_VALUE
                return mCollator.compare(object1.key, object2.key)
            }

        }

    }

}
