# kotlinx.serialization: keep generated serializers for the API models
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.mydreamlinks.app.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mydreamlinks.app.data.**$$serializer { *; }
