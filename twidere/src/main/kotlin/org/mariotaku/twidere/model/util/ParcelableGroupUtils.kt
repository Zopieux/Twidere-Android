package org.mariotaku.twidere.model.util

import org.mariotaku.microblog.library.statusnet.model.Group
import org.mariotaku.twidere.model.ParcelableGroup
import org.mariotaku.twidere.model.UserKey

/**
 * Created by mariotaku on 16/3/9.
 */
object ParcelableGroupUtils {

    fun from(group: Group, accountKey: UserKey, position: Int, member: Boolean): ParcelableGroup {
        val obj = ParcelableGroup()
        obj.account_key = accountKey
        obj.member = member
        obj.position = position.toLong()
        obj.id = group.id
        obj.nickname = group.nickname
        obj.homepage = group.homepage
        obj.fullname = group.fullname
        obj.url = group.url
        obj.description = group.description
        obj.location = group.location
        obj.created = group.created?.time ?: -1
        obj.modified = group.modified?.time ?: -1
        obj.admin_count = group.adminCount
        obj.member_count = group.memberCount
        obj.original_logo = group.originalLogo
        obj.homepage_logo = group.homepageLogo
        obj.stream_logo = group.streamLogo
        obj.mini_logo = group.miniLogo
        obj.blocked = group.isBlocked
        obj.id = group.id
        return obj
    }

}
