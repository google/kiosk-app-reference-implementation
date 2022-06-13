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

package com.ape.apps.sample.baypilot.util.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ape.apps.sample.baypilot.util.worker.DatabaseSyncWorker

class AlarmReceiver : BroadcastReceiver() {

  companion object {
    private const val TAG = "BayPilotAlarmReceiver"

    const val EXTRA_REQUEST_CODE = "requestCode"
    const val EXTRA_DUE_DATE = "DUE_DATE"

    const val ACTION_LOCK = "LOCK"
    const val ACTION_NOTIFICATION = "NOTIFICATION"
  }

  override fun onReceive(context: Context, intent: Intent) {
    Log.d(
      TAG, "Lock onReceive() called with: action ${intent.action} with " +
              "${intent.getIntExtra(EXTRA_REQUEST_CODE, 0)}"
    )

    val dueDate = intent.getStringExtra(EXTRA_DUE_DATE)
    val workAction = when (intent.action) {
      ACTION_LOCK -> DatabaseSyncWorker.ACTION_LOCK_DEVICE
      ACTION_NOTIFICATION -> DatabaseSyncWorker.ACTION_SEND_NOTIFICATION
      else -> return
    }

    startWorker(context, workAction, dueDate ?: "")
  }

  private fun startWorker(context: Context, workAction: String, dueDate: String) {
    val inputData = workDataOf(
      DatabaseSyncWorker.WORK_ACTION to workAction,
      DatabaseSyncWorker.ALARM_DUE_DATE to dueDate
    )
    val uploadWorkRequest =
      OneTimeWorkRequestBuilder<DatabaseSyncWorker>().apply {
        setInputData(inputData)
//                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
      }.build()

    WorkManager
      .getInstance(context)
      .enqueue(uploadWorkRequest)
  }

}