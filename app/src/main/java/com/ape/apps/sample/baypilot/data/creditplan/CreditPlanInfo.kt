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

package com.ape.apps.sample.baypilot.data.creditplan

import com.google.gson.Gson

enum class CreditPlanType {
  MONTHLY, WEEKLY, DAILY;

  companion object {
    fun toString(creditPlanType: CreditPlanType?): String? {
      return when (creditPlanType) {
        MONTHLY -> "MONTHLY"
        WEEKLY -> "WEEKLY"
        DAILY -> "DAILY"
        else -> null
      }
    }

    fun toCreditPlanType(planType: String?): CreditPlanType? {
      return when (planType) {
        "DAILY" -> DAILY
        "WEEKLY" -> WEEKLY
        "MONTHLY" -> MONTHLY
        else -> null
      }
    }
  }
}

// Class to store all details related to device credit plan.
// TODO: Currently it has only few essential members for testing, add others.
class CreditPlanInfo(
  val totalAmount: Int? = 0,
  val totalPaidAmount: Int? = 0,
  val dueDate: String? = null,
  val nextDueAmount: Int? = 0,
  val planType: CreditPlanType? = null
) {

  companion object {
    // Serialize a single object.
    fun serializeToJson(cpi: CreditPlanInfo?): String? {
      val gson = Gson()
      return gson.toJson(cpi)
    }

    // Deserialize to single object.
    fun deserializeFromJson(jsonString: String?): CreditPlanInfo? {
      val gson = Gson()
      return gson.fromJson(jsonString, CreditPlanInfo::class.java)
    }
  }

  fun toDebugString(): String {
    return "Credit Plan details: Due Date = $dueDate, Total device cost = $totalAmount, " +
            "Amount paid till now: $totalPaidAmount Next installment due = $nextDueAmount, Plan type = $planType"
  }

}