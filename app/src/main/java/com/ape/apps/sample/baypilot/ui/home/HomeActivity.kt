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

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.ape.apps.sample.baypilot.R
import com.ape.apps.sample.baypilot.data.creditplan.CreditPlanInfo
import com.ape.apps.sample.baypilot.data.sharedprefs.SharedPreferencesManager
import com.ape.apps.sample.baypilot.databinding.ActivityHomeBinding
import com.ape.apps.sample.baypilot.util.date.DateTimeHelper
import com.ape.apps.sample.baypilot.util.extension.setTint
import com.ape.apps.sample.baypilot.util.network.InternetConnectivity
import com.ape.apps.sample.baypilot.util.network.InternetStatus
import com.ape.apps.sample.baypilot.util.worker.DatabaseSyncWorker

class HomeActivity : AppCompatActivity() {

  companion object {
    private const val TAG = "BayPilotHomeActivity"
  }

  private lateinit var binding: ActivityHomeBinding

  private val viewModel: HomeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    Log.d(TAG, "onCreate() called with: savedInstanceState")
    super.onCreate(savedInstanceState)

    binding = ActivityHomeBinding.inflate(layoutInflater)

    val view = binding.root
    setContentView(view)

    binding.buttonSettings.setOnClickListener {
      startActivity(Intent(Settings.ACTION_SETTINGS))
    }

    binding.buttonInternetSetting.setOnClickListener {
      startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
    }

    binding.buttonMakeACall.setOnClickListener {
      startActivity(Intent(Intent.ACTION_DIAL))
    }

    binding.buttonRefresh.setOnClickListener {
      // Check is device is online.
      if (InternetConnectivity.isConnectedToInternet(applicationContext)) {
        // Schedule Database Sync using WorkManager
        DatabaseSyncWorker.scheduleSync(applicationContext)
      } else {
        // Display message to connect to internet for refresh.
        Toast.makeText(
          baseContext, R.string.connect_to_internet_welcome,
          Toast.LENGTH_SHORT
        ).show()
      }
    }

    // Observe changes to credit plan details stored in device locally in sharedPreferences.
    viewModel.creditPlanInfo.observe(this) {
      updateUI(it)
    }

    viewModel.observeCreditPlanInfo(applicationContext)

    InternetConnectivity(this@HomeActivity).observe(this) {
      binding.buttonInternetSetting.isVisible = it == InternetStatus.OFFLINE
      binding.textViewInternetStatus.isVisible = it == InternetStatus.OFFLINE
    }
  }

  // Update UI accordingly to LOCK status of device.
  private fun updateUI(creditPlanInfo: CreditPlanInfo?) {
    Log.d(TAG, "updateUI() called with: creditPlanInfo = $creditPlanInfo")

    binding.relativeDeviceReleased.isVisible = false
    binding.relativeLayoutLoading.isVisible = false
    binding.relativeLayoutMissingImei.isVisible = false

    val sharedPreferencesManager = SharedPreferencesManager(applicationContext)

    if (sharedPreferencesManager.isDeviceReleased()) {
      Log.d(TAG, "updateUI() called with: sharedPreferencesManager.isDeviceReleased() = true")
      binding.relativeDeviceReleased.isVisible = true
      return
    }

    if (sharedPreferencesManager.isCreditPlanSaved().not()) {
      Log.d(TAG, "updateUI() called with: sharedPreferencesManager.isCreditPlanSaved() = false")
      binding.relativeLayoutLoading.isVisible = true
      return
    }

    if (sharedPreferencesManager.isValidCreditPlan().not()) {
      Log.d(TAG, "updateUI() called with: sharedPreferencesManager.isValidCreditPlan() = false")
      binding.relativeLayoutMissingImei.isVisible = true
      return
    }

    if (creditPlanInfo == null) {
      Log.d(TAG, "updateUI() called with: creditPlanInfo = null. Returning")
      return
    }

    // Get Credit Plan details.
    val totalAmount = creditPlanInfo.totalAmount ?: 0
    val nextDueAmount = creditPlanInfo.nextDueAmount ?: 0
    val totalPaidAmount: Int = creditPlanInfo.totalPaidAmount ?: 0
    val totalDueAmount: Int = totalAmount - totalPaidAmount

    // Populate Ui components with credit plan details.
    binding.textViewTotalCost.text = getString(R.string.total_cost, totalAmount)
    binding.textViewPaid.text = getString(R.string.paid_so_far, totalPaidAmount)
    binding.textViewRemaining.text = getString(R.string.remaining, (totalAmount - totalPaidAmount))

    binding.progressIndicatorOwedNow.progress = (totalDueAmount.toDouble() / totalAmount.toDouble() * 100).toInt()
    binding.progressIndicatorPaid.progress = (totalPaidAmount.toDouble() / totalAmount.toDouble() * 100).toInt()

    // Get next due date.
    val dueDateTime = DateTimeHelper.getDeviceZonedDueDateTime(creditPlanInfo.dueDate ?: "")
    val timeLeft = DateTimeHelper.getTimeDiff(applicationContext, dueDateTime)
    // Get formatted due date to display in UI.
    val formattedDueDateTime = DateTimeHelper.formattedDateTime(dueDateTime)

    // Device must be unlocked if due date is in future.
    if (DateTimeHelper.isInFuture(dueDateTime)) {
      // Show unlocked device UI
      unlockUI(nextDueAmount, timeLeft, formattedDueDateTime)
    } else {
      // Show locked device UI
      lockUI(nextDueAmount, timeLeft, formattedDueDateTime)
    }
  }

  private fun unlockUI(nextDueAmount: Int, remainingTime: String, formattedDate: String) {
    binding.textViewNextPayment.text = getString(R.string.installment_due_on, nextDueAmount, formattedDate)
    binding.textViewPaymentDue.text = getString(R.string.due_in, nextDueAmount, remainingTime)

    // If due date is in less than an hour set background color to red.
    if (remainingTime == "0 hours") {
      binding.frameLayoutPaymentDue.setBackgroundColor(ContextCompat.getColor(this@HomeActivity, R.color.red))
    } else {
      binding.frameLayoutPaymentDue.setBackgroundColor(ContextCompat.getColor(this@HomeActivity, R.color.green))
    }
    binding.imageViewDeviceStatus.setTint(R.color.green)
    binding.textViewDeviceStatus.text = getString(R.string.device_unlocked)
  }

  private fun lockUI(nextDueAmount: Int, remainingTime: String, formattedDate: String) {
    binding.textViewNextPayment.text = getString(R.string.installment_was_due_on, nextDueAmount, formattedDate)
    binding.textViewPaymentDue.text = getString(R.string.overdue_by, nextDueAmount, remainingTime)
    binding.frameLayoutPaymentDue.setBackgroundColor(ContextCompat.getColor(this@HomeActivity, R.color.red))
    binding.imageViewDeviceStatus.setTint(R.color.red)
    binding.textViewDeviceStatus.text = getString(R.string.device_locked_payment_overdue)
  }

}