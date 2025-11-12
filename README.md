---
author: Tony Myles
title: "AferoJavaSDK"
date: 2023-Jun-30
status: 1.5.4
---

# AferoJavaSDK

An SDK for interacting with the Afero service and peripheral platform.

This project adheres to the Contributor Covenant [code of conduct](CODE_OF_CONDUCT.md).
By participating, you are expected to uphold this code.

## Getting Started
### Specifying the Repository

The SDK binaries are hosted on [JFrog](https://www.jfrog.com/artifactory/). To specify the repo use the following Gradle repository reference.

```Gradle
    repositories {
        maven {
            url "https://afero.jfrog.io/afero/afero-java-sdk"
            artifactUrls "https://afero.jfrog.io/afero/hubby-android"
        }
    }
```

### Specifying the Modules

The SDK is composed of four separate modules.

The `afero-sdk-core` module is required for base functionality such as interacting with the Afero Cloud and manipulating devices.
```Gradle
    implementation 'io.afero.sdk:afero-sdk-core:1.5.3'
```

The `afero-sdk-client-retrofit2` module provides an optional implementation of the AferoClient REST API interface using [Retrofit2](http://square.github.io/retrofit/) and [okhttp3](http://square.github.io/okhttp/). If you choose not to include this module in your project, you will need to develop your own implementation of AferoClient using your preferred http client library.

```Gradle
    implementation 'io.afero.sdk:afero-sdk-client-retrofit2:1.5.3'
```

The `afero-sdk-android` module is required for Android development.
```Gradle
    implementation 'io.afero.sdk:afero-sdk-android:1.5.3'
```

The `afero-sdk-softhub` module is required for soft hub functionality on Android.
```Gradle
    implementation 'io.afero.sdk:afero-sdk-softhub:1.5.3'
    implementation "io.afero.sdk:hubby:1.0.978@aar"
```

## License

[MIT & ARM Permissive Binary](LICENSE)
