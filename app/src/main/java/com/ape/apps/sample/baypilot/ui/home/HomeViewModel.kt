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

package com.ape.apps.sample.baypilot.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.ape.apps.sample.baypilot.data.creditplan.CreditPlanInfo
import com.ape.apps.sample.baypilot.data.sharedprefs.SharedPreferencesManager
import com.ape.apps.sample.baypilot.util.sharedpreferences.SharedPreferenceIntLiveData
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

  companion object {
    private const val TAG = "BayPilotHomeViewModel"
  }

  private val _creditPlanInfo = MutableLiveData<CreditPlanInfo>()
  val creditPlanInfo: LiveData<CreditPlanInfo>
    get() = _creditPlanInfo

  fun observeCreditPlanInfo(context: Context) {
    Log.d(TAG, "observeCreditPlanInfo() called with: context")

    val sharedPreferencesManager = SharedPreferencesManager(context)

    viewModelScope.launch {
      SharedPreferenceIntLiveData(
        sharedPreferencesManager.sharedPreferences,
        SharedPreferencesManager.KEY_CREDIT_PLAN_SAVE_ID,
        0
      ).asFlow().collect {
        Log.d(TAG, "observeCreditPlanInfo() asFlow().collect called with: creditPlanSaveId = $it")

        _creditPlanInfo.value = sharedPreferencesManager.readCreditPlan()
      }
    }
  }

}