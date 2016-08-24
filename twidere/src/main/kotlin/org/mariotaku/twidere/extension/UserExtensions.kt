package org.mariotaku.twidere.extension

import org.mariotaku.microblog.library.twitter.model.User

/**
 * Created by mariotaku on 16/8/24.
 */
val User.isFanfouUser: Boolean
    get() = uniqueId != null && profileImageUrlLarge != null