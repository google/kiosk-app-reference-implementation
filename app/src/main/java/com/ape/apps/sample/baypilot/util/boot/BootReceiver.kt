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

package com.ape.apps.sample.baypilot.util.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ape.apps.sample.baypilot.data.sharedprefs.SharedPreferencesManager
import com.ape.apps.sample.baypilot.util.alarm.AlarmSetter
import com.ape.apps.sample.baypilot.util.worker.DatabaseSyncWorker

// Class to reset LOCK/Notification alarms after phone is restarted.
// When phone is restarted it loses all pending alarms so we need to Register for BootReceiver and set the alarms again.
class BootReceiver : BroadcastReceiver() {

  companion object {
    private const val TAG = "BayPilotBootReceiver"
  }

  override fun onReceive(context: Context, intent: Intent?) {
    // Get CreditPlanInfo stored locally in device in sharedPreferences.
    val sharedPreferencesManager = SharedPreferencesManager(context)
    val creditPlanInfo = sharedPreferencesManager.readCreditPlan() ?: return

    // Set Alarms
    Log.d(TAG, "Setting Alarms after boot")
    val alarmSetter = AlarmSetter(context)
    alarmSetter.setCreditAlarms(creditPlanInfo)

    // Schedule a sync to server when Internet connectivity available to make
    // sure we don't lose any update in case phone was switched on after long time.
    DatabaseSyncWorker.scheduleSync(context)
  }

}