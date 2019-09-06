---
author: Tony Myles
title: "AferoJavaSDK"
date: 2017-Jul-11
status: 0.6.0
---

# AferoJavaSDK

An SDK for interacting with the Afero service and peripheral platform.

## Getting Started
### Specifying the Repository

The SDK binaries are hosted privately on [JFrog](https://www.jfrog.com/artifactory/). To specify the repo use the following Gradle repository reference.

```Gradle
    repositories {
        maven {
            url "https://afero.jfrog.io/afero/afero-java-sdk"
            credentials {
                username artifactory_username
                password artifactory_password
            }
        }
    }
```

To specify the credentials, create a `gradle.properties` file either in `~/.gradle` (recommended) or in the root of your project, and insert the following:

```Gradle
    artifactory_username=my_username
    artifactory_password=my_password
```

If you have not been provided with an artifactory username and password, please contact Afero.

### Specifying the Modules

The SDK is composed of four separate modules.

The `afero-sdk-core` module is required for base functionality.
```Gradle
    compile 'io.afero.sdk:afero-sdk-core:0.6.0'
```

The `afero-sdk-client-retrofit2` module provides an optional implementation of the AferoClient REST API interface using [Retrofit2](http://square.github.io/retrofit/) and [okhttp3](http://square.github.io/okhttp/). If you choose not to include this module in your project, you will need to develop your own implementation of AferoClient using your preferred http client library.

```Gradle
    compile 'io.afero.sdk:afero-sdk-client-retrofit2:0.6.0'
```

The `afero-sdk-android` module is required for Android development.
```Gradle
    compile 'io.afero.sdk:afero-sdk-android:0.6.0'
```

The `afero-sdk-softhub` module and `hubby` binary are required for Afero soft hub functionality.
```Gradle
    compile 'io.afero.sdk:afero-sdk-softhub:0.6.0'
    compile 'io.afero.sdk:hubby:1.0.482@aar'
```

## LICENSE

  AFERO CONFIDENTIAL AND PROPRIETARY INFORMATION
  © Copyright 2017 Afero, Inc, All Rights Reserved.

  Any use and distribution of this software is subject to the terms
  of the License and Services Agreement between Afero, Inc. and licensee.

  SDK products contain certain trade secrets, patents, confidential and
  proprietary information of Afero.  Use, reproduction, disclosure
  and distribution by any means are prohibited, except pursuant to
  a written license from Afero. Use of copyright notice is
  precautionary and does not imply publication or disclosure.

  Restricted Rights Legend:
  Use, duplication, or disclosure by the Government is subject to
  restrictions as set forth in subparagraph (c)(1)(ii) of The
  Rights in Technical Data and Computer Software clause in DFARS
  252.227-7013 or subparagraphs (c)(1) and (2) of the Commercial
  Computer Software--Restricted Rights at 48 CFR 52.227-19, as
  applicable.


