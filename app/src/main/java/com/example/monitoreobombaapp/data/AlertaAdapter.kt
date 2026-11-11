package com.example.monitoreobombaapp.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.monitoreobombaapp.R
import com.google.android.material.button.MaterialButton

class AlertaAdapter(
    private val alertas: MutableList<Alerta>,
    private val onSolucionar: (Alerta) -> Unit
) : RecyclerView.Adapter<AlertaAdapter.AlertaViewHolder>() {

    class AlertaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAlerta: ImageView = view.findViewById(R.id.imgAlerta)
        val tvTipo: TextView = view.findViewById(R.id.tvTipo)
        val tvBomba: TextView = view.findViewById(R.id.tvBomba)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcion)
        val tvValores: TextView = view.findViewById(R.id.tvValores)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val tvAtencion: TextView = view.findViewById(R.id.tvAtencion)
        val btnSolucionar: MaterialButton = view.findViewById(R.id.btnSolucionar)
    }

    companion object {
        private val ETIQUETAS = mapOf(
            "nivel_bajo" to "Nivel bajo",
            "nivel_alto" to "Nivel alto",
            "bomba_apagada" to "Bomba apagada",
            "falla_electrica" to "Falla eléctrica",
            "sobrecorriente" to "Sobrecorriente",
            "temperatura_alta" to "Temperatura alta",
            "vibracion_alta" to "Vibración alta",
            "sensor_desconectado" to "Sensor desconectado",
            "tiempo_excedido" to "Tiempo de funcionamiento excedido",
            "ciclos_frecuentes" to "Ciclos de encendido frecuentes",
            "posible_rebalse" to "Posible rebalse"
        )

        fun etiquetaDe(tipo: String): String = ETIQUETAS[tipo] ?: tipo.replace("_", " ")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alerta, parent, false)
        return AlertaViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertaViewHolder, position: Int) {
        val alerta = alertas[position]
        val ctx = holder.itemView.context

        // Si llega un tipo nuevo que la app no conoce, se muestra legible igual.
        holder.tvTipo.text = etiquetaDe(alerta.tipo)

        holder.tvBomba.text = listOfNotNull(
            alerta.bomba?.let { "${it.codigo} - ${it.nombre}" },
            alerta.sensor
        ).joinToString(" · ")

        holder.tvDescripcion.text = alerta.descripcion ?: ""
        holder.tvDescripcion.visibility =
            if (alerta.descripcion.isNullOrBlank()) View.GONE else View.VISIBLE

        if (alerta.valorDetectado != null) {
            val permitido = alerta.valorPermitido?.let { " (permitido: $it)" } ?: ""
            holder.tvValores.text = "Valor detectado: ${alerta.valorDetectado}$permitido"
            holder.tvValores.visibility = View.VISIBLE
        } else {
            holder.tvValores.visibility = View.GONE
        }

        holder.tvFecha.text = Formato.fechaLocal(alerta.fechaHora)

        if (alerta.atendida) {
            holder.tvEstado.text = "Solucionada"
            holder.tvEstado.setBackgroundResource(R.drawable.bg_chip_ok)
            holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.verde_osc))
            holder.imgAlerta.setBackgroundResource(R.drawable.bg_chip_ok)
            holder.imgAlerta.setColorFilter(ContextCompat.getColor(ctx, R.color.verde_osc))
            val quien = alerta.atendidaPor?.let { " por $it" } ?: ""
            holder.tvAtencion.text = "Atendida$quien · ${Formato.fechaLocal(alerta.fechaAtencion)}"
            holder.tvAtencion.visibility = View.VISIBLE
            holder.btnSolucionar.visibility = View.GONE
        } else {
            holder.tvEstado.text = "Pendiente"
            holder.tvEstado.setBackgroundResource(R.drawable.bg_chip_danger)
            holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.rojo_osc))
            holder.imgAlerta.setBackgroundResource(R.drawable.bg_chip_danger)
            holder.imgAlerta.clearColorFilter()
            holder.tvAtencion.visibility = View.GONE
            holder.btnSolucionar.visibility = View.VISIBLE
            holder.btnSolucionar.setOnClickListener { onSolucionar(alerta) }
        }
    }

    override fun getItemCount(): Int = alertas.size

    /** Reemplaza una alerta ya actualizada sin recargar toda la lista. */
    fun reemplazar(actualizada: Alerta) {
        val indice = alertas.indexOfFirst { it.id == actualizada.id }
        if (indice >= 0) {
            alertas[indice] = actualizada
            notifyItemChanged(indice)
        }
    }
}
