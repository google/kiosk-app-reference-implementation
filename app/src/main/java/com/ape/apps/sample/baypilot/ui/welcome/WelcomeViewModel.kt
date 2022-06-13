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

package com.ape.apps.sample.baypilot.ui.welcome

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ape.apps.sample.baypilot.R
import com.ape.apps.sample.baypilot.data.sharedprefs.SharedPreferencesManager
import com.ape.apps.sample.baypilot.util.dlc.DeviceLockMessenger
import com.ape.apps.sample.baypilot.util.dlc.DeviceLockServiceProtocol
import com.ape.apps.sample.baypilot.util.worker.DatabaseSyncWorker
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class WelcomeViewModel : ViewModel() {

  companion object {
    private const val TAG = "BayPilotWelcomeViewModel"
  }

  override fun onCleared() {
    super.onCleared()

    Log.d(TAG, "onCleared() called")
  }

  fun checkFirstRun(context: Context) {
    val sharedPreferencesManager = SharedPreferencesManager(context)

    val isFirstRun = sharedPreferencesManager.isFirstRun()
    if (isFirstRun) {
      Log.d(TAG, "Reached here means there was error in first initial setup.")

      initialSetup(context)
    } else {
      Log.d(TAG, "Not first run...")
    }
  }

  // Initial Setup to be run when app is first run.
  // Get IMEI from DLC -> Store it in SharedPreferences
  // -> Fetch CreditPlan details from Database
  // -> Store firebase token for device in database.
  fun initialSetup(context: Context) {
    Log.d(TAG, "initialSetup() called with: context")

    val messenger = DeviceLockMessenger()

    viewModelScope.launch {
      val connected = messenger.bindToDlcAndWait(context)

      if (connected) {
        Log.d(TAG, "Connected to DLC. Getting IMEI")

        // Messenger to handle reply from DLC for imei and run further setup.
        val replyMessenger = Messenger(object : Handler(Looper.getMainLooper()) {
          override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            Log.d(TAG, "handleMessage() called with: msg = $msg")

            if (msg.what == DeviceLockServiceProtocol.CLIENT_MSG_QUERY_IMEI) {
              // Retrieve imei from reply.
              val bundle = msg.data
              val imei = bundle.getString(DeviceLockServiceProtocol.KEY_IMEI) ?: ""
              Log.d(TAG, "Received IMEI = $imei")

              // Write IMEI to sharedPreferences to access easily for future references.
              val sharedPreferencesManager = SharedPreferencesManager(context)
              sharedPreferencesManager.writeIMEI(imei)
              Log.d(TAG, "Wrote IMEI to sharedPreferences. Scheduling Sync")

              // Schedule first sync to fetch credit details from Realtime DB
              DatabaseSyncWorker.scheduleSync(context)

              // Get unique token for Firebase Cloud Messaging and store it in Realtime DB under device IMEI
              // to send updates to device whenever credit plan is updated.
              FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                Log.d(TAG, "Setting FCM Token for imei: $imei to $token during initial setup")

                // Token fetched successfully, Connect to Realtime DB and store FCM token.
                val database = FirebaseDatabase.getInstance()
                val imeiRef = database.getReference(imei)
                imeiRef.child(context.getString(R.string.firebase_token_column)).setValue(token).addOnSuccessListener {
                  Log.d(TAG, "FCM token successfully stored in Database. Setting first run to false in sharedPrefs")
                  sharedPreferencesManager.writeFirstRun(false)
                }
              }.addOnFailureListener { error ->
                Log.e(TAG, "Failed to connect to firebase to get FCM token: $error")
                // TODO: Add some retry. currently working when restarting app once.
              }
            }
          }
        })

        // Send messenger to query IMEI and use above messenger to handle reply.
        messenger.sendMessage(DeviceLockServiceProtocol.CLIENT_MSG_QUERY_IMEI, replyMessenger)
      } else {
        Log.e(TAG, "Couldn't connect to DLC")
      }
    }
  }

}