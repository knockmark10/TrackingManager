package com.android.trackinglibrary.managers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.support.annotation.RequiresPermission
import android.support.v4.content.ContextCompat
import com.android.permissionlibrary.callbacks.PermissionCallback
import com.android.permissionlibrary.managers.PermissionManager
import com.android.trackinglibrary.callbacks.TrackingCallback
import com.google.android.gms.location.*

class TrackingManager(private val mContext: Context) : PermissionCallback, LocationCallback() {

    private var mUpdateInterval: Long = (15 * 1000).toLong()

    private var mFastestInterval: Long = (10 * 1000).toLong()

    private var mListener: TrackingCallback? = null

    private var mFusedClient: FusedLocationProviderClient? = null

    private var mPermissionsManager: PermissionManager? = null

    private var mLooper: Looper? = null

    private val mLocationRequest: LocationRequest by lazy {
        LocationRequest().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = mUpdateInterval
            fastestInterval = mFastestInterval
        }
    }

    /**
     * Request location updates to track user's location
     * @param activity to auto-setup permission request
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startLocationUpdates(activity: Activity? = null) {
        this.checkManifestPermissions()
        this.initializePermissionManager(activity)
        if (this.isLocationPermissionGranted()) {
            this.setupLocation()
        } else {
            this.requestLocationPermission()
        }
    }

    /**
     * Stop receiving location updates (turn off gps use)
     */
    fun stopLocationUpdates() {
        this.mFusedClient?.removeLocationUpdates(this)
    }

    /**
     * Request user's last known location
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun getLastLocation(activity: Activity?) {
        this.checkManifestPermissions()
        this.initializePermissionManager(activity)
        if (this.isLocationPermissionGranted()) {
            val locationClient = LocationServices.getFusedLocationProviderClient(mContext)
            locationClient.lastLocation
                .addOnSuccessListener { mListener?.onLocationHasChanged(it) }
                .addOnFailureListener { mListener?.onLocationHasChangedError(it) }
        } else {
            this.requestLocationPermission()
        }
    }

    /**
     * Checks whether or not location services are up and running
     * @return if services are enabled
     */
    fun areLocationServicesEnabled(): Boolean {
        val locationManager =
            this.mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        val isNetworkAvailable =
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                ?: false
        return isGpsEnabled && isNetworkAvailable && this.isLocationPermissionGranted()
    }

    fun setOnLocationChangeListener(listener: TrackingCallback?) {
        this.mListener = listener
    }

    private fun initializePermissionManager(activity: Activity?) =
        activity?.let { this.mPermissionsManager = PermissionManager(it, this) }

    private fun checkManifestPermissions() {
        this.mPermissionsManager?.let {
            if (!it.isPermissingPresentInManifest(Manifest.permission.ACCESS_FINE_LOCATION)) {
                throw SecurityException("Manifest permission missing. You need ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION to use this feature.")
            }
        }
    }

    private fun requestLocationPermission() {
        this.mPermissionsManager?.let {
            if (!it.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                it.requestSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } ?: run { this.onPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION) }
    }

    private fun setupLocation() {
        LocationServices
            .getSettingsClient(this.mContext)
            .checkLocationSettings(LocationSettingsRequest
                .Builder()
                .apply { addLocationRequest(mLocationRequest) }
                .build()
            )
        this.mFusedClient = LocationServices.getFusedLocationProviderClient(this.mContext)
        this.mFusedClient?.requestLocationUpdates(this.mLocationRequest, this, this.mLooper)
    }

    private fun isLocationPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            mContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    override fun onPermissionGranted(permissions: String) {
        this.setupLocation()
    }

    override fun onPermissionDenied(permission: String) {
        this.mListener?.onLocationHasChangedError(
            RuntimeException("Location services won't work if location permissions are denied. $permission is required.")
        )
    }

    override fun onLocationResult(locationResult: LocationResult?) {
        super.onLocationResult(locationResult)
        locationResult?.lastLocation?.let { this.mListener?.onLocationHasChanged(it) }
    }

    object Builder {

        private var updateInterval: Long = (15 * 1000).toLong()

        private var fastestInterval: Long = (10 * 1000).toLong()

        private var useLooper: Boolean = false

        fun setUpdateInterval(milliseconds: Long): Builder {
            this.updateInterval = milliseconds
            return this
        }

        fun setFastestInterval(milliseconds: Long): Builder {
            this.fastestInterval = milliseconds
            return this
        }

        fun useLooper(useLooper: Boolean): Builder {
            this.useLooper = useLooper
            return this
        }

        fun build(context: Context): TrackingManager = TrackingManager(context).apply {
            this.mUpdateInterval = updateInterval
            this.mFastestInterval = fastestInterval
            this.mLooper = if (useLooper) Looper.myLooper() else null
        }

    }

}