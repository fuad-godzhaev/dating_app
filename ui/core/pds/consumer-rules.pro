# Consumer proguard rules for PDS module

# Keep nodejs-mobile native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep NodeJS class
-keep class com.janeasystems.cdvnodejsmobile.NodeJS { *; }
