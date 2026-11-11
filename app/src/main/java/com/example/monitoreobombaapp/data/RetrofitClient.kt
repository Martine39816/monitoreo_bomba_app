package com.example.monitoreobombaapp.data

import android.content.Context
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Dirección por defecto (la de tu proyecto). Ahora se puede cambiar desde la app
    // (icono de engranaje) sin recompilar: útil cuando la PC recibe otra IP.
    // Emulador de Android Studio: http://10.0.2.2:8000/
    const val URL_POR_DEFECTO = "http://192.168.100.35:8000/"

    private const val PREFS = "monitoreo_bomba_prefs_servidor"
    private const val CLAVE_URL = "server_url"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var baseUrl: String = URL_POR_DEFECTO

    @Volatile
    private var servicio: ApiService? = null

    fun iniciar(context: Context) {
        val guardada = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CLAVE_URL, null)
        baseUrl = normalizar(guardada) ?: URL_POR_DEFECTO
        servicio = null
    }

    fun urlActual(): String = baseUrl

    /** Devuelve true si la dirección es válida y quedó guardada. */
    fun cambiarUrl(context: Context, nueva: String): Boolean {
        val url = normalizar(nueva) ?: return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(CLAVE_URL, url).apply()
        baseUrl = url
        servicio = null
        return true
    }

    fun restablecer(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(CLAVE_URL).apply()
        baseUrl = URL_POR_DEFECTO
        servicio = null
    }

    /** Acepta "192.168.1.5:8000", agrega http:// y la barra final si faltan. */
    private fun normalizar(texto: String?): String? {
        var t = texto?.trim().orEmpty()
        if (t.isEmpty()) return null
        if (!t.startsWith("http://") && !t.startsWith("https://")) t = "http://$t"
        if (!t.endsWith("/")) t += "/"
        return if (t.toHttpUrlOrNull() != null) t else null
    }

    val apiService: ApiService
        get() = servicio ?: synchronized(this) {
            servicio ?: Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
                .also { servicio = it }
        }
}
