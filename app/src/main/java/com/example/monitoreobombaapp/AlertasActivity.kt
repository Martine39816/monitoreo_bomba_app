package com.example.monitoreobombaapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.monitoreobombaapp.data.Alerta
import com.example.monitoreobombaapp.data.AlertaAdapter
import com.example.monitoreobombaapp.data.RetrofitClient
import com.example.monitoreobombaapp.data.SessionManager
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.launch

class AlertasActivity : AppCompatActivity() {

    companion object {
        /** Si se envía, solo se muestran las alertas de esa bomba. */
        const val EXTRA_BOMBA_ID = "bomba_id"
    }

    private var filtroBombaId = -1

    private lateinit var sessionManager: SessionManager
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerAlertas: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvVacio: TextView
    private lateinit var toggleFiltro: MaterialButtonToggleGroup
    private var adapter: AlertaAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurarPantalla()
        setContentView(R.layout.activity_alertas)

        sessionManager = SessionManager(this)
        filtroBombaId = intent.getIntExtra(EXTRA_BOMBA_ID, -1)

        swipeRefresh = findViewById(R.id.swipeRefresh)
        recyclerAlertas = findViewById(R.id.recyclerAlertas)
        progressBar = findViewById(R.id.progressBar)
        tvVacio = findViewById(R.id.tvVacio)
        toggleFiltro = findViewById(R.id.toggleFiltro)

        findViewById<View>(R.id.headerAlertas).aplicarInsets(arriba = true)
        recyclerAlertas.aplicarInsets(abajo = true)

        recyclerAlertas.layoutManager = GridLayoutManager(this, columnasSegunAncho())

        findViewById<View>(R.id.btnVolver).setOnClickListener { finish() }
        toggleFiltro.addOnButtonCheckedListener { _, _, marcado -> if (marcado) cargarAlertas() }
        swipeRefresh.setOnRefreshListener { cargarAlertas() }

        cargarAlertas()
    }

    private fun cargarAlertas() {
        val token = sessionManager.obtenerToken()
        if (token == null) {
            irALogin()
            return
        }

        if (!swipeRefresh.isRefreshing) progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val soloPendientes = if (toggleFiltro.checkedButtonId == R.id.btnFiltroPendientes) 1 else null
                val response = RetrofitClient.apiService.getAlertas("Bearer $token", soloPendientes)

                if (response.isSuccessful && response.body() != null) {
                    val lista = response.body()!!.alertas
                        .filter { filtroBombaId < 0 || it.bomba?.id == filtroBombaId }
                        .toMutableList()
                    adapter = AlertaAdapter(lista) { alerta -> confirmarSolucion(alerta) }
                    recyclerAlertas.adapter = adapter
                    tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                } else if (response.code() == 401) {
                    sesionExpirada()
                } else {
                    mostrar("Error al cargar alertas (código ${response.code()}).")
                }
            } catch (e: Exception) {
                mostrar("No se pudo conectar: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun confirmarSolucion(alerta: Alerta) {
        AlertDialog.Builder(this)
            .setTitle("Marcar como solucionada")
            .setMessage("Esta acción quedará registrada a tu nombre. ¿Confirmas que la alerta fue solucionada?")
            .setPositiveButton("Sí, solucionada") { _, _ -> solucionar(alerta) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun solucionar(alerta: Alerta) {
        val token = sessionManager.obtenerToken()
        if (token == null) {
            irALogin()
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.solucionarAlerta("Bearer $token", alerta.id)

                when {
                    response.isSuccessful && response.body() != null -> {
                        adapter?.reemplazar(response.body()!!.alerta)
                        mostrar("Alerta marcada como solucionada.")
                    }
                    response.code() == 401 -> sesionExpirada()
                    response.code() == 403 -> mostrar("No tienes permiso sobre esta alerta.")
                    else -> mostrar("No se pudo actualizar (código ${response.code()}).")
                }
            } catch (e: Exception) {
                mostrar("No se pudo conectar: ${e.message}")
            }
        }
    }

    private fun sesionExpirada() {
        sessionManager.cerrarSesion()
        mostrar("Sesión expirada, inicia sesión de nuevo.")
        irALogin()
    }

    private fun irALogin() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun mostrar(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }
}