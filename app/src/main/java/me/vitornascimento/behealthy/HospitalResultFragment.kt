package me.vitornascimento.behealthy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import me.vitornascimento.behealthy.databinding.CustomMarkerInfoBinding

class HospitalResultFragment : Fragment(R.layout.fragment_hospital_result), OnMapReadyCallback {

  private val hospitalIcon: BitmapDescriptor by lazy {
    getHospitalIconBitmap()
  }
  private var lastKnownLocation: Location? = null
  private var token: AutocompleteSessionToken? = null
  private var locationPermissionGranted = false
  private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
  private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
  private lateinit var placesClient: PlacesClient
  private var map: GoogleMap? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    @RequiresApi(Build.VERSION_CODES.N)
    locationPermissionRequest = registerForActivityResult(
      ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
      when {
        permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
          locationPermissionGranted = true
          updateLocationUI()
        }
        permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
          // Only approximate location access granted.
        }
        else -> {
          // No location access granted.
        }
      }
    }

    Places.initialize(requireContext().applicationContext, getString(R.string.places_key))
    placesClient = Places.createClient(requireContext())
    fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
    token = AutocompleteSessionToken.newInstance()

    val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)
  }

  private fun getHospitalIconBitmap(): BitmapDescriptor {
    val vectorDrawable =
      ResourcesCompat.getDrawable(requireContext().resources, R.drawable.ic_round_local_hospital_24, null)
    val bitmap = vectorDrawable?.let {
      Bitmap.createBitmap(
        it.intrinsicWidth,
        vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
      )
    }
    if (vectorDrawable == null || bitmap == null) {
      return BitmapDescriptorFactory.defaultMarker()
    }
    val canvas = Canvas(bitmap)
    vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
    DrawableCompat.setTint(vectorDrawable, Color.RED)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }

  private fun updateLocationUI() {
    if (map == null) {
      return
    }
    try {
      map?.isMyLocationEnabled = true
      getDeviceLocation()
    } catch (e: SecurityException) {
      Log.e("Exception: %s", e.message, e)
    }
  }

  private fun getDeviceLocation() {
    try {
      val locationResult = fusedLocationProviderClient.lastLocation
      locationResult.addOnCompleteListener(requireActivity()) { task ->
        if (task.isSuccessful) {
          lastKnownLocation = task.result
          if (lastKnownLocation != null) {
            map?.moveCamera(
              CameraUpdateFactory.newLatLngZoom(
                LatLng(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude), DEFAULT_ZOOM
              )
            )
          }
          searchHospitals()
        }
      }
    } catch (e: SecurityException) {
      Log.e("Exception: %s", e.message, e)
    }
  }

  private fun getLocationPermission() {
    locationPermissionRequest.launch(
      arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
  }

  override fun onMapReady(map: GoogleMap?) {
    this.map = map
    do {
      if (
        checkSelfPermission(
          requireContext(),
          Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
      ) {
        locationPermissionGranted = true
      } else {
        getLocationPermission()
      }
    } while (!locationPermissionGranted)
    updateLocationUI()
    map?.setOnCameraIdleListener {
      searchHospitals()
    }
    val adapter = object : GoogleMap.InfoWindowAdapter {
      override fun getInfoWindow(p0: Marker?): View? = null

      override fun getInfoContents(marker: Marker?): View {
        val place: Place = marker?.tag as Place
        val binding = CustomMarkerInfoBinding.inflate(layoutInflater)
        binding.hospitalName.text = place.name
        binding.hospitalAddress.text = place.address
        if (!place.phoneNumber.isNullOrBlank()) {
          binding.hospitalPhone.visibility = View.VISIBLE
          binding.hospitalPhone.text = place.phoneNumber
        }
        return binding.root
      }

    }
    map?.setInfoWindowAdapter(adapter)
    map?.uiSettings?.isMapToolbarEnabled = false
    map?.uiSettings?.isZoomControlsEnabled = true
    map?.setOnInfoWindowClickListener {
      val place = it.tag as Place
      if (!place.phoneNumber.isNullOrBlank()) {
        val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:" + place.phoneNumber) }
        startActivity(intent)
      }
    }
  }

  private fun searchHospitals() {
    val request = FindAutocompletePredictionsRequest.builder()
      .setCountry("BR")
      .setSessionToken(token)
      .setLocationRestriction(RectangularBounds.newInstance(this.map?.projection?.visibleRegion?.latLngBounds!!))
      .setQuery("Hospital")
      .build()

    val placeResult = placesClient.findAutocompletePredictions(request)
    val placeFields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.PHONE_NUMBER)
    placeResult.addOnCompleteListener { task ->
      if (task.isSuccessful && task.result != null) {
        val hospitals = task.result.autocompletePredictions
        for (hospital in hospitals) {
          Log.e(TAG, "showCurrentPlace: ${hospital.getFullText(null)}")
          FetchPlaceRequest.builder(hospital.placeId, placeFields).build().run {
            placesClient.fetchPlace(this).addOnSuccessListener {
              addMarker(it.place)
            }
          }
        }
      } else {
        Log.e(TAG, "Exception: %s", task.exception)
      }
    }
  }

  private fun addMarker(place: Place) {
    with(this.map!!) {
      addMarker(
        MarkerOptions()
          .title(place.name)
          .position(place.latLng!!)
          .icon(hospitalIcon)
      ).also {
        it.tag = place
      }
    }
  }

  companion object {
    private val TAG = this::class.java.simpleName
    private const val DEFAULT_ZOOM = 12.5F
  }

}