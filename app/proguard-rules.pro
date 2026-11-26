# Reglas de ProGuard/R8. Vacío por defecto para este proyecto académico.
# Ver: https://developer.android.com/studio/build/shrink-code

# Capa remota: Gson crea los DTO por reflexión y Retrofit lee las anotaciones de la interfaz.
# (La minificación está desactivada; estas reglas dejan listo el build si se activa.)
-keep class com.cappucare.app.data.remote.*Dto { *; }
-keep interface com.cappucare.app.data.remote.ApiService { *; }
-keepattributes Signature, *Annotation*
