/*
 * Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package org.mariotaku.twidere.model

/**
 * Created by mariotaku on 15/3/25.
 */
class StringLongPair(val key: String, var value: Long) {

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as StringLongPair?

        return key == that!!.key
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun toString(): String {
        return "$key:$value"
    }

    companion object {

        @Throws(NumberFormatException::class)
        fun valueOf(s: String): StringLongPair {
            val segs = s.split(":").dropLastWhile(String::isEmpty).toTypedArray()
            if (segs.size != 2) throw NumberFormatException()
            return StringLongPair(segs[0], segs[1].toLong())
        }

        fun toString(pairs: Array<StringLongPair>?): String? {
            return pairs?.joinToString(";")
        }

        @Throws(NumberFormatException::class)
        fun valuesOf(s: String?): Array<StringLongPair>? {
            return s?.split(";")?.dropLastWhile(String::isEmpty)?.map {
                valueOf(it)
            }?.toTypedArray()
        }
    }
}
