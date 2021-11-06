package me.vitornascimento.behealthy

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class HospitalResultFragment : Fragment(R.layout.fragment_hospital_result) {

  val args: HospitalResultFragmentArgs by navArgs()

  private val hospitalIcon: BitmapDescriptor by lazy {
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
      return@lazy BitmapDescriptorFactory.defaultMarker()
    }
    val canvas = Canvas(bitmap)
    vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
    DrawableCompat.setTint(vectorDrawable, Color.RED)
    vectorDrawable.draw(canvas)
    BitmapDescriptorFactory.fromBitmap(bitmap)
  }

  private val callback = OnMapReadyCallback { googleMap ->
    val minhaCasa = LatLng(-22.6337, -43.2370)
    with(googleMap) {
      moveCamera(CameraUpdateFactory.newLatLng(minhaCasa))
      addMarker(
        MarkerOptions()
          .position(minhaCasa)
          .icon(hospitalIcon)
      )
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    Toast.makeText(requireContext(), args.selectedSpecialty, Toast.LENGTH_SHORT).show()
    val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
    mapFragment?.getMapAsync(callback)
  }
}