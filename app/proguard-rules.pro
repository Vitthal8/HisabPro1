# HisabPro Production ProGuard & R8 Rules

# Keep androidx.annotation.Keep annotated elements
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Room Database
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep Room entity, domain, and data models
-keep class com.hisabpro.app.data.local.entity.** { *; }
-keep class com.hisabpro.app.data.model.** { *; }
-keep class com.hisabpro.app.data.backup.** { *; }
-keep class com.hisabpro.app.data.sync.** { *; }
-keep class com.hisabpro.app.domain.subscription.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Google Mobile Ads (AdMob) & User Messaging Platform (UMP)
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep public class com.google.ads.** {
   public *;
}
-keep public class com.google.android.ump.** {
   public *;
}

# Google Play Billing 6+ / 7+
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.api.**

# ZXing Core
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Coroutines and WorkManager
-keep class kotlinx.coroutines.** { *; }
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Preserve Line Numbers for Crash Reporting
-keepattributes SourceFile,LineNumberTable
