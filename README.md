# Kiosk app reference implementation

This is a reference implementation of DLC Kiosk app, intended to help Android
ecosystem partners (carriers, OEMs, SIs) understand how to integrate their own
Kiosk app with DLC APIs. It serves both as a sample Kiosk app and as a testing &
development application for DLC APIs. It supports devices running Android 11.0
and above.

### Getting started

-  You can directly import this repo into Android Studio or download/clone
    it and build using `gradle`
-  You will need an Android 12 device or a DLC compatible Android 11 device

You can find more information about DLC in the partner documentation at https://docs.partner.android.com/gms/building/integrating/device-lock & https://docs.partner.android.com/gms/building/integrating/device-lock/requirements

### Prepare

We have used Firebase cloud messaging to send lock / unlock commands to the
device, however this is entirely optional. You can implement your logic and use
your preferred service provider for push messaging.  
If you need to simply try out the Kiosk app reference implementation as is, you
need to configure the project as follows:

1. Create a new Firebase project at
    [https://console.firebase.google.com/](https://console.firebase.google.com/)
1.  Add an Android app in your project
    1. The default package name is `com.ape.apps.sample.baypilot`. If
        you are creating your own app, then you might need to refactor the
        package name in Android Studio. Enter the package name in the firebase console.
    1. Select a nickname of your choice.
    1. Register.
    1. Download the generated `google-services.json` file and copy it in
        your project folder as directed in Firebase console / documentation.
    1. Finish the remaining setup. Firebase sdk libraries have already
        been added to the projects.   

1. Turn on Anonymous authentication for your app in Firebase.

### Build

1. From the `bay-pilot` folder, build the kiosk app 

```
./gradlew clean build
```

This will build the apk and output it at
`app/build/outputs/apk/debug/app_debug.apk`

### Deploy

#### Using QR Code

Note: This method does not currently work on Android 12. You will need a
compatible Android 11 userdebug build.

1. Factory reset the device
1. Sideload the kiosk app and dev DLC apk on the device. Contact your Google
    poc for the dev DLC apk.

```
adb install app-debug.apk
adb install devicelock_dev.apk
```

1. Tap anywhere on the screen six times to start the QR code scanner
1. The following is a sample config, for a test QR code:

```
{
  'android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED': true,
  'android.app.extra.PROVISIONING_SKIP_EDUCATION_SCREENS': false,
  'android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE': {
    'com.google.android.apps.devicelock.CREDITOR_PACKAGE': 'com.ape.apps.sample.baypilot',
    'com.google.android.apps.devicelock.CREDITOR_SETUP_ACTIVITY': 'com.ape.apps.sample.baypilot/com.ape.apps.sample.baypilot.ui.welcome.WelcomeActivity',
    'com.google.android.apps.devicelock.CREDITOR_NAME': 'Bay Pilot Creditor',
    'com.google.android.apps.devicelock.CREDITOR_ALLOWLIST_0': 'com.android.chrome',
    'com.google.android.apps.devicelock.EXTRA_CREDITOR_READ_IMEI_ALLOWED': true
  }
}
```

QR Code:  
![image](docs/imgs/kioskapprefere--ugvo40yvr9l.png)

You can modify the config but make sure to re-generate the QR Code using any QR
Code tool of your choice,

1. Complete the Setup Wizard and you should see the kiosk app setup activity.

#### ZeroTouch

-  You need to sign an Android Enterprise ZeroTouch agreement to obtain
    access to the ZeroTouch portal. 
-  Once you have access to the ZeroTouch portal, you can enroll your
    device's IMEI and add the configuration in the portal. 
-  After that, factory reset the device and set up as usual to trigger DLC
    provisioning.

### Features

The sample Kiosk app demonstrates the following features:

1. Kiosk app Onboarding flow
1. Home screen, with device payment info, upcoming due date, locking status,
    a "pay now" button to redirect to FoP of choice, quick access to settings,
    dialer, refreshing the screen and getting support
1. Locking / Unlocking capabilities
1. Show Notifications per device lock guidelines

### Support

If you've found an error in this sample, please file an issue:
https://github.com/google/kiosk-app-reference-implementation/issues  
Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub.

### License

The code is published under Apache License 2.0. See LICENSE.md for details

### How to make contributions?

Please read and follow the steps in the CONTRIBUTING.md file.

### Disclaimer

This is not an official Google project. If you plan to incorporate the features
demonstrated, please carefully review the code and proceed at your own risk. 
