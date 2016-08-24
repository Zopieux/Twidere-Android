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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast

import org.attoparser.ParseException
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.TwitterOAuth
import org.mariotaku.restfu.http.Authorization
import org.mariotaku.restfu.http.Endpoint
import org.mariotaku.restfu.oauth.OAuthAuthorization
import org.mariotaku.restfu.oauth.OAuthToken
import org.mariotaku.twidere.BuildConfig
import org.mariotaku.twidere.R
import org.mariotaku.twidere.model.SingleResponse
import org.mariotaku.twidere.provider.TwidereDataStore.Accounts
import org.mariotaku.twidere.util.AsyncTaskUtils
import org.mariotaku.twidere.util.MicroBlogAPIFactory
import org.mariotaku.twidere.util.OAuthPasswordAuthenticator
import org.mariotaku.twidere.util.webkit.DefaultWebViewClient

import java.io.IOException
import java.io.StringReader

import android.text.TextUtils.isEmpty
import org.mariotaku.twidere.TwidereConstants.LOGTAG
import org.mariotaku.twidere.TwidereConstants.OAUTH_CALLBACK_OOB
import org.mariotaku.twidere.TwidereConstants.OAUTH_CALLBACK_URL
import org.mariotaku.twidere.constant.IntentConstants.EXTRA_OAUTH_VERIFIER
import org.mariotaku.twidere.constant.IntentConstants.EXTRA_REQUEST_TOKEN
import org.mariotaku.twidere.constant.IntentConstants.EXTRA_REQUEST_TOKEN_SECRET

@SuppressLint("SetJavaScriptEnabled")
class BrowserSignInActivity : BaseActivity() {

    private var mWebView: WebView? = null
    private var mProgressContainer: View? = null

    private var mWebSettings: WebSettings? = null

    private var mRequestToken: OAuthToken? = null

    private var mTask: GetRequestTokenTask? = null

    override fun onContentChanged() {
        super.onContentChanged()
        mWebView = findViewById(R.id.webview) as WebView
        mProgressContainer = findViewById(R.id.progress_container)
    }

    public override fun onDestroy() {
        loaderManager.destroyLoader(0)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser_sign_in)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //noinspection deprecation
            CookieManager.getInstance().removeAllCookie()
        } else {
            CookieManager.getInstance().removeAllCookies(null)
        }
        mWebView!!.setWebViewClient(AuthorizationWebViewClient(this))
        mWebView!!.isVerticalScrollBarEnabled = false
        mWebView!!.addJavascriptInterface(InjectorJavaScriptInterface(this), "injector")
        mWebSettings = mWebView!!.settings
        mWebSettings!!.loadsImagesAutomatically = true
        mWebSettings!!.javaScriptEnabled = true
        mWebSettings!!.blockNetworkImage = false
        mWebSettings!!.saveFormData = true
        getRequestToken()
    }

    private fun getRequestToken() {
        if (mRequestToken != null || mTask != null && mTask!!.status == AsyncTask.Status.RUNNING)
            return
        mTask = GetRequestTokenTask(this)
        AsyncTaskUtils.executeTask<GetRequestTokenTask, Any>(mTask)
    }

    private fun loadUrl(url: String) {
        if (mWebView == null) return
        mWebView!!.loadUrl(url)
    }

    private fun readOAuthPin(html: String): String? {
        try {
            val data = OAuthPasswordAuthenticator.OAuthPinData()
            OAuthPasswordAuthenticator.readOAuthPINFromHtml(StringReader(html), data)
            return data.oauthPin
        } catch (e: ParseException) {
            Log.w(LOGTAG, e)
        } catch (e: IOException) {
            Log.w(LOGTAG, e)
        }

        return null
    }

    private fun setLoadProgressShown(shown: Boolean) {
        mProgressContainer!!.visibility = if (shown) View.VISIBLE else View.GONE
    }

    private fun setRequestToken(token: OAuthToken) {
        mRequestToken = token
    }

    internal class AuthorizationWebViewClient(activity: BrowserSignInActivity) : DefaultWebViewClient(activity) {

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            view.loadUrl(INJECT_CONTENT)
            val activity = activity as BrowserSignInActivity
            activity.setLoadProgressShown(false)
            val uri = Uri.parse(url)
            // Hack for fanfou
            if ("fanfou.com" == uri.host) {
                val path = uri.path
                val paramNames = uri.queryParameterNames
                if ("/oauth/authorize" == path && paramNames.contains("oauth_callback")) {
                    // Sign in successful response.
                    val requestToken = activity.mRequestToken
                    if (requestToken != null) {
                        val intent = Intent()
                        intent.putExtra(EXTRA_REQUEST_TOKEN, requestToken.oauthToken)
                        intent.putExtra(EXTRA_REQUEST_TOKEN_SECRET, requestToken.oauthTokenSecret)
                        activity.setResult(Activity.RESULT_OK, intent)
                        activity.finish()
                    }
                }
            }
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
            super.onPageStarted(view, url, favicon)
            (activity as BrowserSignInActivity).setLoadProgressShown(true)
        }

        @SuppressWarnings("deprecation")
        override fun onReceivedError(view: WebView, errorCode: Int, description: String,
                                     failingUrl: String) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            val activity = activity
            Toast.makeText(activity, description, Toast.LENGTH_SHORT).show()
            activity.finish()
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            val uri = Uri.parse(url)
            if (url.startsWith(OAUTH_CALLBACK_URL)) {
                val oauthVerifier = uri.getQueryParameter(EXTRA_OAUTH_VERIFIER)
                val activity = activity as BrowserSignInActivity
                val requestToken = activity.mRequestToken
                if (oauthVerifier != null && requestToken != null) {
                    val intent = Intent()
                    intent.putExtra(EXTRA_OAUTH_VERIFIER, oauthVerifier)
                    intent.putExtra(EXTRA_REQUEST_TOKEN, requestToken.oauthToken)
                    intent.putExtra(EXTRA_REQUEST_TOKEN_SECRET, requestToken.oauthTokenSecret)
                    activity.setResult(Activity.RESULT_OK, intent)
                    activity.finish()
                }
                return true
            }
            return false
        }

    }

    internal class GetRequestTokenTask(private val mActivity: BrowserSignInActivity) : AsyncTask<Any, Any, SingleResponse<OAuthToken>>() {

        private val mConsumerKey: String
        private val mConsumerSecret: String
        private val mAPIUrlFormat: String
        private val mSameOAuthSigningUrl: Boolean

        init {
            val intent = mActivity.intent
            mConsumerKey = intent.getStringExtra(Accounts.CONSUMER_KEY)
            mConsumerSecret = intent.getStringExtra(Accounts.CONSUMER_SECRET)
            mAPIUrlFormat = intent.getStringExtra(Accounts.API_URL_FORMAT)
            mSameOAuthSigningUrl = intent.getBooleanExtra(Accounts.SAME_OAUTH_SIGNING_URL, true)
        }

        override fun doInBackground(vararg params: Any): SingleResponse<OAuthToken> {
            if (isEmpty(mConsumerKey) || isEmpty(mConsumerSecret)) {
                return SingleResponse()
            }
            try {
                val endpoint = MicroBlogAPIFactory.getOAuthSignInEndpoint(mAPIUrlFormat,
                        mSameOAuthSigningUrl)
                val auth = OAuthAuthorization(mConsumerKey, mConsumerSecret)
                val oauth = MicroBlogAPIFactory.getInstance(mActivity, endpoint,
                        auth, TwitterOAuth::class.java)
                return SingleResponse(oauth.getRequestToken(OAUTH_CALLBACK_OOB), null, Bundle())
            } catch (e: MicroBlogException) {
                return SingleResponse(null, e, Bundle())
            }

        }

        override fun onPostExecute(result: SingleResponse<OAuthToken>) {
            mActivity.setLoadProgressShown(false)
            if (result.hasData()) {
                val token = result.data
                mActivity.setRequestToken(token)
                val endpoint = MicroBlogAPIFactory.getOAuthSignInEndpoint(mAPIUrlFormat, true)
                mActivity.loadUrl(endpoint.construct("/oauth/authorize", *arrayOf("oauth_token", token!!.oauthToken)))
            } else {
                if (BuildConfig.DEBUG && result.hasException()) {
                    Log.w(LOGTAG, "Exception while browser sign in", result.exception)
                }
                if (!mActivity.isFinishing) {
                    Toast.makeText(mActivity, R.string.error_occurred, Toast.LENGTH_SHORT).show()
                    mActivity.finish()
                }
            }
        }

        override fun onPreExecute() {
            mActivity.setLoadProgressShown(true)
        }

    }

    internal class InjectorJavaScriptInterface(private val mActivity: BrowserSignInActivity) {

        @JavascriptInterface
        fun processHTML(html: String) {
            val oauthVerifier = mActivity.readOAuthPin(html)
            val requestToken = mActivity.mRequestToken
            if (oauthVerifier != null && requestToken != null) {
                val intent = Intent()
                intent.putExtra(EXTRA_OAUTH_VERIFIER, oauthVerifier)
                intent.putExtra(EXTRA_REQUEST_TOKEN, requestToken.oauthToken)
                intent.putExtra(EXTRA_REQUEST_TOKEN_SECRET, requestToken.oauthTokenSecret)
                mActivity.setResult(Activity.RESULT_OK, intent)
                mActivity.finish()
            }
        }
    }

    companion object {

        private val INJECT_CONTENT = "javascript:window.injector.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');"
    }
}
