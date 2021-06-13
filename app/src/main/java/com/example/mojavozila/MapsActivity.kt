package com.example.mojavozila

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    lateinit var ref: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var zadnjaLokacija: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var ID: String

    private var locationUpdateState = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private  const val REQUEST_CHECK_SETTINGS = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val intent = intent
        val korisnikId = intent.getStringExtra("id")
        ID = korisnikId
        ref = FirebaseDatabase.getInstance().getReference("korisnici")

        ref.addValueEventListener(object :ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot!!.exists()){
                    for(k in snapshot.children){
                        val korisnik = k.getValue(Korisnik::class.java)
                        if(korisnik!!.id != ID) {
                            val location = LatLng(korisnik.lat, korisnik.lon)
                            val markerOptions =
                                MarkerOptions().position(location)
                                    .title(korisnik.ime.toString() + " " + korisnik.prezime.toString()) 
                            markerOptions.icon(
                                BitmapDescriptorFactory.fromBitmap(
                                    BitmapFactory.decodeResource(resources, R.drawable.ic_lokacija)
                                )
                            )
                            map.addMarker(markerOptions)
                        }
                    }
                }
            }
        })

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                zadnjaLokacija = p0.lastLocation
                val azuriranaLokacija = LatLng(zadnjaLokacija.latitude,zadnjaLokacija.longitude)
                azurirajKorisnika(azuriranaLokacija)
                postaviMarker(LatLng(zadnjaLokacija.latitude, zadnjaLokacija.longitude))
            }
        }
        kreirajLocationRequest()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if(id == R.id.postavke_id) {
            return true
        }else if (id == R.id.odjava_id) {
            odjaviSe()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun odjaviSe() {
        ref.child(ID).setValue(null,null, null)
        finish()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.getUiSettings().setZoomControlsEnabled(true)
        map.setOnMarkerClickListener(this)

        postaviKartu()
    }

    override fun onMarkerClick(p0: Marker?) = false

    private fun postaviKartu(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        map.isTrafficEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {

                zadnjaLokacija = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                azurirajKorisnika(currentLatLng)
                postaviMarker(currentLatLng)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 13f))
            }
        }
    }

    private fun azurirajKorisnika(latLng: LatLng) {
        ref.child(ID).child("lat").setValue(latLng.latitude)
        ref.child(ID).child("lon").setValue(latLng.longitude)
    }

    private fun postaviMarker(location: LatLng){
        map.clear()
        val markerOptions = MarkerOptions().position(location)

        markerOptions.icon(
            BitmapDescriptorFactory.fromBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.ic_lokacija_moja)))
        map.addMarker(markerOptions)
    }


    private fun azurirajLokaciju() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null )
    }

    private fun kreirajLocationRequest() {

        locationRequest = LocationRequest()
        locationRequest.interval = 4000
        locationRequest.fastestInterval = 1000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)


        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            azurirajLokaciju()
        }
        task.addOnFailureListener { e ->

            if (e is ResolvableApiException) {

                try {

                    e.startResolutionForResult(this@MapsActivity,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {

                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                azurirajLokaciju()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            azurirajLokaciju()
        }
    }
}