package org.mariotaku.ktextension

/**
 * Created by mariotaku on 16/8/17.
 */

inline fun <T> configure(receiver: T, block: T.() -> Unit): T {
    receiver.block()
    return receiver
}

inline fun <F, T> F.convert(convert: (F) -> T): T {
    return convert(this)
}
