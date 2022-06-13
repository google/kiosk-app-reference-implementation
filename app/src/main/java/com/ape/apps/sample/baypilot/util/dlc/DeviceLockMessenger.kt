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

package com.ape.apps.sample.baypilot.util.dlc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.widget.Toast
import com.ape.apps.sample.baypilot.R
import com.ape.apps.sample.baypilot.data.sharedprefs.SharedPreferencesManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Class to connect to Device Lock Controller(DLC) and send LOCK/UNLOCK commands, get device status and imei.
class DeviceLockMessenger {

  companion object {
    private const val TAG = "BayPilotDeviceLockMessenger"

    const val LOCK = "LOCK"
    const val UNLOCK = "UNLOCK"
  }

  // Messenger for DLC connection.
  private var messenger: Messenger? = null

  // Used to check DLC connection status
  private var bound = false

  // Default Messenger to handle replies from DLC
  private var incomingMessenger: Messenger? = null

  // Bind to DLC and block thread while binding. Uses suspend Coroutine for Synchronization.
  suspend fun bindToDlcAndWait(context: Context) = suspendCoroutine<Boolean> { continuation ->
    Log.d(TAG, "bindToDlcAndWait() called with: context")

    // Default messenger to handle replies from DLC.
    incomingMessenger = Messenger(IncomingHandler(Looper.getMainLooper(), context))

    // Service connection object for connection between App and DLC.
    val connection: ServiceConnection = object : ServiceConnection {

      // Get Messenger for DLC Service connection.
      override fun onServiceConnected(className: ComponentName, service: IBinder) {
        Log.d(TAG, "onServiceConnected() called with: className, service")

        messenger = Messenger(service)
        bound = true

        // Resume the calling thread.
        continuation.resume(true)
      }

      // Reset messenger on service disconnection.
      override fun onServiceDisconnected(className: ComponentName) {
        messenger = null
        bound = false
      }
    }

    val serviceIntent = Intent(DeviceLockServiceProtocol.SERVICE_ACTION)
    serviceIntent.setClassName(
      DeviceLockServiceProtocol.SERVICE_PACKAGE_NAME,
      DeviceLockServiceProtocol.SERVICE_CLASS_NAME
    )

    // Bind to DLC and get ServiceConnection in connection object.
    context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
  }

  fun sendMessage(msgId: Int, replyMessenger: Messenger? = incomingMessenger) {
    Log.d(TAG, "sendMessage() called with: msgId = ${msgId.toDeviceLockServiceProtocol()}, replyMessenger")
    sendMessage(msgId, 0 /* arg1 */, replyMessenger)
  }

  private fun sendMessage(msgId: Int, arg1: Int, replyMessenger: Messenger?): Boolean {
    if (!bound) {
      Log.d(TAG, "Service is not bound. Returning")
      return false
    }

    Log.d(TAG, "Sending message ${msgId.toDeviceLockServiceProtocol()}")

    val msg = Message.obtain(null /* handler */, msgId, arg1, 0 /* arg2 */)
    msg.replyTo = replyMessenger

    try {
      messenger?.send(msg)
    } catch (e: RemoteException) {
      Log.e(TAG, "Failed to send message ${msgId.toDeviceLockServiceProtocol()}")
      return false
    }

    return true
  }

  private fun lockDevice() {
    sendMessage(DeviceLockServiceProtocol.CLIENT_MSG_LOCK_DEVICE)
    sendMessage(DeviceLockServiceProtocol.CLIENT_MSG_REGISTER_RELAUNCH_ON_DEATH)
  }

  private fun unlockDevice() {
    sendMessage(DeviceLockServiceProtocol.CLIENT_MSG_UNLOCK_DEVICE)
  }

  suspend fun bindAndSendMessage(context: Context, action: String) {
    if (!bound) {
      Log.d(TAG, "Binding to DLC")
      bindToDlcAndWait(context)
    }

    val replyMessenger = Messenger(LockUnlockHandler(Looper.getMainLooper(), action))
    sendMessage(DeviceLockServiceProtocol.CLIENT_MSG_IS_DEVICE_LOCKED, replyMessenger)
  }

  private class IncomingHandler(looper: Looper, val context: Context) : Handler(looper) {

    override fun handleMessage(msg: Message) {
      var text = ""

      when (msg.what) {
        DeviceLockServiceProtocol.CLIENT_MSG_IS_DEVICE_LOCKED -> {
          text = context.getString(
            if (msg.arg1 == 1) R.string.toast_device_locked else R.string.toast_device_unlocked
          )
        }
        DeviceLockServiceProtocol.CLIENT_MSG_QUERY_IMEI -> {
          val bundle = msg.data
          text = bundle.getString(DeviceLockServiceProtocol.KEY_IMEI) ?: ""

          val sharedPreferencesManager = SharedPreferencesManager(context)
          sharedPreferencesManager.writeIMEI(text)
        }
      }

      Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()
    }

  }

  // Checks current status of device.
  // action LOCK -> lock device if it is unlocked.
  // action UNLOCK -> unlock device if it is locked.
  // If device is already in asked state we don't do anything
  inner class LockUnlockHandler(looper: Looper, private val action: String) : Handler(looper) {

    override fun handleMessage(msg: Message) {
      when (msg.what) {
        DeviceLockServiceProtocol.CLIENT_MSG_IS_DEVICE_LOCKED -> {
          if (msg.arg1 == 1) {
            Log.d(TAG, "Device is currently locked")

            if (action == UNLOCK) {
              Log.d(TAG, "Unlocking device...")
              unlockDevice()
            }
          } else {
            Log.d(TAG, "Device is currently unlocked")

            if (action == LOCK) {
              Log.d(TAG, "Locking Device...")
              lockDevice()
            }
          }
        }
      }
    }

  }

}