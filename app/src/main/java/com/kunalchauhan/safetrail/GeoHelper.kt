package com.kunalchauhan.safetrail

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

    // Looks the typed text up as a place name or address
    suspend fun findPlace(context: Context, query: String): PlaceCheck {
        if (!Geocoder.isPresent()) return PlaceCheck.Unavailable
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val results: List<Address>? = withTimeoutOrNull(8000L) { lookup(geocoder, query) }
            when {
                results == null -> PlaceCheck.Unavailable
                results.isEmpty() -> PlaceCheck.NotFound
                else -> results.first().let { PlaceCheck.Found(label(it), it.latitude, it.longitude) }
            }
        } catch (e: Exception) {
            PlaceCheck.Unavailable
        }
    }

    // Turns coordinates into a readable address, or null if it cannot be found
    suspend fun addressFor(context: Context, lat: Double, lng: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            withTimeoutOrNull(8000L) { reverse(geocoder, lat, lng) }
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
}
