package org.mariotaku.twidere.util

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Created by mariotaku on 16/2/15.
 */
object TwidereTypeUtils {

    fun toSimpleName(type: Type): String {
        val sb = StringBuilder()
        buildSimpleName(type, sb)
        return sb.toString()
    }

    private fun buildSimpleName(type: Type, sb: StringBuilder) {
        if (type is Class<*>) {
            sb.append(type.simpleName)
        } else if (type is ParameterizedType) {
            buildSimpleName(type.rawType, sb)
            sb.append("<")
            val args = type.actualTypeArguments
            for (i in args.indices) {
                if (i != 0) {
                    sb.append(",")
                }
                buildSimpleName(args[i], sb)
            }
            sb.append(">")
        }
    }
}
