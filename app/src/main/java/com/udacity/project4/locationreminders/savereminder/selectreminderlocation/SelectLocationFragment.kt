package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val runningQorLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    private val REQUEST_CODE_FOREANDBACKGROUND = 1
    private val REQUEST_CODE_FOREGROUND_ONLY = 2
    private val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    private val zoom = 18f
    private var marker: Marker? = null

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        if (marker == null) {
            binding.saveButton.text = getString(R.string.select_poi)
        }
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        checkDeviceLocationSettings_requestToTurnOnifNotAlready()
        onLocationSelected()

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkIfLocationPermissionsGranted(): Boolean {

        val foregroundPermission = (ActivityCompat.checkSelfPermission(
            context!!,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)

        //access background location permission handled for api 29 or + using runningQorLater boolean.
        val backgroundPermission = if (runningQorLater) {
            ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return foregroundPermission && backgroundPermission
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestLocationPermissions() {

        if (checkIfLocationPermissionsGranted()) {

            if (this::map.isInitialized) {
                setUpCurrentLocation()
            }
            return
        }

        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = if (runningQorLater) {
            permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
            REQUEST_CODE_FOREANDBACKGROUND
        } else {
            REQUEST_CODE_FOREGROUND_ONLY
        }


        requestPermissions(
            permissionsArray,
            resultCode
        )

    }


    override fun onStart() {
        super.onStart()
        if (this::map.isInitialized) {
            setUpCurrentLocation()
        }
    }

    private fun checkDeviceLocationSettings_requestToTurnOnifNotAlready(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (Ex: IntentSender.SendIntentException) {
                    Log.d("SelectLocationFragment", "Error: " + Ex.message)
                }
            } else {
                Snackbar.make(
                    requireView(),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings_requestToTurnOnifNotAlready()
                }.show()
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)


        if (
            grantResults.isEmpty() ||
            grantResults[0] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_CODE_FOREANDBACKGROUND &&
                    grantResults[1] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            Snackbar.make(
                requireView(),
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    Navigation.findNavController(requireView())
                        .navigate(R.id.action_selectLocationFragment_to_saveReminderFragment)
                }.show()
            checkDeviceLocationSettings_requestToTurnOnifNotAlready()
        } else {
            if (this::map.isInitialized) {
                setUpCurrentLocation()
            }
        }
    }

    private fun onLocationSelected() {
        binding.saveButton.setOnClickListener {
            if (marker != null) {
                Navigation.findNavController(requireView())
                    .navigate(R.id.action_selectLocationFragment_to_saveReminderFragment)
            } else {
                Toast.makeText(context, getString(R.string.select_poi), Toast.LENGTH_SHORT).show()
            }
        }


    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Done: Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onMapReady(googleMap: GoogleMap?) {


        googleMap?.apply {
            map = googleMap
        }
        if (this::map.isInitialized) {
            setUpCurrentLocation()
            setUpPoiSelection(map)
            setUpMapStyle(map)
        }

    }

    private fun setUpPoiSelection(map: GoogleMap) {
        map.setOnPoiClickListener {
            binding.saveButton.text = getString(R.string.save)
            _viewModel.selectedPOI.value = it
            if (marker != null) {
                marker!!.remove()
            }
            marker = map.addMarker(MarkerOptions().position(it.latLng).title(it.name))
        }
    }

    //        Done: add style to the map
    private fun setUpMapStyle(map: GoogleMap) {
        try {
            val success =
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.mapstyle))

        } catch (e: Resources.NotFoundException) {
            Log.i("SelectLocationFragment", "style not found.")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    private fun setUpCurrentLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context!!)

        if (!checkIfLocationPermissionsGranted()) {
            requestLocationPermissions()
            return
        }

        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        val location = fusedLocationProviderClient.lastLocation

        location.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Set the map's camera position to the current location of the device.
                val lastKnownLocation = task.result
                if (lastKnownLocation != null) {
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                lastKnownLocation.latitude,
                                lastKnownLocation.longitude
                            ), zoom
                        )
                    )
                }
            } else {
                val sydney = LatLng(-33.852, 151.211)
                map.addMarker(
                    MarkerOptions()
                        .position(sydney)
                        .title("Sydney")
                )
                map.moveCamera(
                    CameraUpdateFactory
                        .newLatLngZoom(sydney, zoom)
                )
                map.uiSettings?.isMyLocationButtonEnabled = false
            }

        }
    }


}
