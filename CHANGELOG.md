Change Log
==========

Version 0.6.3 *(2017-08-??)*
----------------------------

 * Changed `afero-sdk-softhub` build to embed hubby library within the softhub .aar. This
   means apps no longer need to reference hubby in their dependencies and eliminates any
   possibility of a version mismatch between the SDK and apps.
 * New: AfLog now support the notion of `FilterLevel` so the volume of logging output can
   be managed.
 * New: `DeviceModel.writeAttribute()` call replaces all variants of `writeModelValue()`,
   which have now been deprecated.
 * Fix: `DeviceWifiSetup` will no longer hang if the device was already connected, and wifi
   setup state doesn't change.
 * JavaDoc added for `DeviceModel`.
 * Refactored parts of `ConclaveClient` and `DeviceWifiSetup` to remove deprecated RxJava calls.
 * `ConclaveDeviceEventSource.start` and `ConclaveDeviceEventSource.reconnect()` now return
    `Observable<ConclaveDeviceEventSource>`.

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
