package org.mariotaku.twidere.model.util

import android.content.Context
import org.mariotaku.twidere.model.Draft
import org.mariotaku.twidere.model.ParcelableStatusUpdate
import org.mariotaku.twidere.model.draft.UpdateStatusActionExtra

/**
 * Created by mariotaku on 16/2/12.
 */
object ParcelableStatusUpdateUtils {

    fun fromDraftItem(context: Context, draft: Draft): ParcelableStatusUpdate {
        val statusUpdate = ParcelableStatusUpdate()
        if (draft.account_keys != null) {
            statusUpdate.accounts = ParcelableAccountUtils.getAccounts(context, *draft.account_keys!!)
        } else {
            statusUpdate.accounts = emptyArray()
        }
        statusUpdate.text = draft.text
        statusUpdate.location = draft.location
        statusUpdate.media = draft.media
        if (draft.action_extras is UpdateStatusActionExtra) {
            val extra = draft.action_extras as UpdateStatusActionExtra?
            statusUpdate.in_reply_to_status = extra!!.inReplyToStatus
            statusUpdate.is_possibly_sensitive = extra.isPossiblySensitive
            statusUpdate.display_coordinates = extra.displayCoordinates
        }
        return statusUpdate
    }

}
