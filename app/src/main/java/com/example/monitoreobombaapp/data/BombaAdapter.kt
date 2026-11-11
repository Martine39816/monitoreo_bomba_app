package com.example.monitoreobombaapp.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.monitoreobombaapp.R
import com.google.android.material.button.MaterialButton

class BombaAdapter(
    bombasIniciales: List<Bomba>,
    private val puedeControlar: Boolean,
    private val onControlar: (Bomba, Boolean) -> Unit,
    private val onAbrir: (Bomba) -> Unit
) : RecyclerView.Adapter<BombaAdapter.BombaViewHolder>() {

    private var bombas: List<Bomba> = bombasIniciales

    /** Cambia la lista mostrada (filtros, refresco automático). */
    @android.annotation.SuppressLint("NotifyDataSetChanged")
    fun actualizar(nuevas: List<Bomba>) {
        bombas = nuevas
        notifyDataSetChanged()
    }

    class BombaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombreBomba: TextView = view.findViewById(R.id.tvNombreBomba)
        val tvCentroSalud: TextView = view.findViewById(R.id.tvCentroSalud)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoEncendido)
        val contenedorSensores: LinearLayout = view.findViewById(R.id.contenedorSensores)
        val filaControl: View = view.findViewById(R.id.filaControl)
        val btnEncender: MaterialButton = view.findViewById(R.id.btnEncenderBomba)
        val btnApagar: MaterialButton = view.findViewById(R.id.btnApagarBomba)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BombaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bomba, parent, false)
        return BombaViewHolder(view)
    }

    override fun onBindViewHolder(holder: BombaViewHolder, position: Int) {
        val bomba = bombas[position]

        holder.tvNombreBomba.text = "${bomba.codigo} - ${bomba.nombre}"
        holder.tvCentroSalud.text = bomba.centroSalud ?: "Sin centro asignado"
        EstadoUi.aplicarEstado(holder.tvEstado, bomba)
        EstadoUi.dibujarSensores(holder.contenedorSensores, bomba.sensores, detallado = false)

        holder.filaControl.visibility = if (puedeControlar) View.VISIBLE else View.GONE
        holder.btnEncender.setOnClickListener { onControlar(bomba, true) }
        holder.btnApagar.setOnClickListener { onControlar(bomba, false) }
        holder.itemView.setOnClickListener { onAbrir(bomba) }
    }

    override fun getItemCount(): Int = bombas.size
}
