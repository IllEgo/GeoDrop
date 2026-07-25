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
4. Configure Firebase App Check for the iOS app. Media moderation uses the authenticated `safeSearch` callable; no Vision credential belongs in the client bundle.
5. Add your Google Maps SDK key to the `GOOGLE_MAPS_API_KEY` entry in `ios/GeoDropIOS/Info.plist`.
6. Copy the required build-time upper bounds from
   `GeoDropIOS/Config/FeatureFlags.xcconfig.example` into the release build
   configuration. Keep NSFW and hunts set to `NO` for the pilot.
7. Publish the reviewed root `remoteconfig.template.json`, then enable only the
   intended `pilot_*` parameters. Both the build value and Remote Config value
   must be true; missing or failed config remains disabled.

## Building

```sh
cd ios
pod install
open GeoDropIOS.xcworkspace
```

Build and run the `GeoDropIOS` scheme on an iOS 15+ simulator or device.

## Notes

* Location, camera, microphone, and photo-library permissions are configured in `Info.plist`.
* SafeSearch requests use the authenticated Cloud Function; no Vision API key is stored in the app.
* Firebase Remote Config is fetched on startup and listens for realtime updates
  so creation, notifications, coupons, and media can be stopped without a new
  binary.
* The project targets iOS 15+ and uses SwiftUI throughout.
