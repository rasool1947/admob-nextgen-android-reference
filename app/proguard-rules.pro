# Project-specific R8 rules.
# The Ads SDK, UMP, Firebase, Koin, Navigation and AndroidX ship their own consumer rules,
# and view/fragment classes referenced from XML are kept automatically by AAPT.

# Keep line numbers so release crash reports (Crashlytics / Play Console) are readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
