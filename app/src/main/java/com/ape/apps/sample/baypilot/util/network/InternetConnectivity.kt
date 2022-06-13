/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ape.apps.sample.baypilot.util.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.LiveData

enum class InternetStatus {
  ONLINE, OFFLINE;
}

class InternetConnectivity(context: Context) : LiveData<InternetStatus>() {

  companion object {
    private const val TAG = "BayPilotInternetConnectivity"
    fun isConnectedToInternet(context: Context): Boolean {
      val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      val activeNetwork = connectivityManager.activeNetwork ?: return false
      return connectivityManager.getNetworkCapabilities(activeNetwork)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    }
  }

  val connectivityManager: ConnectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  var connectionList = mutableSetOf<Network>()

  fun emitStatus() {
    if (connectionList.isNotEmpty()) {
      postValue(InternetStatus.ONLINE)
    } else {
      postValue(InternetStatus.OFFLINE)
    }

  }

  override fun onActive() {
    super.onActive()
    val networkRequest = NetworkRequest
      .Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()
    connectivityManager.registerNetworkCallback(networkRequest, getNetworkCallback())
  }

  override fun onInactive() {
    super.onInactive()
    try {
      connectivityManager.unregisterNetworkCallback(getNetworkCallback())
    } catch (e: Exception) {
      Log.d(TAG, "Network callback was already unregistered")
    }
  }

  private fun getNetworkCallback() = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
      Log.d(TAG, "New Network Available $network")
      val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
      val isInternetAvailable =
        networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
      if (isInternetAvailable) {
        connectionList.add(network)
        emitStatus()
      }
    }

    override fun onLost(network: Network) {
      Log.d(TAG, "Network Lost $network")
      connectionList.remove(network)
      emitStatus()
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
      if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
        connectionList.add(network)
      } else {
        connectionList.remove(network)
      }
    }
  }

}