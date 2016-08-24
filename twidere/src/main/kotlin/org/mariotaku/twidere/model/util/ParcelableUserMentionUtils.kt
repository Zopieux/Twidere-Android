package org.mariotaku.twidere.model.util

import org.mariotaku.microblog.library.twitter.model.User
import org.mariotaku.microblog.library.twitter.model.UserMentionEntity
import org.mariotaku.twidere.model.ParcelableUserMention
import org.mariotaku.twidere.model.UserKey

/**
 * Created by mariotaku on 16/3/7.
 */
object ParcelableUserMentionUtils {

    fun fromMentionEntity(user: User,
                          entity: UserMentionEntity): ParcelableUserMention {
        val obj = ParcelableUserMention()
        obj.key = UserKey(entity.id, UserKeyUtils.getUserHost(user))
        obj.name = entity.name
        obj.screen_name = entity.screenName
        return obj
    }

    fun fromUserMentionEntities(user: User, entities: Array<UserMentionEntity>?): Array<ParcelableUserMention>? {
        if (entities == null) return null
        return Array(entities.size) {
            fromMentionEntity(user, entities[it])
        }
    }
}
