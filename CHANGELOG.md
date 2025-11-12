Change Log
==========
Version 1.6.0 *(2025-11-06)*
----------------------------
* Update target version to 35 and minSDK to 21.
* Updates Hubby to 1.0.978

Version 1.5.4 *(2023-06-30)*
----------------------------
* Make schedule put public.

Version 1.5.3 *(2023-01-27)*
----------------------------
* Fixes access token refresh in sample app
* Schedule sample 

Version 1.5.2 *(2022-09-13)*
----------------------------
* New: Adds compact schedule days

Version 1.5.0 *(2022-02-24)*
----------------------------
 * New: Adds ability to configure the the underlying http client to enable oAuth to be handled by the app.
 * New: Adds ability to configure softhub to connect to different services

Version 1.4.5 *(2019-09-20)*
----------------------------
 * Fix: `AferoClient.resetPasswordWithCode` now actually works rather than returning a 400 error.
 * New: AferoLab sample app now has a basic "Forgot Password" flow.

Version 1.4.4 *(2019-09-09)*
----------------------------
 * Fix: Added logic to `ConclaveDeviceEventSource.stop()` to clear the access credentials and account id.
 * Removed: `ConclaveDeviceEventSource.setAccountId()` since this function is defunct.

Version 1.4.3 *(2019-08-22)*
----------------------------
 * Changed: Updated `AferoSofthub` to softhub version 1.0.844 to pick up latest fixes.

Version 1.4.2 *(2019-08-06)*
----------------------------
 * Changed: `AferoClient.internalRefreshAccessToken` is now protected instead of private.

Version 1.4.1 *(2019-08-05)*
----------------------------
 * New: Added a new `AferoClient.createAccount` overload that accepts `appId` and `platform` to enabled custom partner verification emails

Version 1.4.0 *(2019-07-26)*
----------------------------
 * Fix: Refactor attribute write metrics which had been inadvertently disabled.
 * Fix: Javadoc is once again published for the `android` and `softhub` modules.
 * New: Added `AferoClient` call `resendVerificationEmail`, to resend a verification token for a new account.

Version 1.3.0 *(2019-06-04)*
----------------------------
 * New: Added `AferoClient` calls for new password reset flow. See `AferoClient.sendPasswordRecoveryEmail` and `AferoClient.resetPasswordWithCode`.
 * New: Added `AferoClient` call to fetch a `DeviceProfile` for device prior to association. This is useful for display information for nearby devices that are in setup-mode for BLE based association. See `AferoClient.getDeviceProfilePreAssociation`.

Version 1.2.1 *(2019-05-23)*
----------------------------
 * Changed: `AferoSofthub` now defaults to enterprise mode.

Version 1.2.0 *(2019-05-23)*
----------------------------
 * Changed: Updated `AferoSofthub` to softhub version 1.0.840 to pick up latest fixes.
 * New: Added support for associating devices via BLE scan magic. Apps can now subscribe to `AferoSoftHub.observeSetupModeDevices()` to receive notification of a device that is available for association.
 * Changed: `AferoSofthub` now reports association failures to `Hubby`, allowing for proper recovery.

Version 1.1.0 *(2019-02-01)*
----------------------------
 * New: Added several new API calls to `AferoClient` (and `AferoClientRetrofit2`) to support creating accounts, rules, and account sharing.
 * New: Added `RuleCollection`, `DeviceRuleCollection`, and `RuleBuilder` utility classes.

Version 1.0.10 *(2019-01-09)*
----------------------------
 * Changed: Updated `AferoSofthub` to softhub version 1.0.792 to pick up latest fixes.

Version 1.0.9 *(2018-12-09)*
----------------------------
 * Changed: Updated `AferoSofthub` to softhub version 1.0.786 to pick up latest fixes.

Version 1.0.8 *(2018-10-3)*
----------------------------
 * Changed: Set the `AferoSofthub` `minSdkVersion` to 19 rather than 21.

Version 1.0.7 *(2018-09-7)*
----------------------------
 * Fix: `AferoSofthub` has added more rigorous null checks and synchronization around the start process.

Version 1.0.6 *(2018-08-21)*
----------------------------
 * Changed: `AferoSofthub` now exposes `HubType` so that partners can make use of `ENTERPRISE` mode should they require it.

Version 1.0.5 *(2018-07-23)*
----------------------------
 * Changed: Updated `AferoSofthub` to softhub version 1.0.756 to pick up latest fixes.

Version 1.0.4 *(2018-07-16)*
----------------------------
 * Changed: Updated `AferoSofthub` to softhub version 1.0.751 to pick up latest fixes.

Version 1.0.3 *(2018-07-10)*
----------------------------
 * Changed: Updated `AferoSofthub` to softhub version 1.0.750 to pick up latest fixes.

Version 1.0.2 *(2018-05-08)*
----------------------------
 * Fix: Changed `AferoClientRetrofit2` to prevent potential uncaught exception when refreshing access token.
 * Changed: `Attribute` length and default value fields are now exposed via getter/setter.
 * Fix: Removed some extraneous preceding slashes from some `AferoClientAPI` endpoints
 * Changed: Updated `AferoSofthub` to softhub version 674.
 * Fix: Conclave login now sends mobileDeviceId/clientId once again.

Version 1.0.1 *(2017-12-12)*
----------------------------
 * Fix: Removed reference to HttpException in MockAferoClient to fix Proguard IOException.
 * Fix: Improved concurrent stability in ConclaveClient.

Version 1.0.0 *(2017-11-27)*
----------------------------
 * New: Added persistent device tags API to `DeviceModel`
 * New: Added device tags sample code and UI to AferoLab
 * New: Added ability to add devices via QR code scan to AferoLab
 * New: Added device wifi setup sample code and UI to AferoLab
 * New: JavaDoc is now published to Artifactory
 * Fix: AferoSofthub now properly handles and reports errors that occur on startup

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
