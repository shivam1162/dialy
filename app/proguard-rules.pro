# Project specific ProGuard rules for Dialy

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room generated classes and DAOs
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.dialy.app.data.local.dao.** { *; }
-keep class com.dialy.app.data.local.entities.** { *; }

# Keep domain models & notification models
-keep class com.dialy.app.domain.model.** { *; }
-keep class com.dialy.app.core.auth.** { *; }
-keep class com.dialy.app.core.sync.** { *; }
-keep class com.dialy.app.core.notification.** { *; }

# Keep kotlinx.serialization classes
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Google API Client & Google Drive REST API
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep interface com.google.api.client.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.common.**
-dontwarn org.apache.http.**
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}

# Gson (used by Google Drive client)
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
