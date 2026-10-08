package com.example.monitoreobombaapp.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.monitoreobombaapp.R

class BombaAdapter(private val bombas: List<Bomba>) :
    RecyclerView.Adapter<BombaAdapter.BombaViewHolder>() {

    class BombaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombreBomba: TextView = view.findViewById(R.id.tvNombreBomba)
        val tvCentroSalud: TextView = view.findViewById(R.id.tvCentroSalud)
        val tvEstadoEncendido: TextView = view.findViewById(R.id.tvEstadoEncendido)
        val tvSensores: TextView = view.findViewById(R.id.tvSensores)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BombaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bomba, parent, false)
        return BombaViewHolder(view)
    }

    override fun onBindViewHolder(holder: BombaViewHolder, position: Int) {
        val bomba = bombas[position]

        holder.tvNombreBomba.text = "${bomba.codigo} - ${bomba.nombre}"
        holder.tvCentroSalud.text = bomba.centroSalud ?: "Sin centro asignado"

        val real = if (bomba.funcionando) "🟢 Funcionando" else "⚪ Detenida"
        val orden = if (bomba.encendido) "Orden: encender" else "Orden: apagar"
        holder.tvEstadoEncendido.text = "$real  ·  $orden"

        holder.tvSensores.text = bomba.sensores.joinToString(separator = "\n") { sensor ->
            val valor = sensor.ultimaLectura ?: "sin lectura"
            val unidad = sensor.unidadMedida ?: ""
            "• ${sensor.nombre}: $valor $unidad"
        }
    }

    override fun getItemCount(): Int = bombas.size
}