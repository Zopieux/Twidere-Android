/*
 * Twidere - Twitter client for Android
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

package org.mariotaku.twidere.loader

import android.content.Context
import android.os.Bundle
import android.support.v4.content.AsyncTaskLoader
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.model.ErrorInfo
import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.constant.IntentConstants
import org.mariotaku.twidere.model.ParcelableStatus
import org.mariotaku.twidere.model.SingleResponse
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.model.util.ParcelableCredentialsUtils
import org.mariotaku.twidere.model.util.ParcelableStatusUtils
import org.mariotaku.twidere.util.DataStoreUtils
import org.mariotaku.twidere.util.UserColorNameManager
import org.mariotaku.twidere.util.Utils.findStatus
import org.mariotaku.twidere.util.dagger.GeneralComponentHelper
import javax.inject.Inject

/**
 * Created by mariotaku on 14/12/5.
 */
class ParcelableStatusLoader(
        context: Context,
        private val omitIntentExtra: Boolean,
        private val extras: Bundle?,
        private val accountKey: UserKey?,
        private val statusId: String?
) : AsyncTaskLoader<SingleResponse<ParcelableStatus>>(context), Constants {

    @Inject
    lateinit var userColorNameManager: UserColorNameManager

    init {
        GeneralComponentHelper.build(context).inject(this)
    }

    override fun loadInBackground(): SingleResponse<ParcelableStatus> {
        if (accountKey == null || statusId == null) return SingleResponse.getInstance<ParcelableStatus>()
        if (!omitIntentExtra && extras != null) {
            val cache = extras.getParcelable<ParcelableStatus>(IntentConstants.EXTRA_STATUS)
            if (cache != null) {
                val response = SingleResponse.getInstance(cache)
                val extras = response.extras
                extras.putParcelable(IntentConstants.EXTRA_ACCOUNT, ParcelableCredentialsUtils.getCredentials(context, accountKey))
                return response
            }
        }
        try {
            val credentials = ParcelableCredentialsUtils.getCredentials(context, accountKey) ?: return SingleResponse.getInstance<ParcelableStatus>()
            val status = findStatus(context, accountKey, statusId)
            ParcelableStatusUtils.updateExtraInformation(status, credentials, userColorNameManager)
            val response = SingleResponse.getInstance(status)
            val extras = response.extras
            extras.putParcelable(IntentConstants.EXTRA_ACCOUNT, credentials)
            return response
        } catch (e: MicroBlogException) {
            if (e.errorCode == ErrorInfo.STATUS_NOT_FOUND) {
                // Delete all deleted status
                val cr = context.contentResolver
                DataStoreUtils.deleteStatus(cr, accountKey,
                        statusId, null)
                DataStoreUtils.deleteActivityStatus(cr, accountKey, statusId, null)
            }
            return SingleResponse.getInstance<ParcelableStatus>(e)
        }

    }

    override fun onStartLoading() {
        forceLoad()
    }

}
