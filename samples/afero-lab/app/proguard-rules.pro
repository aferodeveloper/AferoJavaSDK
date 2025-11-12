# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/tony/androidsdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class io.kiban.hubby.** { *; }
-keep class io.afero.sdk.client.** { *; }
-keep class io.afero.sdk.client.afero.models.** { *; }
-keep class io.afero.sdk.conclave.** { *; }
-keep class io.afero.sdk.device.** { *; }
-keep class io.afero.client.** { *; }
-keep class io.afero.client.kenmore.models.** { *; }

# https://github.com/square/retrofit#proguard
# https://github.com/square/okhttp#proguard
-dontwarn okio.**
-dontwarn javax.annotation.**

# https://github.com/square/picasso#proguard
-dontwarn com.squareup.okhttp.**


# RxJava
# https://github.com/ReactiveX/RxJava/issues/1415
# https://github.com/ReactiveX/RxJava/issues/3097
-dontwarn sun.misc.Unsafe
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

# Retrofit2
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontwarn retrofit2.**
-dontnote retrofit2.Platform
# Platform used when running on RoboVM on iOS. Will not be used at runtime.
-dontnote retrofit2.Platform$IOS$MainThreadExecutor
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Jackson
# http://stackoverflow.com/questions/27687128/how-to-setup-proguard-for-jackson-json-processor
# Update for com.fasterxml package intead of org.codehaus
-keepattributes *Annotation*,EnclosingMethod,Signature
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**
-keep class com.fasterxml.** { *; }
-keepclassmembers public final enum com.fasterxml.jackson.annotate.JsonAutoDetect$Visibility {
public static final com.fasterxml.jackson.annotation.JsonAutoDetect$Visibility *; }
