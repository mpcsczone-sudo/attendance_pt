package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class GeoTagResult(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val addressLabel: String
)

object GeoLocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun captureCurrentLocation(context: Context): GeoTagResult? = withContext(Dispatchers.IO) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val tokenSource = CancellationTokenSource()

        val location = suspendCancellableCoroutine<Location?> { cont ->
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        cont.resume(loc)
                    } else {
                        client.lastLocation
                            .addOnSuccessListener { last -> cont.resume(last) }
                            .addOnFailureListener { cont.resume(null) }
                    }
                }
                .addOnFailureListener {
                    client.lastLocation
                        .addOnSuccessListener { last -> cont.resume(last) }
                        .addOnFailureListener { cont.resume(null) }
                }
            cont.invokeOnCancellation { tokenSource.cancel() }
        } ?: return@withContext null

        val addressLabel = reverseGeocode(context, location.latitude, location.longitude)
        GeoTagResult(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            addressLabel = addressLabel
        )
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(context: Context, lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var result: String? = null
                val latch = java.util.concurrent.CountDownLatch(1)
                geocoder.getFromLocation(lat, lon, 1) { list ->
                    val first = list.firstOrNull()
                    result = listOfNotNull(
                        first?.featureName,
                        first?.subLocality,
                        first?.locality,
                        first?.adminArea
                    ).distinct().joinToString(", ")
                    latch.countDown()
                }
                latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
                result
            } else {
                val first = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                listOfNotNull(
                    first?.featureName,
                    first?.subLocality,
                    first?.locality,
                    first?.adminArea
                ).distinct().joinToString(", ")
            }
            addresses?.takeIf { it.isNotBlank() }
                ?: String.format(Locale.US, "GPS %.5f, %.5f", lat, lon)
        } catch (_: Exception) {
            String.format(Locale.US, "GPS %.5f, %.5f", lat, lon)
        }
    }
}
