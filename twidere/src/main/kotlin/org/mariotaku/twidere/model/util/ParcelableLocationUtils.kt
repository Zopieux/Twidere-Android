package org.mariotaku.twidere.model.util

import android.location.Location

import org.mariotaku.microblog.library.twitter.model.GeoLocation
import org.mariotaku.twidere.model.ParcelableLocation
import org.mariotaku.twidere.util.InternalParseUtils

/**
 * Created by mariotaku on 16/3/8.
 */
object ParcelableLocationUtils {

    fun getHumanReadableString(obj: ParcelableLocation, decimalDigits: Int): String {
        return String.format("%s,%s", InternalParseUtils.parsePrettyDecimal(obj.latitude, decimalDigits),
                InternalParseUtils.parsePrettyDecimal(obj.longitude, decimalDigits))
    }

    fun fromGeoLocation(geoLocation: GeoLocation?): ParcelableLocation? {
        if (geoLocation == null) return null
        val result = ParcelableLocation()
        result.latitude = geoLocation.latitude
        result.longitude = geoLocation.longitude
        return result
    }

    fun fromLocation(location: Location?): ParcelableLocation? {
        if (location == null) return null
        val result = ParcelableLocation()
        result.latitude = location.latitude
        result.longitude = location.longitude
        return result
    }

    fun isValidLocation(location: ParcelableLocation?): Boolean {
        return location != null && !java.lang.Double.isNaN(location.latitude) && !java.lang.Double.isNaN(location.longitude)
    }

    fun toGeoLocation(location: ParcelableLocation): GeoLocation? {
        return if (isValidLocation(location)) GeoLocation(location.latitude, location.longitude) else null
    }

    fun isValidLocation(latitude: Double, longitude: Double): Boolean {
        return !java.lang.Double.isNaN(latitude) && !java.lang.Double.isNaN(longitude)
    }
}
