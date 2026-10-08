package com.example.monitoreobombaapp.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("monitoreo_bomba_prefs", Context.MODE_PRIVATE)

    fun guardarSesion(token: String, usuario: Usuario) {
        prefs.edit()
            .putString("token", token)
            .putString("rol", usuario.rol)
            .putString("nombre", "${usuario.nombre} ${usuario.apellido}")
            .apply()
    }

    fun obtenerToken(): String? = prefs.getString("token", null)

    fun obtenerRol(): String? = prefs.getString("rol", null)

    fun obtenerNombre(): String? = prefs.getString("nombre", null)

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }

    fun haySesionActiva(): Boolean = obtenerToken() != null
}