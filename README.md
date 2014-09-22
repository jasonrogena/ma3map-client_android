# ma3map

This is my atttempt in putting [Nairobi matatu transit route data](http://www.gtfs-data-exchange.com/agency/university-of-nairobi-c4dlab/) by [Digital Matatus](http://www.digitalmatatus.com/) to good use


## Android Client

The android client was made using [Android Studio](https://developer.android.com/sdk/installing/studio.html) and the [Gradle Build System](http://www.gradle.org/).
To correctly run this project, do:

    ./gradlew clean
    ./gradlew build --debug

### Android Maps API

This application uses the Android Maps API. Inorder to use this API you need an API key. This [link](https://developers.google.com/maps/documentation/android/start) explains how to get one. Generate your signing keys by running the following commands:

    cd ~/.android
    keytool -genkey -v -keystore release.keystore -alias androidreleasekey -keyalg RSA -keysize 2048 -validity 10000
    
Show the SHA1 fingerprint for your key by running:

    cd ~/.android
    keytool -v -list -keystore release.keystore
