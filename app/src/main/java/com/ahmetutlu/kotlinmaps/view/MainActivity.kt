package com.ahmetutlu.kotlinmaps.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.ahmetutlu.kotlinmaps.R
import com.ahmetutlu.kotlinmaps.adapter.PlaceAdapter
import com.ahmetutlu.kotlinmaps.databinding.ActivityMainBinding
import com.ahmetutlu.kotlinmaps.model.Place
import com.ahmetutlu.kotlinmaps.roomdb.PlaceDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val compositeDisposable=CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val db= Room.databaseBuilder(applicationContext, PlaceDatabase::class.java,"Places").build()
        val placeDao=db.PlaceDao()

        //şuan mainActivye verileri çekiiyoruz
        compositeDisposable.add(
            placeDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse)
        )


    }
    private fun handleResponse(placeList: List<Place>) {//yukarda çekilen veriler burdaki listede döndürülecek
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val placeAdapter = PlaceAdapter(placeList)
        binding.recyclerView.adapter = placeAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater=menuInflater
        menuInflater.inflate(R.menu.place_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId== R.id.add_place){
            val intet= Intent(this, MapsActivity::class.java)
            intet.putExtra("info","new")
            startActivity(intet)
        }
        return super.onOptionsItemSelected(item)
    }
}