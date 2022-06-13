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

package com.ape.apps.sample.baypilot.data.sharedprefs

import android.content.Context
import android.content.SharedPreferences
import com.ape.apps.sample.baypilot.data.creditplan.CreditPlanInfo
import com.ape.apps.sample.baypilot.data.creditplan.CreditPlanType

class SharedPreferencesManager(context: Context) {

  companion object {
    private const val TAG = "BayPilotSharedPreferencesManager"

    private const val SHARED_PREFS = "shared_prefs"

    private const val KEY_IS_FIRST_RUN = "is_first_run"

    private const val KEY_IMEI = "imei"

    private const val KEY_CREDIT_PLAN_SAVED = "credit_plan_saved"
    private const val KEY_VALID_CREDIT_PLAN = "valid_credit_plan"
    private const val KEY_DEVICE_RELEASED = "device_released"

    // Used to observe changes; creditPlanSaveId = lastCreditPlanSaveId + 1
    const val KEY_CREDIT_PLAN_SAVE_ID = "credit_plan_save_id"

    private const val KEY_TOTAL_AMOUNT = "total_amount"
    private const val KEY_PAID_AMOUNT = "paid_amount"
    private const val KEY_DUE_DATE = "due_date"
    private const val KEY_NEXT_DUE_AMOUNT = "next_due_amount"
    private const val KEY_PLAN_TYPE = "plan_type"
  }

  var sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)

  fun isFirstRun(): Boolean = sharedPreferences.getBoolean(KEY_IS_FIRST_RUN, true)

  fun writeFirstRun(firstRun: Boolean) = with(sharedPreferences.edit()) {
    putBoolean(KEY_IS_FIRST_RUN, firstRun)
    commit()
  }

  fun readIMEI(): String = sharedPreferences.getString(KEY_IMEI, "") ?: ""

  fun writeIMEI(imei: String) = with(sharedPreferences.edit()) {
    putString(KEY_IMEI, imei)
    commit()
  }

  fun isCreditPlanSaved(): Boolean = sharedPreferences.getBoolean(KEY_CREDIT_PLAN_SAVED, false)

  fun isValidCreditPlan(): Boolean = sharedPreferences.getBoolean(KEY_VALID_CREDIT_PLAN, false)

  fun isDeviceReleased(): Boolean = sharedPreferences.getBoolean(KEY_DEVICE_RELEASED, false)

  fun markDeviceReleased() = with(sharedPreferences.edit()) {
    incrementCreditPlanSaveId()
    putBoolean(KEY_DEVICE_RELEASED, true)
    commit()
  }

  fun setCreditPlanInvalid() = with(sharedPreferences.edit()) {
    incrementCreditPlanSaveId()
    putBoolean(KEY_CREDIT_PLAN_SAVED, true)
    putBoolean(KEY_VALID_CREDIT_PLAN, false)
    commit()
  }

  fun readCreditPlan(): CreditPlanInfo? {
    if (sharedPreferences.getBoolean(KEY_CREDIT_PLAN_SAVED, false).not()) return null
    if (sharedPreferences.getBoolean(KEY_VALID_CREDIT_PLAN, false).not()) return null

    return CreditPlanInfo(
      sharedPreferences.getInt(KEY_TOTAL_AMOUNT, 0),
      sharedPreferences.getInt(KEY_PAID_AMOUNT, 0),
      sharedPreferences.getString(KEY_DUE_DATE, ""),
      sharedPreferences.getInt(KEY_NEXT_DUE_AMOUNT, 0),
      CreditPlanType.toCreditPlanType(sharedPreferences.getString(KEY_PLAN_TYPE, ""))
    )
  }

  fun writeCreditPlan(creditPlanInfo: CreditPlanInfo) = with(sharedPreferences.edit()) {
    incrementCreditPlanSaveId()
    putBoolean(KEY_CREDIT_PLAN_SAVED, true)
    putBoolean(KEY_VALID_CREDIT_PLAN, true)
    putInt(KEY_TOTAL_AMOUNT, creditPlanInfo.totalAmount ?: 0)
    putInt(KEY_PAID_AMOUNT, creditPlanInfo.totalPaidAmount ?: 0)
    putString(KEY_DUE_DATE, creditPlanInfo.dueDate ?: "")
    putInt(KEY_NEXT_DUE_AMOUNT, creditPlanInfo.nextDueAmount ?: 0)
    putString(KEY_PLAN_TYPE, CreditPlanType.toString(creditPlanInfo.planType) ?: "")
    commit()
  }

  // Used to observe changes; creditPlanSaveId = lastCreditPlanSaveId + 1
  private fun SharedPreferences.Editor.incrementCreditPlanSaveId() {
    putInt(KEY_CREDIT_PLAN_SAVE_ID, sharedPreferences.getInt(KEY_CREDIT_PLAN_SAVE_ID, 0) + 1)
  }

}