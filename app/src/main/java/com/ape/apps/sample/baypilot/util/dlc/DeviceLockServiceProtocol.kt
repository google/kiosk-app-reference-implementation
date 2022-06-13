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

import android.os.Bundle

object DeviceLockServiceProtocol {

  const val SERVICE_ACTION = "com.google.android.apps.devicelock.service.DeviceLockService.BIND"
  const val SERVICE_PACKAGE_NAME = "com.google.android.apps.devicelock"
  const val SERVICE_CLASS_NAME = "com.google.android.apps.devicelock.service.DeviceLockService"

  /**
   * The [Bundle] key client can use to read IMEI from the [Message] passed to it.
   *
   * @see .CLIENT_MSG_QUERY_IMEI
   *
   * @see Message.getData
   */
  const val KEY_IMEI = "imei"

  /*
 * Version used by client;
 * - arg1 : client version
 */
  const val CLIENT_MSG_VERSION = 1

  /*
 * Command to enter lock task mode
 */
  const val CLIENT_MSG_LOCK_DEVICE = 2

  /*
 * Command to leave lock task mode
 */
  const val CLIENT_MSG_UNLOCK_DEVICE = 3

  /*
 * Clear Device owner. This is irreversible.
 */
  const val CLIENT_MSG_CLEAR_OWNER = 4

  /**
   * Query if device is locked. Client needs to set [android.os.Message.replyTo] field to a
   * Messenger object. The DLC will send a response on this [android.os.Messenger] object with
   * the [android.os.Message.arg1] set to 1 if locked or 0 if not locked.
   */
  const val CLIENT_MSG_IS_DEVICE_LOCKED = 5

  /**
   * Client can register with DeviceLockService to be re-launched if the app gets killed. When the
   * device is in locked state, any crashes on the lock screen activity can cause the device to drop
   * out of locked state. Client should send this message by setting a [ ][android.os.Message.replyTo] messenger. DeviceLockService will monitor client death using [ ][android.os.IBinder.linkToDeath] and restart the activity if the client dies.
   */
  const val CLIENT_MSG_REGISTER_RELAUNCH_ON_DEATH = 6

  /**
   * The message client can use to query the IMEI of the device.
   *
   *
   * Client needs to set [android.os.Message.replyTo] field to a [ ] object. DLC will send a response on the [android.os.Messenger]
   * object with the IMEI being included in the [Bundle].
   *
   * @see .KEY_IMEI
   *
   * @see Message.setData
   * @see Message.getData
   */
  const val CLIENT_MSG_QUERY_IMEI = 7

  /*
 * Service version
 */
  const val SERVICE_VERSION_1 = 1

  const val CURRENT_SERVICE_VERSION = SERVICE_VERSION_1

}

fun Int.toDeviceLockServiceProtocol(): String = when {
  this == 1 -> "CLIENT_MSG_VERSION"
  this == 2 -> "CLIENT_MSG_LOCK_DEVICE"
  this == 3 -> "CLIENT_MSG_UNLOCK_DEVICE"
  this == 4 -> "CLIENT_MSG_CLEAR_OWNER"
  this == 5 -> "CLIENT_MSG_IS_DEVICE_LOCKED"
  this == 6 -> "CLIENT_MSG_REGISTER_RELAUNCH_ON_DEATH"
  this == 7 -> "CLIENT_MSG_QUERY_IMEI"
  else -> "Unknown DeviceLockServiceProtocol"
}