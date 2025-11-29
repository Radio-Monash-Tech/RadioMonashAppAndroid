
-keep class com.radmon.radmonradio.data.network.** { *; }
-keep interface com.radmon.radmonradio.data.network.RadioApiService { *; }

-keep class com.radmon.radmonradio.data.model.** { *; }

-keepattributes Signature
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-dontwarn okio.**
-dontwarn javax.annotation.**

-keep class kotlin.coroutines.Continuation

-keep class com.radmon.radmonradio.RadioViewModel {
    public <init>(android.app.Application);
}
