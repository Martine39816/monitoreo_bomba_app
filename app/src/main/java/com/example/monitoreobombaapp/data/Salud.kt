package com.example.monitoreobombaapp.data

import com.example.monitoreobombaapp.R

/**
 * Resumen de "salud" de una bomba calculado en la app con lo que ya entrega Laravel:
 * sensores fuera de rango + alertas pendientes. Es un indicador orientativo.
 */
data class Salud(
    val titulo: String,
    val detalle: String,
    val fondo: Int,
    val color: Int,
    val sensoresFuera: Int,
    val sensoresTotal: Int,
    val alertasPendientes: Int
) {
    companion object {
        private fun num(s: String?): Double? = s?.trim()?.replace(",", ".")?.toDoubleOrNull()

        fun sensorFuera(s: Sensor): Boolean {
            val l = num(s.ultimaLectura) ?: return false
            val min = num(s.valorMinimo) ?: return false
            val max = num(s.valorMaximo) ?: return false
            return l < min || l > max
        }

        fun evaluar(b: Bomba, alertasPendientes: Int): Salud {
            val fuera = b.sensores.count { sensorFuera(it) }
            val total = b.sensores.size
            val estadoMalo = b.estado.contains("falla", true) || b.estado.contains("error", true)

            return when {
                alertasPendientes >= 3 || fuera >= 2 || estadoMalo -> Salud(
                    "Crítica", "Requiere revisión inmediata.",
                    R.drawable.bg_chip_danger, R.color.rojo_osc, fuera, total, alertasPendientes
                )
                alertasPendientes > 0 || fuera > 0 -> Salud(
                    "Atención", "Hay valores fuera de rango o alertas por atender.",
                    R.drawable.bg_chip_warn, R.color.ambar_osc, fuera, total, alertasPendientes
                )
                else -> Salud(
                    "Saludable", "Sensores dentro de rango y sin alertas pendientes.",
                    R.drawable.bg_chip_ok, R.color.verde_osc, fuera, total, alertasPendientes
                )
            }
        }
    }
}
