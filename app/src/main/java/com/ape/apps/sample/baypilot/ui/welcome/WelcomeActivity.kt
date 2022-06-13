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

package com.ape.apps.sample.baypilot.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.ape.apps.sample.baypilot.R
import com.ape.apps.sample.baypilot.databinding.ActivityWelcomeBinding
import com.ape.apps.sample.baypilot.ui.home.HomeActivity
import com.ape.apps.sample.baypilot.util.network.InternetConnectivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class WelcomeActivity : AppCompatActivity() {

  companion object {
    private const val TAG = "BayPilotWelcomeActivity"
  }

  private lateinit var binding: ActivityWelcomeBinding
  private lateinit var auth: FirebaseAuth

  private val viewModel: WelcomeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    Log.d(TAG, "onCreate() called with: savedInstanceState")
    super.onCreate(savedInstanceState)

    auth = Firebase.auth

    binding = ActivityWelcomeBinding.inflate(layoutInflater)

    val view = binding.root
    setContentView(view)

    binding.buttonInternet.setOnClickListener {
      startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
    }
  }

  override fun onStart() {
    super.onStart()

    val currentUser = auth.currentUser
    if (currentUser == null) {
      Log.d(TAG, "Creating new Anonymous user")
      signIn()
    } else {
      Log.d(TAG, "Current firebase auth is not null. Checking if its still first run...")
      viewModel.checkFirstRun(applicationContext)

      Log.d(TAG, "Not first run. Starting Main Activity")
      val intent = Intent(this, HomeActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }
      startActivity(intent)
    }
  }

  private fun signIn() {
    // Try signing in Anonymously.
    auth.signInAnonymously()
      .addOnCompleteListener(this) { task ->
        if (task.isSuccessful) {
          // Sign in success
          Log.d(TAG, "signInAnonymously:success. Starting initial Setup after sign in success")
          viewModel.initialSetup(applicationContext)

          Log.d(TAG, "Starting Main Activity after sign in Success")
          val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
          }
          startActivity(intent)
        } else {
          // TODO: Add checks for specific errors.
          // If sign in failed, display a message to the user.
          Log.e(TAG, "signInAnonymously:failure", task.exception)
          Toast.makeText(
            baseContext, "Authentication failed." + task.exception,
            Toast.LENGTH_SHORT
          ).show()
          binding.textViewErrorMsg.setText(R.string.connect_to_internet_welcome)

          // Trying SignIn Again when internet connection is available.
          InternetConnectivity(this).observe(this) {
            signIn()
          }
        }
      }
  }

}