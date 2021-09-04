package com.ahmetutlu.kotlinmaps.view

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.ahmetutlu.kotlinmaps.R
import com.ahmetutlu.kotlinmaps.databinding.ActivityMapsBinding
import com.ahmetutlu.kotlinmaps.model.Place
import com.ahmetutlu.kotlinmaps.roomdb.PlaceDao
import com.ahmetutlu.kotlinmaps.roomdb.PlaceDatabase

import com.google.android.gms.maps.*

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback ,GoogleMap.OnMapLongClickListener{

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissonLauncher:ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    private var trackBoolean:Boolean?=null
    private var selectedLatitude:Double?=null
    private var selectedLongtitude:Double?=null
    private lateinit var db:PlaceDatabase
    private lateinit var placeDao:PlaceDao
    val compositeDisposable=CompositeDisposable()//hafızada yer kaplamaması için kullanıp atmaya yarar
    var placeFromMain:Place?=null
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        registerLauncher()
        sharedPreferences=this.getSharedPreferences("com.ahmetutlu.kotlinmaps", MODE_PRIVATE)
        trackBoolean=false
        selectedLatitude=0.0
        selectedLongtitude=0.0

        db= Room.databaseBuilder(applicationContext,PlaceDatabase::class.java,"Places").build()
        placeDao=db.PlaceDao()

        saveButton.isEnabled=false
    }

    override fun onMapReady(googleMap: GoogleMap) {//harita hazır olunca çağrılan fonksiyon
        mMap = googleMap
        mMap.setOnMapLongClickListener (this)

        val intent=intent
        val info=intent.getStringExtra("info")

        if (info=="new"){
            saveButton.visibility=View.VISIBLE
            deleteButton.visibility=View.GONE

            //casting(aşağıda locationManager olarak tanımladık)
            locationManager=this.getSystemService(LOCATION_SERVICE) as LocationManager

            locationListener= object:LocationListener {
                override fun onLocationChanged(location: Location) {
                    trackBoolean=sharedPreferences.getBoolean("trackBoolean",false)
                    if (trackBoolean==false) {
                        val userLocation = LatLng(location.latitude, location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                        sharedPreferences.edit().putBoolean("trackBoolean",true).apply()
                    }
                }

            }

            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                //izin yok yani izin al
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                    Snackbar.make(getWindow().getDecorView().getRootView(),"Permission needed for location",Snackbar.LENGTH_INDEFINITE).setAction("Give Permisson"){
                        //request permission
                        permissonLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }.show()

                }else{
                    //request permission
                    permissonLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }

            }else{
                //permission granted
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                val lastLocation=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastLocation!=null){
                    val lastUserLocatio=LatLng(lastLocation.latitude,lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocatio,15f))
                }
                mMap.isMyLocationEnabled=true
            }

        } else {
            //Sqlite data && intent data
            mMap.clear()
            placeFromMain = intent.getSerializableExtra("selectedPlace") as? Place
            placeFromMain?.let {
                val latLng = LatLng(it.latitude, it.longitude)

                mMap.addMarker(MarkerOptions().position(latLng).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                placeText.setText(it.name)
                saveButton.visibility = View.GONE
                deleteButton.visibility = View.VISIBLE

            }
        }



    }

    private fun registerLauncher(){
        permissonLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){ result->
            if (result) {
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                    //permission granted
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                    val lastLocation=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastLocation!=null){
                        val lastUserLocatio=LatLng(lastLocation.latitude,lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocatio,15f))
                    }
                    mMap.isMyLocationEnabled=true
                }
            }else{
                //permission denied
                Toast.makeText(this@MapsActivity,"Permission Needed!",Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onMapLongClick(p0: LatLng) {
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(p0))
        selectedLatitude=p0.latitude
        selectedLongtitude=p0.longitude

        saveButton.isEnabled=true
    }
    fun save(view: View){
        if (selectedLatitude!=null && selectedLongtitude!=null) {
            val place = Place(placeText.text.toString(), selectedLatitude!!, selectedLongtitude!!)
            compositeDisposable.add(
                placeDao.insert(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }

    }
    private fun handleResponse(){
        val intent=Intent(this,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
    fun delete(view: View){
        placeFromMain?.let {
            compositeDisposable.add(placeDao.delete(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse))

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}