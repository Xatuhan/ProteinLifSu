# ---- Sıfır log sızıntısı ----
# Release derlemesinde tüm android.util.Log çağrıları R8 tarafından tamamen silinir.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static java.lang.String getStackTraceString(java.lang.Throwable);
}

# Kaynak dosya adları release'de gizlenir.
-renamesourcefileattribute SourceFile
