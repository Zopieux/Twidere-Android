package org.mariotaku.ktextension

/**
 * Created by mariotaku on 16/8/24.
 */

fun <T> Array<out T>.containsAll(target: Array<out T>): Boolean {
    for (item in target) {
        if (item !in this) return false
    }
    return true
}