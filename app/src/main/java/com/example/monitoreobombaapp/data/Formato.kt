package com.example.monitoreobombaapp.data

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object Formato {

    /**
     * Laravel entrega las fechas en UTC (ej. 2026-09-28T05:56:01.000000Z).
     * Aquí se convierten a la hora de Bolivia para mostrarlas.
     */
    fun fechaLocal(iso: String?): String {
        if (iso == null || iso.length < 19) return "-"
        return try {
            val entrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
            val salida = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("America/La_Paz") }
            salida.format(entrada.parse(iso.substring(0, 19))!!)
        } catch (e: Exception) {
            iso
        }
    }
}