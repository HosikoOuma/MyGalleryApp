# Add project specific ProGuard rules here.
# You can find general rules for popular libraries at
# https://github.com/consumer-proguard-rules/rules.

# Disabling optimization as it seems to be causing issues with cryptography.
#-dontoptimize

# Keep the data class used by Gson untouched
-keep class com.example.nkdsify.ui.utils.ViewedItem { *; }
-keepclassmembers class com.example.nkdsify.ui.utils.ViewedItem { *; }

# Keep the data class for tags backup
-keep class com.example.nkdsify.ui.dialogs.TagsBackup { *; }
-keepclassmembers class com.example.nkdsify.ui.dialogs.TagsBackup { *; }

# Keep Github update checker data classes
-keep class com.example.nkdsify.ui.utils.GithubRelease { *; }
-keepclassmembers class com.example.nkdsify.ui.utils.GithubRelease { *; }
-keep class com.example.nkdsify.ui.utils.GithubAsset { *; }
-keepclassmembers class com.example.nkdsify.ui.utils.GithubAsset { *; }
-keep interface com.example.nkdsify.ui.utils.GithubApiService { *; }

# Keep other data classes that might be used with Gson or reflection
-keep class com.example.nkdsify.data.** { *; }
-keepclassmembers class com.example.nkdsify.data.** { *; }

# Keep cryptography classes from being obfuscated
-keep class com.example.nkdsify.ui.utils.CryptoUtils { *; }
-keepclassmembers class com.example.nkdsify.ui.utils.CryptoUtils { *; }

# General rule for keeping data classes, which is good practice with ProGuard
-keepattributes Signature,InnerClasses,*Annotation*,EnclosingMethod
-keep class * extends java.lang.annotation.Annotation { *; }
-keepclassmembers,allowshrinking,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep class kotlin.Metadata { *; }

# Keep all security providers to prevent crypto errors like BadPaddingException
-keep class * extends java.security.Provider { *; }
-keep class java.security.** { *; }
-keep class javax.crypto.** { *; }
-dontwarn java.security.**
-dontwarn javax.crypto.**

# Rules for Coil
-dontwarn coil.**

# Rules for Retrofit, OkHttp, and Gson
# Conscrypt is the security provider. Keep it to prevent crypto errors.
-keep class org.conscrypt.** { *; }
-dontwarn org.conscrypt.**
-keepclassmembers class com.squareup.okhttp3.internal.publicsuffix.PublicSuffixDatabase { *; }
-dontwarn okio.**
-dontwarn retrofit2.Platform$Java8
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation


# Gson specific rules to preserve generic type information for TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep fields annotated with SerializedName
-keepclassmembers,allowoptimization class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Rules for Jetpack Compose
-keepclasseswithmembers,allowshrinking class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keepclassmembers,allowshrinking class * {
    @androidx.compose.runtime.Composable <fields>;
}
-keepclassmembers,allowshrinking class **.R$* {
    <fields>;
}

# Rules for Media3 (ExoPlayer)
-keep public class androidx.media3.common.Player$Listener {}
-keep public interface androidx.media3.common.Player$Listener
-keepclassmembers public class * extends androidx.media3.ui.PlayerView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclassmembers public class * extends androidx.media3.ui.PlayerControlView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
}

# Rules for testing libraries (JUnit, Espresso)
-dontwarn androidx.test.**
-dontwarn com.google.errorprone.annotations.**
-keep class androidx.test.runner.AndroidJUnitRunner { *; }

# Rules for image cropper libraries
-keep class com.canhub.cropper.CropImageActivity { *; }

# Remove all logging calls from release builds
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
