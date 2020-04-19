package com.nipun.locationtracking.fragments

import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.tasks.Task
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.nipun.locationtracking.LocationFetchService
import com.nipun.locationtracking.LocationUtils
import com.nipun.locationtracking.R
import com.nipun.locationtracking.databinding.FragmentLocationHomeBinding
import com.nipun.locationtracking.helper.GoogleMapHelper
import com.nipun.locationtracking.helper.LatLngInterpolator
import com.nipun.locationtracking.helper.MarkerAnimationHelper


class LocationHomeFragment : BaseFragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentLocationHomeBinding
    private val mMarkers = HashMap<String, Marker>()
    private var mMap: GoogleMap? = null
    private var myPositionMarker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLocationHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(
                requireContext()
            )
        ) {
            findNavController().navigate(
                LocationHomeFragmentDirections.actionLocationHomeFragmentToPermissionsFragment()
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        if (!LocationUtils.isLocationEnabled(requireContext())) {
            enableGps()
        }
        startTrackerService()
        binding.fabLogOut.setOnClickListener {
            AuthUI.getInstance().signOut(requireContext()).addOnCompleteListener {
                if (it.isSuccessful)
                    findNavController().navigate(LocationHomeFragmentDirections.actionLocationHomeFragmentToLoginFragment())
            }
        }
        binding.fabGps.setOnClickListener {
            val location = myPositionMarker?.position
            if (location != null)
                animateCamera(location)
        }
    }

    private fun enableGps() {
        if (LocationUtils.checkPlayServices(
                requireActivity()
            )
        ) {
            val locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.interval = (30 * 1000).toLong()
            locationRequest.fastestInterval = (5 * 1000).toLong()
            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
            val result =
                LocationServices.getSettingsClient(requireActivity())
                    .checkLocationSettings(builder.build())
            showLocationSettingDialog(result)
        }
    }

    private fun showLocationSettingDialog(task: Task<LocationSettingsResponse>) {
        task.addOnCompleteListener { task1 ->
            try {
                val response = task1.getResult(ApiException::class.java)
                if (response?.locationSettingsStates?.isGpsUsable!!) {
                    openLocationSettings()
                }
                // All location settings are satisfied. The client can initialize location
                // requests here.
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->
                        // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.
                        try {
                            // Cast to a resolvable exception.
                            val resolvable = exception as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                requireActivity(),
                                REQUEST_CHECK_SETTINGS
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            // Ignore the error.
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                        }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    }
                }// Location settings are not satisfied. However, we have no way to fix the
                // settings so we won't show the dialog.
            }

        }
    }

    private fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun startTrackerService() {
        requireContext().startService(Intent(requireContext(), LocationFetchService::class.java))
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        this.mMap = googleMap
        mMap?.setMaxZoomPreference(16F)
        subscribeToUpdates()
    }

    private fun subscribeToUpdates() {
        val ref = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_path))
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(
                dataSnapshot: DataSnapshot,
                previousChildName: String?
            ) {
                setMarker(dataSnapshot)
            }

            override fun onChildChanged(
                dataSnapshot: DataSnapshot,
                previousChildName: String?
            ) {
                setMarker(dataSnapshot)
            }

            override fun onChildMoved(
                dataSnapshot: DataSnapshot,
                previousChildName: String?
            ) {
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    private fun setMarker(dataSnapshot: DataSnapshot) {
        // When a location update is received, put or update
        // its value in mMarkers, which contains all the markers
        // for locations received, so that we can build the
        // boundaries required to show them all on the map at once
        val key = dataSnapshot.key
        Log.d(TAG, "$key || ${dataSnapshot.value}")
        val value = dataSnapshot.value as HashMap<String, Any>?
        val lat = value!!["latitude"].toString().toDouble()
        val lng = value["longitude"].toString().toDouble()
        val name = value["name"].toString()
        val location = LatLng(lat, lng)
        if (!mMarkers.containsKey(key)) {
            mMarkers[key!!] = mMap!!.addMarker(
                GoogleMapHelper.geUserMarkerOptions(location).title(name)
            )
        } else {
            MarkerAnimationHelper.animateMarkerToGB(
                myPositionMarker!!,
                location,
                LatLngInterpolator.Spherical()
            )
            mMarkers[key]!!.position = location
        }
        if (key == getFireBaseAuth()?.currentUser?.uid) {
            myPositionMarker = mMarkers[key]
            animateCamera(location)
        }
//        val builder = LatLngBounds.Builder()
//        for (marker in mMarkers.values) {
//            builder.include(marker.position)
//        }
        //mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300))
    }

    private fun animateCamera(latLng: LatLng) {
        val cameraUpdate = GoogleMapHelper.buildCameraUpdate(latLng)
        mMap?.animateCamera(cameraUpdate, 10, null)
    }

    companion object {
        private const val REQUEST_CHECK_SETTINGS = 0x2
        const val TAG = "LocationHomeFragment"
    }

}
