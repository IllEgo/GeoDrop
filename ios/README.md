# GeoDrop iOS

This directory contains the SwiftUI iOS client for GeoDrop. It mirrors the Android application's core functionality:

* Firebase Authentication-based sign-in and sign-up
* Group membership management and drop feed discovery
* Creating new drops (including optional image uploads and SafeSearch validation)
* Drop detail views with reporting and collection actions
* Business profile management and NSFW preferences
* Push notification token registration

## Prerequisites

1. Install Xcode 15 or newer.
2. Install CocoaPods (`sudo gem install cocoapods`).
3. Provide Firebase configuration by copying your iOS `GoogleService-Info.plist` into `ios/GeoDropIOS/GoogleService-Info.plist` (a placeholder is committed).
4. Add your Google Vision API key to the `GOOGLE_VISION_API_KEY` entry in `ios/GeoDropIOS/Info.plist`.

## Building

```sh
cd ios
pod install
open GeoDropIOS.xcworkspace
```

Build and run the `GeoDropIOS` scheme on an iOS 15+ simulator or device.

## Notes

* Location, camera, microphone, and photo-library permissions are configured in `Info.plist`.
* SafeSearch requests fall back to the existing Cloud Function if a Vision API key is not provided.
* The project targets iOS 15+ and uses SwiftUI throughout.