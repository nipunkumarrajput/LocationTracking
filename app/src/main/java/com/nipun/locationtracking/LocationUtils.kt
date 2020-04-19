package com.nipun.locationtracking

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Nipun Kumar Rajput
 */

object LocationUtils {

    fun getBatteryPercentage(context: Context): Int {

        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, iFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        val batteryPct = level / scale.toFloat()

        return (batteryPct * 100).toInt()
    }

    fun isLocationServicesAvailable(context: Context): Boolean {
        var locationMode = 0
        val locationProviders: String
        var isAvailable = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode =
                    Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
            }

            isAvailable = locationMode != Settings.Secure.LOCATION_MODE_OFF
        } else {
            locationProviders =
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED
                )
            isAvailable = !TextUtils.isEmpty(locationProviders)
        }

        val coarsePermissionCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val finePermissionCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return isAvailable && (coarsePermissionCheck || finePermissionCheck)
    }

    fun shouldUpdateLocation(context: Context): Boolean {
        return isLocationEnabled(context) && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        when {
            Build.VERSION.SDK_INT >= 28 -> {
                return locationManager.isLocationEnabled

            }
            Build.VERSION.SDK_INT >= 19 -> {
                var locationManager1 = 0

                try {
                    locationManager1 =
                        Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
                } catch (var3: Settings.SettingNotFoundException) {
                    var3.printStackTrace()
                    Log.e(
                        "isLocationEnabled",
                        "Exception occurred while detecting isLocationEnabled: " + var3.message
                    )
                }

                return locationManager1 != 0
            }
            else -> {

                return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        }
    }

    fun formatDateToDbTime(milis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.getDefault()).format(Date(milis))
    }

    fun checkPlayServices(activity: Activity): Boolean {
        val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(activity)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(
                    activity,
                    resultCode,
                    PLAY_SERVICES_RESOLUTION_REQUEST
                )
                    .show()
            } else {
                Log.e(activity.localClassName, "This device is not supported.")
            }
            return false
        }
        return true
    }

    fun getNotificationChannel(channelId: String): NotificationChannel? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //Notification Channel
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(channelId, channelId, importance)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern =
                longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            notificationChannel
        } else {
            null
        }
    }

    fun locationToString(location: Location): String {
        return "Latitude: " + location.latitude + " " +
                "Longitude: " + location.longitude + " " +
                "Provider: " + location.provider + " " +
                "Time: " + formatDateToDbTime(location.time) + " " +
                "Speed: " + location.speed + " " +
                "Altitude: " + location.altitude + " " +
                "Bearing: " + location.bearing + " " +
                "Accuracy: " + location.accuracy
    }

}
