Change Log
==========

Version 0.8.0 *(2017-09-27)*
----------------------------
 * New: Added `DeviceModel.setTimeZone(TimeZone)` and `getTimeZone()`
 * New: Added device inspector and attribute editor views to `AferoLab` sample app
 * Changed `OfflineScheduler` to use device local time for all event rather than UTC.
 * Changed `DeviceModel` to add the ability to migrate device data should attribute formats change.
 * Added `DeviceDataMigrator` to migrate `OfflineScheduleEvents` from UTC to device local time.
 * Changed `AttributeWriter` logic to send write requests in batches of 5 to prevent too
   many concurrent writes to devices.
 * Fix: `AttributeWriter` now handles several error conditions more gracefully.
 * Fix: `DeviceProfile.RangeOptions.getCount()` returns the correct count instead of `count - 1`.
 * Changed `ConclaveAccessManager` to use a different endpoint to fetch the Conclave token.
   The new endpoint doesn't require a mobileDeviceId as did the old one.
 * Changed `AferoSofthub.acquireInstance` to accept an `appInfo` string that can be used
   to attach app specific information to the softhub instance for debugging purposes.
 * Changed `DeviceCollection` to no longer require a `clientId` to be passed in.

Version 0.7.1 *(2017-08-25)*
----------------------------

 * New: Added DeviceProfile.setDeviceTypeId() and getDeviceTypeId()
 * New: Added DeviceInfoBody.appId to support specifying the Android application id for notifications.

Version 0.7.0 *(2017-08-18)*
----------------------------

 * New: Generated JavaDoc
 * Fix: Minor change to OTAInfo.getProgress() logic to return 0% at offset 0 of partition 2
   instead of 100%, and to be consistent with iOS/Swift.

Version 0.6.3 *(2017-08-15)*
----------------------------

 * Changed `afero-sdk-softhub` build to embed hubby library within the softhub .aar. This
   means apps no longer need to reference hubby in their dependencies and eliminates any
   possibility of a version mismatch between the SDK and apps.
 * New: JavaDoc for `DeviceModel`.
 * New: `DeviceModel.writeAttributes()` call replaces all variants of `writeModelValue()`,
   which have now been deprecated.
 * New: `DeviceModel.getOTAProgress()` has been refactored to return on `rx.Observable`
   that emits OTA progress.
 * `DeviceModel.setLocation(Location)` now performs the appropriate AferoClient call and
   updates the DeviceModel's LocationState.
 * New: `AferoClient.putDeviceLocation(String deviceId, Location location)` added to core.
 * New: `DeviceRules` model class.
 * Fix: `DeviceWifiSetup` sending credentials will no longer fail to complete if the device
   was already connected, and wifi setup state doesn't change.
 * Refactored parts of `ConclaveClient` and `DeviceWifiSetup` to remove deprecated RxJava calls.
 * `ConclaveDeviceEventSource.start` and `ConclaveDeviceEventSource.reconnect()` now return
   `Observable<ConclaveDeviceEventSource>`.
 * Removed `ControlModel` interface.
 * `DeviceModel.setProfile()` is now package-private.
 * `DeviceModel.State` enum has been changed to `DeviceModel.UpdateState`.
 * `DeviceModel.getLocationState` now returns an `rx.Observable`. If the `DeviceModel`
   contains a valid location, it is returned immediately, otherwise the location is
   fetched from the Afero Cloud.
 * `DeviceModel.setAvailable(boolean)` has been removed.

Version 0.6.2 *(2017-08-2)*
----------------------------

 * Hotfix to use Hubby 1.0.495 for internal purposes.

Version 0.6.1 *(2017-07-27)*
----------------------------

 * Refactored `DeviceCollection.start()` to return `rx.Observable<DeviceCollection>`.
   When this observable completes, the `DeviceCollection` is fully populated with `DeviceModel`
   objects associated with the active account.
 * New: `AferoClient.getDevicesWithState()` interface call added
 * `AferoClientRetrofit2` Changes:
     * New: `getDevicesWithState()` implementation added.
     * New: JavaDoc added for all public functions
     * `ConfigBuilder.clientId()` renamed to `oauthClientId()`
     * `ConfigBuilder.clientSecret()` renamed to `oauthClientSecret()`
     * `getAccessToken(String user, String password, String grantType)`
       deprecated in favor of `getAccessToken(String user, String password)`
     * `refreshAccessToken(String refreshToken, String grantType)` is
       deprecated and will be removed in the next minor release.
     * `getLocale()` is now protected
     * `RetryOnError` is now private
 * AferoLab sample app updated to accomodate above changes.

Version 0.6.0 *(2017-07-12)*
----------------------------

Initial release.
