# Keep models for serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    *;
}
-keepclasseswithmembers class * {
    native <methods>;
}
