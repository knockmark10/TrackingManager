package com.android.tracking

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.android.trackinglibrary.callbacks.TrackingCallback
import com.android.trackinglibrary.managers.TrackingManager

class MainActivity : AppCompatActivity(), TrackingCallback {

    private val trackingManager by lazy {
        TrackingManager
            .Builder
            .setFastestInterval(15000)
            .setUpdateInterval(10000)
            .useLooper(true)
            .build(this)
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.trackingManager.setOnLocationChangeListener(this)
        Thread.sleep(2000)
        this.trackingManager.startLocationUpdates(this)
    }

    override fun onLocationHasChanged(location: Location) {
        Log.d(MainActivity::class.java.name, "HasChanged")
        this.trackingManager.stopLocationUpdates()
    }

    override fun onLocationHasChangedError(error: Exception) {
        Log.e(MainActivity::class.java.name, "Exception")
    }
}
