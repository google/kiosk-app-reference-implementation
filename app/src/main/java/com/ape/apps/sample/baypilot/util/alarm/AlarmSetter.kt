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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ape.apps.sample.baypilot.data.creditplan.CreditPlanInfo
import com.ape.apps.sample.baypilot.data.creditplan.CreditPlanType
import com.ape.apps.sample.baypilot.util.date.DateTimeHelper
import java.time.ZonedDateTime

class AlarmSetter(private val context: Context) {

  companion object {
    private const val TAG = "BayPilotAlarmSetter"

    private const val MAX_NOTIFICATION_ALARMS = 3
  }

  private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

  fun setCreditAlarms(creditPlanInfo: CreditPlanInfo) {
    Log.d(TAG, "setCreditAlarms() for ${creditPlanInfo.toDebugString()}")

    val dueDateString = creditPlanInfo.dueDate ?: ""
    val dueDateTime = DateTimeHelper.getDeviceZonedDueDateTime(dueDateString)
    setAlarm(AlarmReceiver.ACTION_LOCK, dueDateTime, dueDateString, 0)
    setNotificationAlarms(dueDateTime, dueDateString, creditPlanInfo.planType ?: CreditPlanType.MONTHLY)
  }

  private fun setNotificationAlarms(dueDateTime: ZonedDateTime, dueDateString: String, creditPlanType: CreditPlanType) {
    val notificationDues = getNotificationsSchedule(dueDateTime, creditPlanType)
    // Cancel pending alarms which won't be overwritten.
    var alarmNumber = MAX_NOTIFICATION_ALARMS
    while (alarmNumber > notificationDues.size) {
      alarmManager.cancel(getPendingIntent(AlarmReceiver.ACTION_NOTIFICATION, dueDateString, alarmNumber - 1))
      alarmNumber--
    }
    notificationDues.forEachIndexed { requestNumber, notificationDateTime ->
      setAlarm(AlarmReceiver.ACTION_NOTIFICATION, notificationDateTime, dueDateString, requestNumber)
    }
  }

  // Notification requirements for Kiosk App
  // https://docs.partner.android.com/gms/building/integrating/device-lock/requirements#unlocked-reqs
  private fun getNotificationsSchedule(
    dueDateTime: ZonedDateTime,
    creditPlanType: CreditPlanType
  ): MutableList<ZonedDateTime> {
    val notificationDues = mutableListOf<ZonedDateTime>()
    when (creditPlanType) {
      CreditPlanType.MONTHLY -> {
        notificationDues.add(dueDateTime.minusDays(7))
        notificationDues.add(dueDateTime.minusDays(3))
        notificationDues.add(dueDateTime.minusDays(1))
      }
      CreditPlanType.WEEKLY -> {
        notificationDues.add(dueDateTime.minusDays(3))
        notificationDues.add(dueDateTime.minusDays(1))
        notificationDues.add(dueDateTime.minusHours(3))
      }
      CreditPlanType.DAILY -> {
        notificationDues.add(dueDateTime.minusHours(3))
        notificationDues.add(dueDateTime.minusHours(1))
      }
    }
    // Remove past due reminder notifications.
    notificationDues.retainAll { date -> DateTimeHelper.isInFuture(date) }
    return notificationDues
  }

  private fun setAlarm(alarmAction: String, time: ZonedDateTime, dueDateString: String, alarmRequestCode: Int) {
    val timeInMilliseconds: Long = time.toOffsetDateTime().toInstant().toEpochMilli()
    Log.d(
      TAG,
      "$alarmAction $alarmRequestCode Alarm set for $timeInMilliseconds, it will go after" +
              "${timeInMilliseconds - System.currentTimeMillis()} ms"
    )
    val pendingIntent = getPendingIntent(alarmAction, dueDateString, alarmRequestCode)
    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMilliseconds, pendingIntent)
  }

  private fun getPendingIntent(alarmAction: String, dueDateString: String, alarmRequestCode: Int): PendingIntent {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
      action = alarmAction
    }.putExtra(AlarmReceiver.EXTRA_REQUEST_CODE, alarmRequestCode)
      .putExtra(AlarmReceiver.EXTRA_DUE_DATE, dueDateString)
    return PendingIntent.getBroadcast(context, alarmRequestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT)
  }

}
