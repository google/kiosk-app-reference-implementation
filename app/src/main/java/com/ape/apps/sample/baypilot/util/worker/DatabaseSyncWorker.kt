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

package com.ape.apps.sample.baypilot.util.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.ape.apps.sample.baypilot.R
import com.ape.apps.sample.baypilot.data.sharedprefs.SharedPreferencesManager
import com.ape.apps.sample.baypilot.ui.home.HomeActivity
import com.ape.apps.sample.baypilot.util.alarm.AlarmSetter
import com.ape.apps.sample.baypilot.util.date.DateTimeHelper
import com.ape.apps.sample.baypilot.util.dlc.DeviceLockMessenger
import com.ape.apps.sample.baypilot.util.firebase.FirebaseDatabaseManager
import com.ape.apps.sample.baypilot.util.network.InternetConnectivity

class DatabaseSyncWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

  companion object {
    private const val TAG = "BayPilotDatabaseSyncWorker"

    private const val CHANNEL_ID = "BayPilotNotification"
    private const val CHANNEL_NAME = "Due payment reminder"
    private const val CHANNEL_DESCRIPTION = "Shows reminder for due payments"

    // worker input keys
    const val WORK_ACTION = "ACTION"
    const val ALARM_DUE_DATE = "ALARM_DUE_DATE"

    // Actions which can be passed to this worker.
    const val ACTION_LOCK_DEVICE = "LOCK_DEVICE"
    const val ACTION_SEND_NOTIFICATION = "SEND_NOTIFICATION"
    const val ACTION_SYNC_TO_DATABASE = "SYNC_TO_DATABASE"

    fun scheduleSync(context: Context) {
      Log.d(TAG, "scheduleSync() called with: context")

      val inputData = workDataOf(
        WORK_ACTION to ACTION_SYNC_TO_DATABASE
      )
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      val uploadSyncRequest =
        OneTimeWorkRequestBuilder<DatabaseSyncWorker>().apply {
          setInputData(inputData)
          setConstraints(constraints)
        }.build()

      Log.d(TAG, "setting sync request")
      WorkManager
        .getInstance(context)
        .enqueue(uploadSyncRequest)
    }
  }

  override suspend fun doWork(): Result {
    val messenger = DeviceLockMessenger()

    val sharedPreferencesManager = SharedPreferencesManager(applicationContext)
    var deviceImei = sharedPreferencesManager.readIMEI()

    // TODO TEMP
    if (deviceImei == "") {
      deviceImei = "testimei"
    }

    val workAction = inputData.getString(WORK_ACTION)
    val setForDueDate = inputData.getString(ALARM_DUE_DATE)

    if (InternetConnectivity.isConnectedToInternet(applicationContext)) {
      Log.d(TAG, "Internet Connection Available. Syncing with Database for IMEI $deviceImei.")
      val databaseManager = FirebaseDatabaseManager()
      val creditPlanInfo = databaseManager.readCreditPlanInfoFromDatabase(applicationContext, deviceImei)

      creditPlanInfo?.let {
        sharedPreferencesManager.writeCreditPlan(creditPlanInfo)

        // If due date fetched from database is different from the one
        // for which LOCK/NOTIFICATION Alarm was set, then set new alarms.
        if (creditPlanInfo.dueDate != null && creditPlanInfo.dueDate != setForDueDate) {
          // Set Alarms
          Log.d(TAG, "Due Date shifted setting up new Alarms while $workAction")
          val alarmSetter = AlarmSetter(applicationContext)
          alarmSetter.setCreditAlarms(creditPlanInfo)
          // UNLOCK device if needed.
          if (DateTimeHelper.isDueDateInFuture(creditPlanInfo.dueDate ?: "")) {
            Log.d(TAG, "Due date is in future. Unlocking the device....")
            messenger.bindAndSendMessage(applicationContext, DeviceLockMessenger.UNLOCK)
          }
          return Result.success()
        }
      } ?: run {
        Log.d(TAG, "Received null CreditPlanInfo from database this shouldn't happen")
        return Result.retry()
      }
    } else {
      Log.d(TAG, "not connected to internet")
    }

    when (workAction) {
      ACTION_LOCK_DEVICE -> {
        Log.d(TAG, "Locking the device")
        messenger.bindAndSendMessage(applicationContext, DeviceLockMessenger.LOCK)
      }
      ACTION_SEND_NOTIFICATION -> {
        Log.d(TAG, "Sending reminder notification to pay installment")
        createNotificationChannel(applicationContext)
        sendNotification(applicationContext, setForDueDate)
      }
    }
    return Result.success()
  }

  private fun createNotificationChannel(context: Context) {
    // Create the NotificationChannel
    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
      description = CHANNEL_DESCRIPTION
    }
    // Register the channel with the system
    val notificationManager: NotificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
  }

  private fun sendNotification(context: Context, dueDate: String?) {
    val notificationMessage = if (dueDate.isNullOrEmpty()) {
      context.getString(R.string.general_notification_message)
    } else {
      val zonedDateTime = DateTimeHelper.getDeviceZonedDueDateTime(dueDate)
      val timeLeft = DateTimeHelper.getTimeDiff(context, zonedDateTime)
      val formattedDate = DateTimeHelper.formattedDateTime(zonedDateTime)
      context.getString(R.string._detailed_notification_message, timeLeft, formattedDate)
    }

    val intent = Intent(context, HomeActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
    val notificationTitle: String = context.getString(R.string.notification_title)
    val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(notificationTitle)
      .setContentText(notificationMessage)
      .setStyle(
        NotificationCompat.BigTextStyle().bigText(notificationMessage)
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
      // notificationId is a unique int for each notification that you must define
      notify(0, builder.build())
    }
  }

}