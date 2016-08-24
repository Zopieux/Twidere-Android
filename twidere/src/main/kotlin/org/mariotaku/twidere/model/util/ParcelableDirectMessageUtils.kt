package org.mariotaku.twidere.model.util

import org.mariotaku.microblog.library.twitter.model.DirectMessage
import org.mariotaku.twidere.model.ParcelableDirectMessage
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.util.InternalTwitterContentUtils
import org.mariotaku.twidere.util.TwitterContentUtils

/**
 * Created by mariotaku on 16/2/13.
 */
object ParcelableDirectMessageUtils {

    fun fromDirectMessage(message: DirectMessage, accountKey: UserKey, isOutgoing: Boolean): ParcelableDirectMessage {
        val result = ParcelableDirectMessage()
        result.account_key = accountKey
        result.is_outgoing = isOutgoing
        val sender = message.sender
        val recipient = message.recipient
        assert(sender != null && recipient != null)
        val sender_profile_image_url = TwitterContentUtils.getProfileImageUrl(sender)
        val recipient_profile_image_url = TwitterContentUtils.getProfileImageUrl(recipient)
        result.id = message.id
        result.timestamp = message.createdAt?.time ?: -1
        result.sender_id = sender!!.id
        result.recipient_id = recipient!!.id
        val pair = InternalTwitterContentUtils.formatDirectMessageText(message)
        result.text_unescaped = pair.first
        result.spans = pair.second
        result.text_plain = message.text
        result.sender_name = sender.name
        result.recipient_name = recipient.name
        result.sender_screen_name = sender.screenName
        result.recipient_screen_name = recipient.screenName
        result.sender_profile_image_url = sender_profile_image_url
        result.recipient_profile_image_url = recipient_profile_image_url
        result.media = ParcelableMediaUtils.fromEntities(message)
        if (isOutgoing) {
            result.conversation_id = result.recipient_id
        } else {
            result.conversation_id = result.sender_id
        }
        return result
    }


}
