package com.example.monitoreobombaapp.data

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.monitoreobombaapp.R
import com.google.android.material.progressindicator.LinearProgressIndicator

object EstadoUi {

    /** Etiqueta de color según el estado real de la bomba. */
    fun aplicarEstado(tv: TextView, b: Bomba) {
        val (texto, fondo, color) = when {
            b.funcionando -> Triple("● En marcha", R.drawable.bg_chip_ok, R.color.verde_osc)
            b.encendido -> Triple("● Arrancando", R.drawable.bg_chip_warn, R.color.ambar_osc)
            else -> Triple("● Detenida", R.drawable.bg_chip_off, R.color.texto_sec)
        }
        tv.text = texto
        tv.setBackgroundResource(fondo)
        tv.setTextColor(ContextCompat.getColor(tv.context, color))
    }

    /** Icono según el tipo/nombre del sensor (temperatura, vibración, nivel, corriente). */
    private fun iconoSensor(s: Sensor): Int {
        val t = "${s.tipo} ${s.nombre}".lowercase()
        return when {
            "temp" in t -> R.drawable.ic_termometro
            "vib" in t -> R.drawable.ic_vibracion
            "nivel" in t || "flot" in t || "agua" in t -> R.drawable.ic_nivel
            "corr" in t || "amp" in t || "elect" in t || "energ" in t -> R.drawable.ic_energia
            else -> R.drawable.ic_sensor
        }
    }

    private fun num(s: String?): Double? = s?.trim()?.replace(",", ".")?.toDoubleOrNull()

    /** Dibuja los sensores de una bomba dentro del contenedor. */
    fun dibujarSensores(contenedor: ViewGroup, sensores: List<Sensor>, detallado: Boolean) {
        contenedor.removeAllViews()
        val ctx = contenedor.context
        if (sensores.isEmpty()) {
            contenedor.addView(TextView(ctx).apply {
                text = "Sin sensores registrados"
                setTextColor(ContextCompat.getColor(ctx, R.color.texto_sec))
                textSize = 14f
            })
            return
        }
        val inflater = LayoutInflater.from(ctx)
        for (s in sensores) {
            val v = inflater.inflate(R.layout.item_sensor, contenedor, false)
            val unidad = s.unidadMedida ?: ""
            val lectura = num(s.ultimaLectura)
            val min = num(s.valorMinimo)
            val max = num(s.valorMaximo)
            val fuera = lectura != null && min != null && max != null && (lectura < min || lectura > max)

            v.findViewById<TextView>(R.id.tvSensorNombre).text = s.nombre
            v.findViewById<android.widget.ImageView>(R.id.imgSensor).setImageResource(iconoSensor(s))
            val tvValor = v.findViewById<TextView>(R.id.tvSensorValor)
            tvValor.text = if (s.ultimaLectura == null) "sin lectura" else "${s.ultimaLectura} $unidad".trim()
            tvValor.setTextColor(
                ContextCompat.getColor(ctx, if (fuera) R.color.rojo_osc else R.color.texto)
            )

            if (detallado) {
                val rango = v.findViewById<TextView>(R.id.tvSensorRango)
                rango.text = "Rango permitido: ${s.valorMinimo} – ${s.valorMaximo} $unidad".trim() +
                    (if (s.fechaHora != null) " · ${Formato.fechaLocal(s.fechaHora)}" else "")
                rango.visibility = View.VISIBLE

                if (lectura != null && min != null && max != null && max > min) {
                    val p = (((lectura - min) / (max - min)) * 100).toInt().coerceIn(0, 100)
                    val barra = v.findViewById<LinearProgressIndicator>(R.id.progressSensor)
                    barra.setIndicatorColor(
                        ContextCompat.getColor(ctx, if (fuera) R.color.rojo else R.color.turquesa)
                    )
                    barra.setProgressCompat(p, false)
                    barra.visibility = View.VISIBLE
                }
            }
            contenedor.addView(v)
        }
    }
}
