# Add project specific ProGuard rules here.

# Keep nodejs-mobile native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep NodeJS class
-keep class com.janeasystems.cdvnodejsmobile.NodeJS { *; }

# Keep PDS service classes
-keep class com.apiguave.pds.** { *; }
