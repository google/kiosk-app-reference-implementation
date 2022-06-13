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

package com.ape.apps.sample.baypilot.util.sharedpreferences

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

abstract class SharedPreferenceLiveData<T>(
  val sharedPrefs: SharedPreferences,
  private val key: String,
  private val defValue: T
) : LiveData<T>() {

  private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
    if (key == this.key) {
      value = getValueFromPreferences(key, defValue)
    }
  }

  abstract fun getValueFromPreferences(key: String, defValue: T): T

  override fun onActive() {
    super.onActive()

    value = getValueFromPreferences(key, defValue)
    sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
  }

  override fun onInactive() {
    sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    super.onInactive()
  }
}

class SharedPreferenceIntLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Int) :
  SharedPreferenceLiveData<Int>(sharedPrefs, key, defValue) {
  override fun getValueFromPreferences(key: String, defValue: Int): Int = sharedPrefs.getInt(key, defValue)
}
