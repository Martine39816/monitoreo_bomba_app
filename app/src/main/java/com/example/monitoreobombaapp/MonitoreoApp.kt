package com.example.monitoreobombaapp

import android.app.Application
import com.example.monitoreobombaapp.data.RetrofitClient

/** Se ejecuta una sola vez al abrir la app: carga la dirección del servidor guardada. */
class MonitoreoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.iniciar(this)
    }
}
