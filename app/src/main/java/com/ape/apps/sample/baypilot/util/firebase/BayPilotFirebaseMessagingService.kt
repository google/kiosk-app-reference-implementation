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

package com.ape.apps.sample.baypilot.util.firebase

import android.util.Log
import com.ape.apps.sample.baypilot.data.sharedprefs.SharedPreferencesManager
import com.ape.apps.sample.baypilot.util.dlc.DeviceLockMessenger
import com.ape.apps.sample.baypilot.util.dlc.DeviceLockServiceProtocol
import com.ape.apps.sample.baypilot.util.worker.DatabaseSyncWorker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// Class to process messages from Firebase Cloud Messaging(FCM).
// Whenever a new FCM token is generated for the device(this will happen after factory reset) this class
// will register that token with the database to send future credit plan details updates to app.
class BayPilotFirebaseMessagingService : FirebaseMessagingService() {

  companion object {
    private const val TAG = "BayPilotFirebaseMsgService"

    // Key used by FCM to send SYNC command to app to fetch latest credit plan details from database.
    // This is send whenver there has been an update in database corresponding to device's IMEI entry.
    const val DLC_COMMAND = "dlcCommand"
  }

  // Stores connection to DLC.
  private lateinit var messenger: DeviceLockMessenger

  override fun onCreate() {
    super.onCreate()

    messenger = DeviceLockMessenger()
  }

  override fun onNewToken(token: String) {
    Log.d(TAG, "onNewToken() called with: token = $token")

    // Register this token in Database.
    sendRegistrationToServer(token)
  }

  // Process new messages send from FCM. We are sending Data Messages so they can received irrespective of
  // whether the app is in foreground or background.
  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    Log.d(TAG, "onMessageReceived() called with: remoteMessage = $remoteMessage")

    remoteMessage.data.let { data ->
      data[DLC_COMMAND]?.let { dlcCommand ->
        // Send LOCK and UNLOCK command to DLC according to message received.
        when (dlcCommand) {
          // TODO: Remove LOCK and UNLOCK. They were added just for testing we don't want to support forced locking/unlocking.
          "LOCK" -> {
            Log.d(TAG, "LOCK received")
            GlobalScope.launch(Dispatchers.IO) {
              messenger.bindAndSendMessage(applicationContext, DeviceLockMessenger.LOCK)
            }
          }
          "UNLOCK" -> {
            Log.d(TAG, "UNLOCK received")
            GlobalScope.launch(Dispatchers.IO) {
              messenger.bindAndSendMessage(applicationContext, DeviceLockMessenger.UNLOCK)
            }
          }
          "SYNC" -> {
            Log.d(TAG, "SYNC received. Fetching credit details from database")
            DatabaseSyncWorker.scheduleSync(applicationContext)
          }
          "RELEASE" -> {
            Log.d(TAG, "RELEASE received. Releasing the device from financed mode.")
            GlobalScope.launch(Dispatchers.IO) {
              messenger.bindToDlcAndWait(applicationContext)
              messenger.sendMessage(DeviceLockServiceProtocol.CLIENT_MSG_CLEAR_OWNER)
              val sharedPreferencesManager = SharedPreferencesManager(applicationContext)
              sharedPreferencesManager.markDeviceReleased()
            }
          }
          else -> Log.d(TAG, "Unknown Command = $dlcCommand")
        }
      }
    }
  }

  // Register FCM token with Database.
  private fun sendRegistrationToServer(token: String) {
    val sharedPreferencesManager = SharedPreferencesManager(applicationContext)

    // TODO: add empty IMEI handling.
    // Retrieve IMEI from sharedPreferences.
    val imei = sharedPreferencesManager.readIMEI()
    Log.d(TAG, "Setting Token for imei: $imei to $token")

    // Store the token in database under device IMEI.
    val databaseManager = FirebaseDatabaseManager()
    databaseManager.storeFcmToken(token, imei)
  }

}