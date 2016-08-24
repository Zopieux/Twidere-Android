package org.mariotaku.twidere.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.annotation.WorkerThread
import android.text.TextUtils
import android.webkit.URLUtil
import com.fasterxml.jackson.core.JsonParseException
import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.*
import org.mariotaku.microblog.library.twitter.auth.BasicAuthorization
import org.mariotaku.microblog.library.twitter.auth.EmptyAuthorization
import org.mariotaku.microblog.library.twitter.util.TwitterConverterFactory
import org.mariotaku.restfu.*
import org.mariotaku.restfu.http.*
import org.mariotaku.restfu.oauth.OAuthAuthorization
import org.mariotaku.restfu.oauth.OAuthEndpoint
import org.mariotaku.restfu.oauth.OAuthToken
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.twidere.BuildConfig
import org.mariotaku.twidere.TwidereConstants.*
import org.mariotaku.twidere.model.ConsumerKeyType
import org.mariotaku.twidere.model.ParcelableAccount
import org.mariotaku.twidere.model.ParcelableCredentials
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.model.util.ParcelableAccountUtils
import org.mariotaku.twidere.model.util.ParcelableCredentialsUtils
import org.mariotaku.twidere.provider.TwidereDataStore.Accounts
import org.mariotaku.twidere.util.dagger.DependencyHolder
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

/**
 * Created by mariotaku on 15/5/7.
 */
object MicroBlogAPIFactory {

    val CARDS_PLATFORM_ANDROID_12 = "Android-12"


    private val sConstantPoll = SimpleValueMap()

    init {
        sConstantPoll.put("include_cards", "true")
        sConstantPoll.put("cards_platform", CARDS_PLATFORM_ANDROID_12)
        sConstantPoll.put("include_my_retweet", "true")
        sConstantPoll.put("include_rts", "true")
        sConstantPoll.put("include_reply_count", "true")
        sConstantPoll.put("include_descendent_reply_count", "true")
        sConstantPoll.put("full_text", "true")
        sConstantPoll.put("model_version", "7")
        sConstantPoll.put("skip_aggregation", "false")
        sConstantPoll.put("include_ext_alt_text", "true")
        sConstantPoll.put("tweet_mode", "extended")
    }

    @WorkerThread
    fun getDefaultTwitterInstance(context: Context?, includeEntities: Boolean): MicroBlog? {
        if (context == null) return null
        return getDefaultTwitterInstance(context, includeEntities, true)
    }

    @WorkerThread
    fun getDefaultTwitterInstance(context: Context?, includeEntities: Boolean,
                                  includeRetweets: Boolean): MicroBlog? {
        if (context == null) return null
        val accountKey = Utils.getDefaultAccountKey(context) ?: return null
        return getInstance(context, accountKey, includeEntities, includeRetweets)
    }

    @WorkerThread
    fun getInstance(context: Context,
                    accountKey: UserKey,
                    includeEntities: Boolean): MicroBlog? {
        return getInstance(context, accountKey, includeEntities, true)
    }

    @WorkerThread
    fun getInstance(context: Context,
                    accountKey: UserKey,
                    includeEntities: Boolean,
                    includeRetweets: Boolean): MicroBlog? {
        return getInstance(context, accountKey, includeEntities, includeRetweets, MicroBlog::class.java)
    }

    fun getInstance(context: Context,
                    credentials: ParcelableCredentials,
                    includeEntities: Boolean, includeRetweets: Boolean): MicroBlog? {
        return getInstance(context, credentials, includeEntities, includeRetweets, MicroBlog::class.java)
    }


    @WorkerThread
    fun <T> getInstance(context: Context,
                        accountKey: UserKey,
                        includeEntities: Boolean,
                        includeRetweets: Boolean,
                        cls: Class<T>): T? {
        val credentials = ParcelableCredentialsUtils.getCredentials(context, accountKey) ?: return null
        return getInstance(context, credentials, includeEntities, includeRetweets, cls)
    }

    fun <T> getInstance(context: Context,
                        credentials: ParcelableCredentials,
                        includeEntities: Boolean, includeRetweets: Boolean,
                        cls: Class<T>): T? {
        val extraParams = HashMap<String, String>()
        when (ParcelableAccountUtils.getAccountType(credentials)) {
            ParcelableAccount.Type.FANFOU -> {
                extraParams.put("format", "html")
            }
            ParcelableAccount.Type.TWITTER -> {
                extraParams.put("include_entities", includeEntities.toString())
                extraParams.put("include_retweets", includeRetweets.toString())
            }
        }
        return getInstance(context, credentials, extraParams, cls)
    }


    @WorkerThread
    fun <T> getInstance(context: Context, endpoint: Endpoint,
                        auth: Authorization?, extraRequestParams: Map<String, String>?,
                        cls: Class<T>, twitterExtraQueries: Boolean): T {
        val factory = RestAPIFactory<MicroBlogException>()
        val userAgent: String?
        if (auth is OAuthAuthorization) {
            val consumerKey = auth.consumerKey
            val consumerSecret = auth.consumerSecret
            val officialKeyType = TwitterContentUtils.getOfficialKeyType(context, consumerKey, consumerSecret)
            if (officialKeyType != ConsumerKeyType.UNKNOWN) {
                userAgent = getUserAgentName(context, officialKeyType)
            } else {
                userAgent = getTwidereUserAgent(context)
            }
        } else {
            userAgent = getTwidereUserAgent(context)
        }
        val holder = DependencyHolder.get(context)
        factory.setHttpClient(holder.restHttpClient)
        factory.setAuthorization(auth)
        factory.setEndpoint(endpoint)
        if (twitterExtraQueries) {
            factory.setConstantPool(sConstantPoll)
        } else {
            factory.setConstantPool(SimpleValueMap())
        }
        val converterFactory = TwitterConverterFactory()
        factory.setRestConverterFactory(converterFactory)
        factory.setRestRequestFactory(TwidereRestRequestFactory(extraRequestParams))
        factory.setHttpRequestFactory(TwidereHttpRequestFactory(userAgent))
        factory.setExceptionFactory(TwidereExceptionFactory(converterFactory))
        return factory.build(cls)
    }

    @WorkerThread
    fun <T> getInstance(context: Context, endpoint: Endpoint,
                        auth: Authorization, cls: Class<T>): T {
        return getInstance(context, endpoint, auth, null, cls, true)
    }

    @WorkerThread
    fun <T> getInstance(context: Context, endpoint: Endpoint,
                        credentials: ParcelableCredentials,
                        cls: Class<T>): T {
        return getInstance(context, endpoint, credentials, null, cls)
    }

    @WorkerThread
    fun <T> getInstance(context: Context, endpoint: Endpoint,
                        credentials: ParcelableCredentials,
                        extraRequestParams: Map<String, String>?, cls: Class<T>): T {
        return getInstance(context, endpoint, getAuthorization(credentials), extraRequestParams, cls,
                isTwitterCredentials(credentials))
    }

    fun isTwitterCredentials(context: Context, accountId: UserKey): Boolean {
        val account = ParcelableAccountUtils.getAccount(context, accountId) ?: return false
        return isTwitterCredentials(account)
    }

    fun isTwitterCredentials(account: ParcelableAccount): Boolean {
        if (account.account_type == null) {
            val accountHost = account.account_key.host ?: return true
            return USER_TYPE_TWITTER_COM == accountHost
        }
        return ParcelableAccount.Type.TWITTER == account.account_type
    }

    fun isStatusNetCredentials(account: ParcelableAccount): Boolean {
        return ParcelableAccount.Type.STATUSNET == account.account_type
    }

    @WorkerThread
    internal fun <T> getInstance(context: Context, credentials: ParcelableCredentials,
                                 cls: Class<T>): T {
        return getInstance(context, credentials, null, cls)
    }

    @WorkerThread
    internal fun <T> getInstance(context: Context, credentials: ParcelableCredentials,
                                 extraRequestParams: Map<String, String>?, cls: Class<T>): T {
        return MicroBlogAPIFactory.getInstance(context, getEndpoint(credentials, cls), credentials,
                extraRequestParams, cls)
    }

    fun getEndpoint(credentials: ParcelableCredentials, cls: Class<*>): Endpoint {
        val apiUrlFormat: String
        val sameOAuthSigningUrl = credentials.same_oauth_signing_url
        val noVersionSuffix = credentials.no_version_suffix
        if (!TextUtils.isEmpty(credentials.api_url_format)) {
            apiUrlFormat = credentials.api_url_format!!
        } else {
            apiUrlFormat = DEFAULT_TWITTER_API_URL_FORMAT
        }
        val domain: String
        val versionSuffix: String?
        if (MicroBlog::class.java.isAssignableFrom(cls)) {
            domain = "api"
            versionSuffix = if (noVersionSuffix) null else "/1.1/"
        } else if (Twitter::class.java.isAssignableFrom(cls)) {
            domain = "api"
            versionSuffix = if (noVersionSuffix) null else "/1.1/"
        } else if (TwitterUpload::class.java.isAssignableFrom(cls)) {
            domain = "upload"
            versionSuffix = if (noVersionSuffix) null else "/1.1/"
        } else if (TwitterOAuth::class.java.isAssignableFrom(cls)) {
            domain = "api"
            versionSuffix = null
        } else if (TwitterOAuth2::class.java.isAssignableFrom(cls)) {
            domain = "api"
            versionSuffix = null
        } else if (TwitterUserStream::class.java.isAssignableFrom(cls)) {
            domain = "userstream"
            versionSuffix = if (noVersionSuffix) null else "/1.1/"
        } else if (TwitterCaps::class.java.isAssignableFrom(cls)) {
            domain = "caps"
            versionSuffix = null
        } else {
            throw TwitterConverterFactory.UnsupportedTypeException(cls)
        }
        val endpointUrl: String
        endpointUrl = getApiUrl(apiUrlFormat, domain, versionSuffix)
        if (credentials.auth_type == ParcelableCredentials.AuthType.XAUTH || credentials.auth_type == ParcelableCredentials.AuthType.OAUTH) {
            val signEndpointUrl: String
            if (!sameOAuthSigningUrl) {
                signEndpointUrl = getApiUrl(DEFAULT_TWITTER_API_URL_FORMAT, domain, versionSuffix)
            } else {
                signEndpointUrl = endpointUrl
            }
            return OAuthEndpoint(endpointUrl, signEndpointUrl)
        }
        return Endpoint(endpointUrl)
    }

    @SuppressLint("SwitchIntDef")
    fun getAuthorization(credentials: ParcelableCredentials?): Authorization? {
        if (credentials == null) return null
        when (credentials.auth_type) {
            ParcelableCredentials.AuthType.OAUTH, ParcelableCredentials.AuthType.XAUTH -> {
                val consumerKey = if (TextUtils.isEmpty(credentials.consumer_key))
                    TWITTER_CONSUMER_KEY_LEGACY
                else
                    credentials.consumer_key
                val consumerSecret = if (TextUtils.isEmpty(credentials.consumer_secret))
                    TWITTER_CONSUMER_SECRET_LEGACY
                else
                    credentials.consumer_secret
                val accessToken = OAuthToken(credentials.oauth_token,
                        credentials.oauth_token_secret)
                if (isValidConsumerKeySecret(consumerKey) && isValidConsumerKeySecret(consumerSecret))
                    return OAuthAuthorization(consumerKey, consumerSecret, accessToken)
                return OAuthAuthorization(TWITTER_CONSUMER_KEY, TWITTER_CONSUMER_SECRET, accessToken)
            }
            ParcelableCredentials.AuthType.BASIC -> {
                val screenName = credentials.screen_name
                val username = credentials.basic_auth_username
                val loginName = username ?: screenName
                val password = credentials.basic_auth_password
                if (TextUtils.isEmpty(loginName) || TextUtils.isEmpty(password)) return null
                return BasicAuthorization(loginName, password)
            }
        }
        return EmptyAuthorization()
    }

    fun verifyApiFormat(format: String): Boolean {
        val url = getApiBaseUrl(format, "test")
        return URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)
    }

    fun getApiBaseUrl(format: String, domain: String?): String {
        var format = format
        val matcher = Pattern.compile("\\[(\\.?)DOMAIN(\\.?)\\]", Pattern.CASE_INSENSITIVE).matcher(format)
        if (!matcher.find()) {
            // For backward compatibility
            format = substituteLegacyApiBaseUrl(format, domain)
            if (!format.endsWith("/1.1") && !format.endsWith("/1.1/")) {
                return format
            }
            val versionSuffix = "/1.1"
            val suffixLength = versionSuffix.length
            val lastIndex = format.lastIndexOf(versionSuffix)
            return format.substring(0, lastIndex) + format.substring(lastIndex + suffixLength)
        }
        if (TextUtils.isEmpty(domain)) return matcher.replaceAll("")
        return matcher.replaceAll("$1$domain$2")
    }

    internal fun substituteLegacyApiBaseUrl(format: String, domain: String?): String {
        val idxOfSlash = format.indexOf("://")
        // Not an url
        if (idxOfSlash < 0) return format
        val startOfHost = idxOfSlash + 3
        if (startOfHost < 0) return getApiBaseUrl("https://[DOMAIN.]twitter.com/", domain)
        val endOfHost = format.indexOf('/', startOfHost)
        val host = if (endOfHost != -1) format.substring(startOfHost, endOfHost) else format.substring(startOfHost)
        if (!host.equals("api.twitter.com", ignoreCase = true)) return format
        val sb = StringBuilder()
        sb.append(format.substring(0, startOfHost))
        if (domain != null) {
            sb.append(domain)
            sb.append(".twitter.com")
        } else {
            sb.append("twitter.com")
        }
        if (endOfHost != -1) {
            sb.append(format.substring(endOfHost))
        }
        return sb.toString()
    }

    fun getApiUrl(pattern: String, domain: String, appendPath: String?): String {
        var appendPath = appendPath
        var urlBase = getApiBaseUrl(pattern, domain)
        if (urlBase.endsWith("/")) {
            urlBase = urlBase.substring(0, urlBase.length - 1)
        }
        if (appendPath == null) return urlBase + "/"
        if (appendPath.startsWith("/")) {
            appendPath = appendPath.substring(1)
        }
        return urlBase + "/" + appendPath
    }

    @WorkerThread
    fun getUserAgentName(context: Context, type: ConsumerKeyType): String? {
        when (type) {
            ConsumerKeyType.TWITTER_FOR_ANDROID -> {
                val versionName = "5.2.4"
                val internalVersionName = "524-r1"
                val model = Build.MODEL
                val manufacturer = Build.MANUFACTURER
                val sdkInt = Build.VERSION.SDK_INT
                val device = Build.DEVICE
                val brand = Build.BRAND
                val product = Build.PRODUCT
                val debug = if (BuildConfig.DEBUG) 1 else 0
                return String.format(Locale.ROOT, "TwitterAndroid /%s (%s) %s/%d (%s;%s;%s;%s;%d)",
                        versionName, internalVersionName, model, sdkInt, manufacturer, device, brand,
                        product, debug)
            }
            ConsumerKeyType.TWITTER_FOR_IPHONE -> {
                return "Twitter-iPhone"
            }
            ConsumerKeyType.TWITTER_FOR_IPAD -> {
                return "Twitter-iPad"
            }
            ConsumerKeyType.TWITTER_FOR_MAC -> {
                return "Twitter-Mac"
            }
            ConsumerKeyType.TWEETDECK -> {
                return UserAgentUtils.getDefaultUserAgentStringSafe(context)
            }
        }
        return "Twitter"
    }

    fun getTwidereUserAgent(context: Context): String {
        val pm = context.packageManager
        try {
            val pi = pm.getPackageInfo(context.packageName, 0)
            return String.format("%s %s / %s", TWIDERE_APP_NAME, TWIDERE_PROJECT_URL, pi.versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            throw AssertionError(e)
        }

    }

    fun getOAuthRestEndpoint(apiUrlFormat: String, sameOAuthSigningUrl: Boolean, noVersionSuffix: Boolean): Endpoint {
        return getOAuthEndpoint(apiUrlFormat, "api", if (noVersionSuffix) null else "1.1", sameOAuthSigningUrl)
    }

    fun getOAuthSignInEndpoint(apiUrlFormat: String, sameOAuthSigningUrl: Boolean): Endpoint {
        return getOAuthEndpoint(apiUrlFormat, "api", null, sameOAuthSigningUrl, true)
    }

    @JvmOverloads fun getOAuthEndpoint(apiUrlFormat: String, domain: String,
                                       versionSuffix: String?,
                                       sameOAuthSigningUrl: Boolean, fixUrl: Boolean = false): Endpoint {
        var endpointUrl: String
        val signEndpointUrl: String
        endpointUrl = getApiUrl(apiUrlFormat, domain, versionSuffix)
        if (fixUrl) {
            val authorityRange = UriUtils.getAuthorityRange(endpointUrl)
            if (authorityRange != null && endpointUrl.regionMatches(authorityRange[0],
                    "api.fanfou.com", 0, authorityRange[1] - authorityRange[0])) {
                endpointUrl = endpointUrl.substring(0, authorityRange[0]) + "fanfou.com" +
                        endpointUrl.substring(authorityRange[1])
            }
        }
        if (!sameOAuthSigningUrl) {
            signEndpointUrl = getApiUrl(DEFAULT_TWITTER_API_URL_FORMAT, domain, versionSuffix)
        } else {
            signEndpointUrl = endpointUrl
        }
        return OAuthEndpoint(endpointUrl, signEndpointUrl)
    }

    fun getOAuthToken(consumerKey: String, consumerSecret: String): OAuthToken {
        if (isValidConsumerKeySecret(consumerKey) && isValidConsumerKeySecret(consumerSecret))
            return OAuthToken(consumerKey, consumerSecret)
        return OAuthToken(TWITTER_CONSUMER_KEY, TWITTER_CONSUMER_SECRET)
    }

    fun isValidConsumerKeySecret(text: CharSequence): Boolean {
        var i = 0
        val j = text.length
        while (i < j) {
            if (!isAsciiLetterOrDigit(text[i])) return false
            i++
        }
        return true
    }

    private fun isAsciiLetterOrDigit(ch: Char): Boolean {
        return ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9'
    }

    fun getOfficialKeyType(context: Context?, accountKey: UserKey): ConsumerKeyType {
        if (context == null) return ConsumerKeyType.UNKNOWN
        val projection = arrayOf(Accounts.CONSUMER_KEY, Accounts.CONSUMER_SECRET, Accounts.AUTH_TYPE)
        val selection = Expression.equalsArgs(Accounts.ACCOUNT_KEY).sql
        val selectionArgs = arrayOf(accountKey.toString())
        val c = context.contentResolver.query(Accounts.CONTENT_URI, projection,
                selection, selectionArgs, null) ?: return ConsumerKeyType.UNKNOWN
//noinspection TryFinallyCanBeTryWithResources
        try {
            if (c.moveToPosition(0) && ParcelableCredentialsUtils.isOAuth(c.getInt(2))) {
                return TwitterContentUtils.getOfficialKeyType(context, c.getString(0), c.getString(1))
            }
        } finally {
            c.close()
        }
        return ConsumerKeyType.UNKNOWN
    }

    class TwidereHttpRequestFactory(private val userAgent: String?) : HttpRequest.Factory {

        @Throws(IOException::class, RestConverter.ConvertException::class, Exception::class)
        override fun <E : Exception> create(endpoint: Endpoint, info: RestRequest,
                                            authorization: Authorization?,
                                            converterFactory: RestConverter.Factory<E>): HttpRequest {
            val restMethod = info.method
            val url = Endpoint.constructUrl(endpoint.url, info)
            var headers: MultiValueMap<String>? = info.headers
            if (headers == null) {
                headers = MultiValueMap<String>()
            }

            if (authorization != null && authorization.hasAuthorization()) {
                headers.add("Authorization", RestFuUtils.sanitizeHeader(authorization.getHeader(endpoint, info)))
            }
            if (userAgent != null) {
                headers.add("User-Agent", RestFuUtils.sanitizeHeader(userAgent))
            }
            return HttpRequest(restMethod, url, headers, info.getBody(converterFactory), null)
        }
    }

    class TwidereExceptionFactory internal constructor(private val converterFactory: TwitterConverterFactory) : ExceptionFactory<MicroBlogException> {

        override fun newException(cause: Throwable?, request: HttpRequest, response: HttpResponse): MicroBlogException {
            val te: MicroBlogException
            if (cause != null) {
                te = MicroBlogException(cause)
            } else {
                te = parseTwitterException(response)
            }
            te.httpRequest = request
            te.httpResponse = response
            return te
        }


        fun parseTwitterException(resp: HttpResponse): MicroBlogException {
            try {
                return converterFactory.forResponse(MicroBlogException::class.java).convert(resp) as MicroBlogException
            } catch (e: JsonParseException) {
                return MicroBlogException("Malformed JSON Data", e)
            } catch (e: IOException) {
                return MicroBlogException("IOException while throwing exception", e)
            } catch (e: RestConverter.ConvertException) {
                return MicroBlogException(e)
            } catch (e: MicroBlogException) {
                return e
            }

        }
    }

    private class TwidereRestRequestFactory(private val extraRequestParams: Map<String, String>?) : RestRequest.Factory<MicroBlogException> {

        @Throws(RestConverter.ConvertException::class, IOException::class, MicroBlogException::class)
        override fun create(restMethod: RestMethod<MicroBlogException>,
                            factory: RestConverter.Factory<MicroBlogException>,
                            valuePool: ValueMap): RestRequest {
            val method = restMethod.method
            val path = restMethod.path
            val headers = restMethod.getHeaders(valuePool)
            val queries = restMethod.getQueries(valuePool)
            val params = restMethod.getParams(factory, valuePool)
            val rawValue = restMethod.rawValue
            val bodyType = restMethod.bodyType
            val extras = restMethod.extras

            if (queries != null && extraRequestParams != null) {
                for ((key, value) in extraRequestParams) {
                    queries.add(key, value)
                }
            }

            return RestRequest(method.value, method.allowBody, path, headers, queries,
                    params, rawValue, bodyType, extras)
        }
    }
}
