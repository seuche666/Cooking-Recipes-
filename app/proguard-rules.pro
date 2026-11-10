# --- R8/ProGuard-Regeln für Release-Builds ---

# Zeilennummern für lesbare Stacktraces behalten, Quelldateinamen verstecken
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization: Backup-Modelle samt generierter Serializer behalten,
# damit das Backup-Format (JSON-Feldnamen) nach dem Shrinking stabil bleibt.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class de.rezeptbuch.app.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class de.rezeptbuch.app.data.backup.model.** { *; }

# Navigation-Routen (typsichere @Serializable-Routen)
-keep class de.rezeptbuch.app.ui.navigation.** { *; }
