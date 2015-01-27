# ma3map
 
ma3map is a [matatu](http://en.wikipedia.org/wiki/Matatu) transit application for Nairobi. It uses GTFS data collected by University of Nairobi's C4DLab available [here](http://www.gtfs-data-exchange.com/agency/university-of-nairobi-c4dlab/). For more information on the GTFS data refer to [Digital Matatus' website](http://www.digitalmatatus.com/). Visit ma3map's [GitHub Homepage](https://www.github.com/ma3map) for the entire codebase.

## Android Client [![Build Status](https://travis-ci.org/ma3map/ma3map-client_android.svg?branch=master)](https://travis-ci.org/ma3map/ma3map-client_android)

The android client was made using [Android Studio](https://developer.android.com/sdk/installing/studio.html) and the [Gradle Build System](http://www.gradle.org/).

### Building the Project

#### Environment

You need to sign the app for it to be allowed to use some of the external APIs it needs. If you don't already have a release key, generate one by running:

    cd ~/.android
    keytool -genkey -v -keystore release.keystore -alias androidreleasekey -keyalg RSA -keysize 2048 -validity 10000

You can show the release key's SHA1 fingerprint by running:

    cd ~/.android
    keytool -v -list -keystore release.keystore

Add the following lines to the local.properties file in the project's root directory (you might have to create it):

    STORE_FILE=/home/[username]/.android/release.keystore
    STORE_PASSWORD=your_key_store_pw
    KEY_ALIAS=androidreleasekey
    KEY_PASSWORD=your_release_key_pw

Make sure your Android SDK has the following installed:

 -  Android SDK Tools v23.0.5 > 
 -  Android SDK Platform-tools v21 >
 -  Android SDK Build-toos v21.0.2 >
 -  SDK platform for API level 21 >
 -  Google APIs for API level 21 >
 -  Android Support Repository v7 >
 -  Android Support Library v21 >
 -  Google Play Services v20 >

Run the following commands when you first clone the project:

    ./gradlew clean
    ./gradlew build --debug


#### Generating a Signed Release APK

Since this app uses API's that need it to be signed, we recommend you install the signed APK on your device. You can generate the signed APK by running:

    ./gradlew assembleRelease
    adb install -r app/build/outputs/apk/app-release.apk


### External APIs Used

The app needs access to several external APIs:

#### 1. Google Maps Android API v2

You'll need to get an API key from [Google's Developer Console](https://console.developers.google.com). Set the API key as *maps_api_key* in app/src/main/res/values/strings.xml.

Note that Google will need your release key's SHA1 fingerprint for them to give you an API key.


#### 2. Google Places API

You'll need to get an API key from [Google's Developer Console](https://console.developers.google.com). Set the API key as *places_api_key* in app/src/main/res/values/strings.xml. 

Google doesn't need your release key's SHA1 fingerprint. Use the Public API access key for this.


## LICENSE

This code is released under the [GNU Affero General Public License version 3](http://www.gnu.org/licenses/agpl-3.0.html). Please see the file LICENSE.md for details.

## Download

You can download the signed Android apk from [here](https://github.com/ma3map/ma3map-client_android/blob/master/app/build/outputs/apk/app-release.apk). Please note that this project is still under development and should be considered pre-alpha. The application currently only works on devices that run Android 5.0.1 and newer. Support for other versions of Android coming very soon!
