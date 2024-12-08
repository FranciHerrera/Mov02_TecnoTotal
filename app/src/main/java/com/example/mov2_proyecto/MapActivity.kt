package com.example.mov2_proyecto

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.model.DirectionsResult
import com.google.maps.model.DirectionsRoute
import com.google.maps.model.GeocodingResult

class MapActivity : AppCompatActivity() , OnMapReadyCallback{
    private lateinit var mMap: GoogleMap
    private lateinit var inicioRuta: String
    private lateinit var finRuta: String
    private lateinit var geoApiContext: GeoApiContext
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        inicioRuta = intent.getStringExtra("inicioRuta") ?: ""
        finRuta = intent.getStringExtra("finRuta") ?: ""

        // Inicializar el mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Inicializar GeoApiContext para Directions API
        geoApiContext = GeoApiContext.Builder()
            .apiKey("AIzaSyB28ehgYtmOx69HIWOBcmxQVDRGLPxhFzI")
            .build()
    }
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Obtener las coordenadas de las direcciones de inicio y fin
        val latLngInicio = getLatLngFromAddress(inicioRuta)
        val latLngFin = getLatLngFromAddress(finRuta)

        // Verifica si las coordenadas son válidas
        if (latLngInicio != null && latLngFin != null) {
            mMap.addMarker(MarkerOptions().position(latLngInicio).title("Inicio"))
            mMap.addMarker(MarkerOptions().position(latLngFin).title("Fin"))

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngInicio, 12f))

            // Traza la ruta
            drawRoute(latLngInicio, latLngFin)
        }
    }

    private fun getLatLngFromAddress(address: String): LatLng? {
        return try {
            // Realiza la búsqueda geocodificada para obtener las coordenadas
            val results: Array<GeocodingResult> = GeocodingApi.geocode(geoApiContext, address).await()

            // Si la búsqueda tiene resultados, obtiene la primera coordenada
            if (results.isNotEmpty()) {
                val location = results[0].geometry.location
                LatLng(location.lat, location.lng)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawRoute(start: LatLng, end: LatLng) {
        val directionsRequest = DirectionsApi.newRequest(geoApiContext)
            .origin(com.google.maps.model.LatLng(start.latitude, start.longitude))
            .destination(com.google.maps.model.LatLng(end.latitude, end.longitude))

        Thread {
            try {
                val result = directionsRequest.await()

                if (result.routes.isNotEmpty()) {
                    val route = result.routes[0]
                    val polylineOptions = PolylineOptions()

                    // Dibuja la ruta
                    for (step in route.legs[0].steps) {
                        val polylinePoints = step.polyline.decodePath()
                        for (point in polylinePoints) {
                            val latLng = LatLng(point.lat, point.lng)
                            polylineOptions.add(latLng)
                        }
                    }

                    runOnUiThread {
                        mMap.addPolyline(polylineOptions)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
