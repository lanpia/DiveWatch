package com.example.dive.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * 수면(세션 시작) 시점의 GPS 위치를 한 번 가져오고, 좌표를 장소명으로 역지오코딩한다.
 * 권한이 없거나 위치를 못 잡으면 null을 반환(앱은 위치 없이 계속 동작).
 */
object LocationHelper {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context): Pair<Double, Double>? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val lastKnown = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).mapNotNull { p ->
            try {
                if (lm.isProviderEnabled(p)) lm.getLastKnownLocation(p) else null
            } catch (e: Exception) {
                null
            }
        }.maxByOrNull { it.time }

        val fresh = withTimeoutOrNull(8000) { requestFresh(lm, context) }
        val loc = fresh ?: lastKnown ?: return null
        return loc.latitude to loc.longitude
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFresh(lm: LocationManager, context: Context): Location? =
        suspendCancellableCoroutine { cont ->
            try {
                val provider = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    LocationManager.GPS_PROVIDER
                } else {
                    LocationManager.NETWORK_PROVIDER
                }
                val signal = CancellationSignal()
                lm.getCurrentLocation(provider, signal, context.mainExecutor) { loc ->
                    if (cont.isActive) cont.resume(loc)
                }
                cont.invokeOnCancellation { signal.cancel() }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    /** 좌표 → 장소명(예: "서귀포시 색달동"). 실패 시 null. */
    suspend fun placeName(context: Context, lat: Double, lng: Double): String? =
        withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)
                val a = addresses?.firstOrNull() ?: return@withContext null
                val parts = listOfNotNull(
                    a.locality ?: a.subAdminArea ?: a.adminArea,
                    a.thoroughfare ?: a.featureName
                ).distinct()
                if (parts.isNotEmpty()) parts.joinToString(" ") else a.getAddressLine(0)
            } catch (e: Exception) {
                null
            }
        }
}
