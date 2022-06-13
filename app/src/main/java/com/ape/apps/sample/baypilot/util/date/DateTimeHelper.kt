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

package com.ape.apps.sample.baypilot.util.date

import android.content.Context
import com.ape.apps.sample.baypilot.R
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.abs

// Object containing utility functions for DueDate.
object DateTimeHelper {

  // Convert dueDate string to same zone ZonedDateTime.
  private fun getZonedDateTimeForDueDate(dueDate: String): ZonedDateTime {
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss ZZ yyyy", Locale.ENGLISH)
    return ZonedDateTime.parse(dueDate, formatter)
  }

  // Convert dueDate string to ZonedDateTIme in device default zone.
  fun getDeviceZonedDueDateTime(dueDate: String): ZonedDateTime {
    val zonedDateTime = getZonedDateTimeForDueDate(dueDate)
    return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault())
  }

  // Checks if given dateTime is in future.
  fun isInFuture(dateTime: ZonedDateTime): Boolean {
    return dateTime.isAfter(ZonedDateTime.now(dateTime.zone))
  }

  // Converts dueDate to ZonedDateTime and checks if it is future.
  fun isDueDateInFuture(dueDate: String): Boolean {
    return isInFuture(getDeviceZonedDueDateTime(dueDate))
  }

  // Formats dueDate to string. Used to show Due Date in UI
  fun formattedDateTime(dueDateTime: ZonedDateTime): String {
    return dueDateTime.format(DateTimeFormatter.ofPattern("dd MMMM, hh:mm a"))
  }

  // Get time diff between given zonedDateTime and now till. Returns in hour if diff is less than 1 day.
  fun getTimeDiff(context: Context, zonedDueDateTime: ZonedDateTime): String {
    var remainingTime: Long = abs(ZonedDateTime.now().until(zonedDueDateTime, ChronoUnit.DAYS))
    var timeUnit = context.getString(R.string.days)
    // Get remaining time in hours if less than a day is left
    if (remainingTime == 0L) {
      remainingTime = abs(ZonedDateTime.now().until(zonedDueDateTime, ChronoUnit.HOURS))
      timeUnit = context.getString(R.string.hours)
    }
    return "$remainingTime $timeUnit"
  }

}