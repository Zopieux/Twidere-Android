package org.mariotaku.twidere.util

import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.restfu.RestAPIFactory

/**
 * Created by mariotaku on 16/5/27.
 */
class MicroBlogBuilder {

    internal val factory: RestAPIFactory<MicroBlogException>

    init {
        factory = RestAPIFactory<MicroBlogException>()
    }


    fun <T> build(cls: Class<T>): T {
        return factory.build(cls)
    }

}
