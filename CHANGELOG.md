## [2.1.2]

### Fixed
* [Android] Fixed possible compile issue with React Native versions older than 0.60.

## [2.1.1]

### Fixed

* [iOS] Fixed duplicate symbols for constants when using PhotoEditor SDK and VideoEditor SDK in the same project.
* [iOS] Fixed return `null` if the editor is dismissed without exporting the edited image.

## [2.1.0]

### Changed

* [iOS] Updated PhotoEditor SDK for iOS to version 10.7.0 and above.

### Fixed

* [iOS] Fixed automatic (CocoaPods) installation process so that PhotoEditor SDK and VideoEditor SDK can be used in the same project.
* [iOS] Fixed `FRAMEWORK_SEARCH_PATHS` for manual linking PhotoEditor SDK which is required for React Native versions older than 0.60.
* Add missing `Platform` import when using React Native versions older than 0.60.

## [2.0.1]

### Fixed

* [Android] Fixed error message: "tools: replace" attribute that is linked to the "provider" element type is not bound.

## [2.0.0]

### Added

* [Android] Added support for PhotoEditor SDK for Android version 7.1.5 and above.

## [1.3.0]

### Changed

* [iOS] Updated PhotoEditor SDK for iOS to version 10.6.0.

## [1.2.0]

### Changed

* [iOS] Updated PhotoEditor SDK for iOS to version 10.5.0.

## [1.1.0]

### Added

* [iOS] Updated PhotoEditor SDK for iOS to version 10.4.0.
* Added configuration options for personal stickers.

## [1.0.2]

### Fixed

* [iOS] Fixed `unlockWithLicense`.

## [1.0.0]

### Added

* [iOS] Initial release of the React Native module for PhotoEditor SDK. This version adds support for iOS only. Android support will be added in a later release.
