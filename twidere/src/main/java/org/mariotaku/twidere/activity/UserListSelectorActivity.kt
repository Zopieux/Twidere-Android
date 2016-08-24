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

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AutoCompleteTextView
import android.widget.ListView

import com.squareup.otto.Subscribe

import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.http.HttpResponseCode
import org.mariotaku.microblog.library.twitter.model.Paging
import org.mariotaku.microblog.library.twitter.model.ResponseList
import org.mariotaku.microblog.library.twitter.model.User
import org.mariotaku.microblog.library.twitter.model.UserList
import org.mariotaku.twidere.R
import org.mariotaku.twidere.adapter.SimpleParcelableUserListsAdapter
import org.mariotaku.twidere.adapter.SimpleParcelableUsersAdapter
import org.mariotaku.twidere.adapter.UserAutoCompleteAdapter
import org.mariotaku.twidere.fragment.CreateUserListDialogFragment
import org.mariotaku.twidere.fragment.ProgressDialogFragment
import org.mariotaku.twidere.model.ParcelableUser
import org.mariotaku.twidere.model.ParcelableUserList
import org.mariotaku.twidere.model.SingleResponse
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.model.message.UserListCreatedEvent
import org.mariotaku.twidere.model.util.ParcelableUserListUtils
import org.mariotaku.twidere.model.util.ParcelableUserUtils
import org.mariotaku.twidere.util.AsyncTaskUtils
import org.mariotaku.twidere.util.MicroBlogAPIFactory
import org.mariotaku.twidere.util.ParseUtils

import java.util.ArrayList

import android.text.TextUtils.isEmpty
import org.mariotaku.twidere.TwidereConstants.LOGTAG
import org.mariotaku.twidere.constant.IntentConstants.EXTRA_ACCOUNT_KEY
import org.mariotaku.twidere.constant.IntentConstants.EXTRA_IS_MY_ACCOUNT
import org.mariotaku.twidere.constant.IntentConstants.EXTRA_SCREEN_NAME
import org.mariotaku.twidere.constant.IntentConstants.EXTRA_USER
import org.mariotaku.twidere.constant.IntentConstants.EXTRA_USER_LIST
import org.mariotaku.twidere.constant.IntentConstants.INTENT_ACTION_SELECT_USER
import org.mariotaku.twidere.util.DataStoreUtils.getAccountScreenName

class UserListSelectorActivity : BaseActivity(), OnClickListener, OnItemClickListener {

    private var mEditScreenName: AutoCompleteTextView? = null
    private var mUserListsListView: ListView? = null
    private var mUsersListView: ListView? = null
    private var mUserListsAdapter: SimpleParcelableUserListsAdapter? = null
    private var mUsersAdapter: SimpleParcelableUsersAdapter? = null
    private var mUsersListContainer: View? = null
    private var mUserListsContainer: View? = null
    private var mCreateUserListContainer: View? = null

    private var mScreenName: String? = null

    private var mResumeFragmentRunnable: Runnable? = null
    private var mFragmentsResumed: Boolean = false
    private var mScreenNameConfirm: View? = null
    private var mCreateList: View? = null

    override fun onClick(v: View) {
        when (v.id) {
            R.id.screen_name_confirm -> {
                val screen_name = ParseUtils.parseString(mEditScreenName!!.text)
                if (isEmpty(screen_name)) return
                searchUser(screen_name)
            }
            R.id.create_list -> {
                val f = CreateUserListDialogFragment()
                val args = Bundle()
                args.putParcelable(EXTRA_ACCOUNT_KEY, accountKey)
                f.arguments = args
                f.show(supportFragmentManager, null)
            }
        }
    }

    override fun onContentChanged() {
        super.onContentChanged()
        mUsersListContainer = findViewById(R.id.users_list_container)
        mUserListsContainer = findViewById(R.id.user_lists_container)
        mEditScreenName = findViewById(R.id.edit_screen_name) as AutoCompleteTextView
        mUserListsListView = findViewById(R.id.user_lists_list) as ListView
        mUsersListView = findViewById(R.id.users_list) as ListView
        mCreateUserListContainer = findViewById(R.id.create_list_container)
        mScreenNameConfirm = findViewById(R.id.screen_name_confirm)
        mCreateList = findViewById(R.id.create_list)
    }

    override fun onItemClick(view: AdapterView<*>, child: View, position: Int, id: Long) {
        val view_id = view.id
        val list = view as ListView
        if (view_id == R.id.users_list) {
            val user = mUsersAdapter!!.getItem(position - list.headerViewsCount) ?: return
            if (isSelectingUser) {
                val data = Intent()
                data.setExtrasClassLoader(classLoader)
                data.putExtra(EXTRA_USER, user)
                setResult(Activity.RESULT_OK, data)
                finish()
            } else {
                getUserLists(user.screen_name)
            }
        } else if (view_id == R.id.user_lists_list) {
            val data = Intent()
            data.putExtra(EXTRA_USER_LIST, mUserListsAdapter!!.getItem(position - list.headerViewsCount))
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    fun setUsersData(data: List<ParcelableUser>) {
        mUsersAdapter!!.setData(data, true)
        mUsersListContainer!!.visibility = View.VISIBLE
        mUserListsContainer!!.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        if (!intent.hasExtra(EXTRA_ACCOUNT_KEY)) {
            finish()
            return
        }
        setContentView(R.layout.activity_user_list_selector)
        if (savedInstanceState == null) {
            mScreenName = intent.getStringExtra(EXTRA_SCREEN_NAME)
        } else {
            mScreenName = savedInstanceState.getString(EXTRA_SCREEN_NAME)
        }

        val selecting_user = isSelectingUser
        setTitle(if (selecting_user) R.string.select_user else R.string.select_user_list)
        if (!isEmpty(mScreenName)) {
            if (selecting_user) {
                searchUser(mScreenName)
            } else {
                getUserLists(mScreenName)
            }
        }
        val adapter = UserAutoCompleteAdapter(this)
        adapter.setAccountKey(accountKey)
        mEditScreenName!!.setAdapter(adapter)
        mEditScreenName!!.setText(mScreenName)
        mUserListsListView!!.setAdapter(mUserListsAdapter = SimpleParcelableUserListsAdapter(this))
        mUsersListView!!.setAdapter(mUsersAdapter = SimpleParcelableUsersAdapter(this))
        mUserListsListView!!.onItemClickListener = this
        mUsersListView!!.onItemClickListener = this
        mScreenNameConfirm!!.setOnClickListener(this)
        mCreateList!!.setOnClickListener(this)
        if (selecting_user) {
            mUsersListContainer!!.visibility = View.VISIBLE
            mUserListsContainer!!.visibility = View.GONE
        } else {
            mUsersListContainer!!.visibility = if (isEmpty(mScreenName)) View.VISIBLE else View.GONE
            mUserListsContainer!!.visibility = if (isEmpty(mScreenName)) View.GONE else View.VISIBLE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_SCREEN_NAME, mScreenName)
    }

    override fun onStart() {
        super.onStart()
        bus.register(this)
    }

    override fun onStop() {
        bus.unregister(this)
        super.onStop()
    }

    @Subscribe
    fun onUserListCreated(event: UserListCreatedEvent) {
        getUserLists(mScreenName)
    }

    private val accountKey: UserKey
        get() = intent.getParcelableExtra<UserKey>(EXTRA_ACCOUNT_KEY)

    private fun getUserLists(screenName: String?) {
        if (screenName == null) return
        mScreenName = screenName
        val task = GetUserListsTask(this, accountKey, screenName)
        AsyncTaskUtils.executeTask(task)
    }

    private val isSelectingUser: Boolean
        get() = INTENT_ACTION_SELECT_USER == intent.action

    private fun searchUser(name: String) {
        val task = SearchUsersTask(this, accountKey, name)
        AsyncTaskUtils.executeTask(task)
    }

    private fun setUserListsData(data: List<ParcelableUserList>, isMyAccount: Boolean) {
        mUserListsAdapter!!.setData(data, true)
        mUsersListContainer!!.visibility = View.GONE
        mUserListsContainer!!.visibility = View.VISIBLE
        mCreateUserListContainer!!.visibility = if (isMyAccount) View.VISIBLE else View.GONE
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (!mFragmentsResumed && mResumeFragmentRunnable != null) {
            mResumeFragmentRunnable!!.run()
        }
        mFragmentsResumed = true
    }

    override fun onPause() {
        mFragmentsResumed = false
        super.onPause()
    }

    private fun dismissDialogFragment(tag: String) {
        mResumeFragmentRunnable = Runnable {
            val fm = supportFragmentManager
            val f = fm.findFragmentByTag(tag)
            if (f is DialogFragment) {
                f.dismiss()
            }
            mResumeFragmentRunnable = null
        }
        if (mFragmentsResumed) {
            mResumeFragmentRunnable!!.run()
        }
    }

    private fun showDialogFragment(df: DialogFragment, tag: String) {
        mResumeFragmentRunnable = Runnable {
            df.show(supportFragmentManager, tag)
            mResumeFragmentRunnable = null
        }
        if (mFragmentsResumed) {
            mResumeFragmentRunnable!!.run()
        }
    }

    private class GetUserListsTask internal constructor(private val mActivity: UserListSelectorActivity, private val mAccountKey: UserKey,
                                                        private val mScreenName: String) : AsyncTask<Any, Any, SingleResponse<List<ParcelableUserList>>>() {

        override fun doInBackground(vararg params: Any): SingleResponse<List<ParcelableUserList>> {
            val twitter = MicroBlogAPIFactory.getInstance(mActivity, mAccountKey, false) ?: return SingleResponse.getInstance<List<ParcelableUserList>>()
            try {
                val lists = twitter.getUserLists(mScreenName, true)
                val data = ArrayList<ParcelableUserList>()
                var isMyAccount = mScreenName.equals(getAccountScreenName(mActivity,
                        mAccountKey), ignoreCase = true)
                for (item in lists) {
                    val user = item.user
                    if (user != null && mScreenName.equals(user.screenName, ignoreCase = true)) {
                        if (!isMyAccount && TextUtils.equals(user.id, mAccountKey.id)) {
                            isMyAccount = true
                        }
                        data.add(ParcelableUserListUtils.from(item, mAccountKey))
                    }
                }
                val result = SingleResponse.getInstance<List<ParcelableUserList>>(data)
                result.extras.putBoolean(EXTRA_IS_MY_ACCOUNT, isMyAccount)
                return result
            } catch (e: MicroBlogException) {
                Log.w(LOGTAG, e)
                return SingleResponse.getInstance<List<ParcelableUserList>>(e)
            }

        }

        override fun onPostExecute(result: SingleResponse<List<ParcelableUserList>>) {
            mActivity.dismissDialogFragment(FRAGMENT_TAG_GET_USER_LISTS)
            if (result.data != null) {
                mActivity.setUserListsData(result.data, result.extras.getBoolean(EXTRA_IS_MY_ACCOUNT))
            } else if (result.exception is MicroBlogException) {
                if (result.exception.statusCode == HttpResponseCode.NOT_FOUND) {
                    mActivity.searchUser(mScreenName)
                }
            }
        }

        override fun onPreExecute() {
            val df = ProgressDialogFragment()
            df.isCancelable = false
            mActivity.showDialogFragment(df, FRAGMENT_TAG_GET_USER_LISTS)
        }

        companion object {

            private val FRAGMENT_TAG_GET_USER_LISTS = "get_user_lists"
        }

    }

    private class SearchUsersTask internal constructor(private val mActivity: UserListSelectorActivity, private val mAccountKey: UserKey,
                                                       private val mName: String) : AsyncTask<Any, Any, SingleResponse<List<ParcelableUser>>>() {

        override fun doInBackground(vararg params: Any): SingleResponse<List<ParcelableUser>> {
            val twitter = MicroBlogAPIFactory.getInstance(mActivity, mAccountKey, false) ?: return SingleResponse.getInstance<List<ParcelableUser>>()
            try {
                val paging = Paging()
                val lists = twitter.searchUsers(mName, paging)
                val data = ArrayList<ParcelableUser>()
                for (item in lists) {
                    data.add(ParcelableUserUtils.fromUser(item, mAccountKey))
                }
                return SingleResponse.getInstance<List<ParcelableUser>>(data)
            } catch (e: MicroBlogException) {
                Log.w(LOGTAG, e)
                return SingleResponse.getInstance<List<ParcelableUser>>(e)
            }

        }

        override fun onPostExecute(result: SingleResponse<List<ParcelableUser>>) {
            mActivity.dismissDialogFragment(FRAGMENT_TAG_SEARCH_USERS)
            if (result.data != null) {
                mActivity.setUsersData(result.data)
            }
        }

        override fun onPreExecute() {
            val df = ProgressDialogFragment()
            df.isCancelable = false
            mActivity.showDialogFragment(df, FRAGMENT_TAG_SEARCH_USERS)
        }

        companion object {

            private val FRAGMENT_TAG_SEARCH_USERS = "search_users"
        }

    }


}
