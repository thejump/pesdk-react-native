<p align="center">
  <a href="https://www.photoeditorsdk.com/?utm_campaign=Projects&utm_source=Github&utm_medium=PESDK&utm_content=React-Native">
    <img src="http://static.photoeditorsdk.com/logo.png" alt="PhotoEditor SDK Logo"/>
  </a>
</p>
<p align="center">
  <a href="https://npmjs.org/package/react-native-photoeditorsdk">
    <img src="https://img.shields.io/npm/v/react-native-photoeditorsdk.svg" alt="NPM version">
  </a>
  <a href="http://twitter.com/PhotoEditorSDK">
    <img src="https://img.shields.io/badge/twitter-@PhotoEditorSDK-blue.svg?style=flat" alt="Twitter">
  </a>
</p>

# React Native module for PhotoEditor SDK

## Getting started

Install the React Native module in your project as follows:

```sh
yarn add react-native-photoeditorsdk
```

### iOS

For React Native 0.60 and above [autolinking](https://github.com/react-native-community/cli/blob/master/docs/autolinking.md) is used and PhotoEditor SDK for iOS should be automatically installed:

```sh
cd ios && pod install && cd ..
```

and updated:

```sh
cd ios && pod update && cd ..
```

with CocoaPods.

For older React Native versions autolinking is not available and PhotoEditor SDK for iOS needs to be [manually integrated](https://docs.photoeditorsdk.com/guides/ios/v10/introduction/getting_started#manually) in your Xcode project if you don't use [CocoaPods to manage your dependencies](https://facebook.github.io/react-native/docs/0.59/integration-with-existing-apps#configuring-cocoapods-dependencies). Make sure to put `ImglyKit.framework` and `PhotoEditorSDK.framework` in the `ios/` directory of your project. Finally, [link the native dependencies](https://facebook.github.io/react-native/docs/0.59/linking-libraries-ios#step-2) with:

```sh
yarn react-native link
```

### Android

1. Because PhotoEditor SDK for Android is quite large, there is a high chance that you will need to enable [Multidex](https://developer.android.com/studio/build/multidex) for your project as follows:

   1. Open the `android/app/build.gradle` file (**not** `android/build.gradle`) and add these lines at the end:
      ```groovy
      android {
          defaultConfig {
              multiDexEnabled true
          }
      }
      dependencies {
          implementation 'androidx.multidex:multidex:2.0.1'
      }
      ```
   2. Open the `android/app/src/main/java/.../MainApplication.java` file and change the superclass of your `MainApplication` class from `Application` to `androidx.multidex.MultiDexApplication`, e.g.:
      ```java
      public class MainApplication extends androidx.multidex.MultiDexApplication implements ReactApplication {
      ```

2. Add the img.ly repository and plugin by opening the `android/build.gradle` file (**not** `android/app/build.gradle`) and adding these lines at the top:
   ```groovy
   buildscript {
       repositories {
           jcenter()
           maven { url "https://plugins.gradle.org/m2/" }
           maven { url "https://artifactory.img.ly/artifactory/imgly" }
       }
       dependencies {
           classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.61"
           classpath 'ly.img.android.sdk:plugin:7.1.5'
       }
   }
   ```
   In order to update PhotoEditor SDK for Android replace the version string `7.1.5` with a [newer release](https://github.com/imgly/pesdk-android-demo/releases).

3. Configure PhotoEditor SDK for Android by opening the `android/app/build.gradle` file  (**not** `android/build.gradle`) and adding the following lines under `apply plugin: "com.android.application"`:
   ```groovy
   apply plugin: 'ly.img.android.sdk'
   apply plugin: 'kotlin-android'

   // Comment out the modules you don't need, to save size.
   imglyConfig {
       modules {
           include 'ui:text'
           include 'ui:focus'
           include 'ui:frame'
           include 'ui:brush'
           include 'ui:filter'
           include 'ui:sticker'
           include 'ui:overlay'
           include 'ui:transform'
           include 'ui:adjustment'
           include 'ui:text-design'

           // This module is big, remove the serializer if you don't need that feature.
           include 'backend:serializer'

           // Remove the asset packs you don't need, these are also big in size.
           include 'assets:font-basic'
           include 'assets:frame-basic'
           include 'assets:filter-basic'
           include 'assets:overlay-basic'
           include 'assets:sticker-shapes'
           include 'assets:sticker-emoticons'
       }
   }
   ```

### Usage

Import the module in your `App.js`:

```js
import {PESDK, Configuration} from 'react-native-photoeditorsdk';
```

Unlock PhotoEditor SDK with a license file:

```js
PESDK.unlockWithLicense(require('./pesdk_license'));
```

Open the editor with an image:

```js
PESDK.openEditor(require('./image.jpg'));
```

Please see the [code documentation](./index.d.ts) for more details and additional [customization and configuration options](./configuration.ts).

## Example

Please see our [example project](https://github.com/imgly/pesdk-react-native-demo) which demonstrates how to use the React Native module for PhotoEditor SDK.

## License Terms

Make sure you have a [commercial license](https://account.photoeditorsdk.com/pricing?utm_campaign=Projects&utm_source=Github&utm_medium=PESDK&utm_content=React-Native) for PhotoEditor SDK before releasing your app.
A commercial license is required for any app or service that has any form of monetization: This includes free apps with in-app purchases or ad supported applications. Please contact us if you want to purchase the commercial license.

## Support and License

Use our [service desk](http://support.photoeditorsdk.com) for bug reports or support requests. To request a commercial license, please use the [license request form](https://account.photoeditorsdk.com/pricing?utm_campaign=Projects&utm_source=Github&utm_medium=PESDK&utm_content=React-Native) on our website.
