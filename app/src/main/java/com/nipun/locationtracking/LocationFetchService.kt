package com.nipun.locationtracking

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


open class LocationFetchService : Service() {
    private var oldLocation: Location = Location("locationOfStartPoint")

    var mIntent: Intent? = null

    override fun onCreate() {
        super.onCreate()
        loginToFirebase()
        buildNotification()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mIntent = intent
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        mIntent = intent
        return null
    }

    private fun buildNotification() {
        val stop = "stop"
        registerReceiver(stopReceiver, IntentFilter(stop))
        val broadcastIntent = PendingIntent.getBroadcast(
            this, 0, Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Create the persistent notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(CHANNEL_ID, "My Background Service")
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .setContentIntent(broadcastIntent)
            .setSmallIcon(R.mipmap.ic_location_tracking)

        startForeground(NOTIFICATION_ID, builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String? {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    protected var stopReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "received stop broadcast")
            // Stop the service when the notification is tapped
            unregisterReceiver(this)
            stopSelf()
            stopForeground(true)
        }
    }

    private fun loginToFirebase() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            requestLocationUpdates()
        } else {
            //stop service
            val intent = Intent("stop")
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    private fun requestLocationUpdates() {
        val auth = FirebaseAuth.getInstance()
        val client = LocationServices.getFusedLocationProviderClient(this)
        val permission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val path = getString(R.string.firebase_path) + "/" + auth.currentUser?.uid
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is
            // received, store the location in Firebase

            client.requestLocationUpdates(createLocationRequest(), object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val ref = FirebaseDatabase.getInstance().getReference(path)
                    val location = locationResult.lastLocation
                    if (location != null) {
                        if (oldLocation.latitude != 0.0) {
                            val distance =
                                location.distanceTo(oldLocation).toDouble()
                            Log.d("", "distance= $distance")
                            if (distance > 5) {
                                Log.d(TAG, "location update $location")
                                ref.setValue(location)
                                ref.child("name").setValue(auth.currentUser?.displayName)
                            }
                        }
                        oldLocation = location
                    }
                }
            }, null)
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(stopReceiver)
        } catch (IllegalArgumentException: IllegalArgumentException) {
        }
        super.onDestroy()

    }

    private fun createLocationRequest(): LocationRequest {
        val mLocationRequest = LocationRequest()
        mLocationRequest.interval = 2000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.smallestDisplacement = 5F
        return mLocationRequest
    }

    companion object {
        const val TAG = "LOCATION_FETCH_SERVICE"
        private val CHANNEL_ID = "channel_01"
        private val NOTIFICATION_ID = 12345678
    }
}
