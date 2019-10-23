package com.android.trackinglibrary.callbacks

import android.location.Location

interface TrackingCallback {
    fun onLocationHasChanged(location: Location)
    fun onLocationHasChangedError(error: Exception)
}