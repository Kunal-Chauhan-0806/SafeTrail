package com.kunalchauhan.safetrail

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume

sealed class PlaceCheck {
    data class Found(val address: String, val latitude: Double, val longitude: Double) : PlaceCheck()
    object NotFound : PlaceCheck()
    object Unavailable : PlaceCheck()
}

object GeoHelper {

    private fun label(a: Address): String {
        val line = a.getAddressLine(0)
        if (!line.isNullOrBlank()) return line
        return listOfNotNull(a.featureName, a.locality, a.adminArea)
            .joinToString(", ")
            .ifBlank { "Unnamed place" }
    }

    // Checks that the typed text is a real place. Tries the phone's own lookup first,
    // then OpenStreetMap, so it still works on phones without Google's lookup service.
    suspend fun findPlace(context: Context, query: String): PlaceCheck {
        val first = findWithGeocoder(context, query)
        if (first is PlaceCheck.Found) return first
        val second = findWithOpenStreetMap(query)
        if (second is PlaceCheck.Found) return second
        return if (first is PlaceCheck.NotFound || second is PlaceCheck.NotFound) {
            PlaceCheck.NotFound
        } else {
            PlaceCheck.Unavailable
        }
    }

    // Turns coordinates into a readable address, or null if none can be found
    suspend fun addressFor(context: Context, lat: Double, lng: Double): String? =
        addressWithGeocoder(context, lat, lng) ?: addressWithOpenStreetMap(lat, lng)

    // ---------- the phone's own Geocoder ----------

    private suspend fun findWithGeocoder(context: Context, query: String): PlaceCheck {
        if (!Geocoder.isPresent()) return PlaceCheck.Unavailable
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val results: List<Address>? = withTimeoutOrNull(6000L) { lookup(geocoder, query) }
            when {
                results == null -> PlaceCheck.Unavailable
                results.isEmpty() -> PlaceCheck.NotFound
                else -> results.first().let { PlaceCheck.Found(label(it), it.latitude, it.longitude) }
            }
        } catch (e: Exception) {
            PlaceCheck.Unavailable
        }
    }

    private suspend fun addressWithGeocoder(context: Context, lat: Double, lng: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            withTimeoutOrNull(6000L) { reverse(geocoder, lat, lng) }
                ?.firstOrNull()
                ?.let { label(it) }
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun lookup(geocoder: Geocoder, query: String): List<Address>? =
        if (Build.VERSION.SDK_INT >= 33) {
            suspendCancellableCoroutine<List<Address>?> { cont ->
                geocoder.getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        cont.resume(addresses)
                    }

                    override fun onError(errorMessage: String?) {
                        cont.resume(null)
                    }
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    geocoder.getFromLocationName(query, 1)
                } catch (e: Exception) {
                    null
                }
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun reverse(geocoder: Geocoder, lat: Double, lng: Double): List<Address>? =
        if (Build.VERSION.SDK_INT >= 33) {
            suspendCancellableCoroutine<List<Address>?> { cont ->
                geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        cont.resume(addresses)
                    }

                    override fun onError(errorMessage: String?) {
                        cont.resume(null)
                    }
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    geocoder.getFromLocation(lat, lng, 1)
                } catch (e: Exception) {
                    null
                }
            }
        }

    // ---------- OpenStreetMap (Nominatim), used as a backup ----------

    private suspend fun fetchText(url: String): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                // OpenStreetMap asks apps to identify themselves
                setRequestProperty("User-Agent", "SafeTrail/1.0 (student project)")
                setRequestProperty("Accept-Language", Locale.getDefault().toLanguageTag())
            }
            if (connection.responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun findWithOpenStreetMap(query: String): PlaceCheck {
        val url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" +
            URLEncoder.encode(query, "UTF-8")
        val body = fetchText(url) ?: return PlaceCheck.Unavailable
        return try {
            val results = JSONArray(body)
            if (results.length() == 0) {
                PlaceCheck.NotFound
            } else {
                val first = results.getJSONObject(0)
                PlaceCheck.Found(
                    first.getString("display_name"),
                    first.getString("lat").toDouble(),
                    first.getString("lon").toDouble()
                )
            }
        } catch (e: Exception) {
            PlaceCheck.Unavailable
        }
    }

    private suspend fun addressWithOpenStreetMap(lat: Double, lng: Double): String? {
        val url = "https://nominatim.openstreetmap.org/reverse?format=json" +
            "&lat=" + String.format(Locale.US, "%.6f", lat) +
            "&lon=" + String.format(Locale.US, "%.6f", lng)
        val body = fetchText(url) ?: return null
        return try {
            val result = JSONObject(body)
            if (result.has("error")) null else result.optString("display_name").ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }
}
