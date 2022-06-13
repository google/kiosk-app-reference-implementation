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

import android.content.Context
import android.util.Log
import com.ape.apps.sample.baypilot.data.FieldNames
import com.ape.apps.sample.baypilot.data.creditplan.CreditPlanInfo
import com.ape.apps.sample.baypilot.data.sharedprefs.SharedPreferencesManager
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

// Class to connect app to Realtime Database.
// Used to fetch credit plan details for device IMEI and store FCM token device in database.
class FirebaseDatabaseManager {

  companion object {
    private const val TAG = "BayPilotFirebaseDatabaseManager"
  }

  private val firebaseDatabase = FirebaseDatabase.getInstance()

  // Read credit plan details from Database.
  suspend fun readCreditPlanInfoFromDatabase(context: Context, deviceImei: String): CreditPlanInfo? {
    Log.d(TAG, "readCreditPlanInfoFromDatabase() called with: context, deviceImei = $deviceImei")

    return try {
      val result = firebaseDatabase.getReference(deviceImei).get().await()

      result?.child(FieldNames.CreditPlanInfo.plan)?.getValue(CreditPlanInfo::class.java)
    } catch (exception: Exception) {
      Log.d(
        TAG,
        "readCreditPlanInfoFromDatabase firebaseDatabase.getReference or result?.child called with exception = $exception"
      )

      SharedPreferencesManager(context).setCreditPlanInvalid()
      null
    }
  }

  // Store FCM token in database under device imei.
  fun storeFcmToken(token: String, imei: String) {
    val imeiRef = firebaseDatabase.getReference(imei)
    imeiRef.child(FieldNames.firebaseToken).setValue(token)
  }

}