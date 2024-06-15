package com.example.myapplicationformaps

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val defaultLocation = LatLng(19.722307860092844, -101.18534445508105)
    private val locationPermissionRequestCode = 1
    private var showingCurrentLocation = false
    private var currentLocation: LatLng? = null
    private lateinit var fab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()

        // Obtén el fragmento de mapa y notifica cuando el mapa esté listo
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fab = findViewById(R.id.floatingActionButton3)
        fab.setOnClickListener {
            toggleLocation()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Añade un marcador en la ubicación predeterminada y mueve la cámara
        mMap.addMarker(MarkerOptions().position(defaultLocation).title("Marker in Default Location"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLocation))

        // Llama a la notificación cuando el mapa esté listo
        showNotification()
    }

    private fun toggleLocation() {
        if (showingCurrentLocation) {
            // Muestra la ubicación predeterminada
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(defaultLocation).title("Marker in Default Location"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
            showingCurrentLocation = false
            fab.setImageResource(android.R.drawable.ic_menu_mylocation)
        } else {
            // Muestra la ubicación actual
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionRequestCode)
            } else {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        currentLocation = LatLng(location.latitude, location.longitude)
                        mMap.clear()
                        mMap.addMarker(MarkerOptions().position(currentLocation!!).title("Marker in Current Location"))
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 15f))
                        showingCurrentLocation = true
                        fab.setImageResource(android.R.drawable.ic_menu_mapmode)
                    } else {
                        Toast.makeText(this, "No se pudo obtener la ubicación actual. Mostrando ubicación predeterminada.", Toast.LENGTH_LONG).show()
                        mMap.clear()
                        mMap.addMarker(MarkerOptions().position(defaultLocation).title("Marker in Default Location"))
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
                        fab.setImageResource(android.R.drawable.ic_menu_mylocation)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequestCode) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                toggleLocation()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado. Mostrando ubicación predeterminada.", Toast.LENGTH_LONG).show()
                mMap.clear()
                mMap.addMarker(MarkerOptions().position(defaultLocation).title("Marker in Default Location"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "MapChannel"
            val descriptionText = "Channel for map notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("MAP_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "MAP_CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de que este recurso existe en tu proyecto
            .setContentTitle("Mapa cargado")
            .setContentText("El mapa se ha cargado correctamente.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND) // Esto asegura que el sonido predeterminado se use

        with(NotificationManagerCompat.from(this)) {
            notify(0, builder.build())
        }
    }

}
