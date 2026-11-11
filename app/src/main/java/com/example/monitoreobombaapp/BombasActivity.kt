
package com.example.monitoreobombaapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.monitoreobombaapp.data.Bomba
import com.example.monitoreobombaapp.data.BombaAdapter
import com.example.monitoreobombaapp.data.RetrofitClient
import com.example.monitoreobombaapp.data.SessionManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class BombasActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    private lateinit var tvUsuario: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvEnMarcha: TextView
    private lateinit var tvAlertasPend: TextView
    private lateinit var tvVacio: TextView

    private lateinit var btnLogout: ImageButton
    private lateinit var progressBar: View
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerBombas: RecyclerView
    private lateinit var etBuscar: TextInputEditText
    private lateinit var chipsCentros: ChipGroup

    private var todas: List<Bomba> = emptyList()
    private var centroSeleccionado: String? = null
    private var adapter: BombaAdapter? = null
    private var centrosMostrados: List<String> = emptyList()

    private val handler = Handler(Looper.getMainLooper())

    private val refrescoAuto = object : Runnable {
        override fun run() {
            cargarBombas(silencioso = true)
            handler.postDelayed(this, 20_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configurarPantalla()
        setContentView(R.layout.activity_bombas)

        sessionManager = SessionManager(this)

        tvUsuario = findViewById(R.id.tvUsuario)
        tvTotal = findViewById(R.id.tvTotalBombas)
        tvEnMarcha = findViewById(R.id.tvEnMarcha)
        tvAlertasPend = findViewById(R.id.tvAlertasPend)
        tvVacio = findViewById(R.id.tvVacioBombas)

        btnLogout = findViewById(R.id.btnLogout)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        recyclerBombas = findViewById(R.id.recyclerBombas)

        etBuscar = findViewById(R.id.etBuscar)
        chipsCentros = findViewById(R.id.chipsCentros)

        findViewById<View>(R.id.headerBombas).aplicarInsets(arriba = true)
        recyclerBombas.aplicarInsets(abajo = true)

        recyclerBombas.layoutManager =
            GridLayoutManager(this, columnasSegunAncho())

        tvUsuario.text =
            "${sessionManager.obtenerNombre()} (${sessionManager.obtenerRol()})"

        btnLogout.setOnClickListener {
            confirmarCerrarSesion()
        }

        findViewById<View>(R.id.cardAlertas).setOnClickListener {
            startActivity(Intent(this, AlertasActivity::class.java))
        }

        swipeRefresh.setOnRefreshListener {
            cargarBombas()
        }

        findViewById<View>(R.id.btnAjustes).setOnClickListener {
            mostrarDialogoServidor {
                cargarBombas()
            }
        }

        etBuscar.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {}

            override fun afterTextChanged(s: Editable?) {
                aplicarFiltros()
            }
        })
    }

    override fun onResume() {
        super.onResume()

        cargarBombas()

        handler.removeCallbacks(refrescoAuto)
        handler.postDelayed(refrescoAuto, 20_000)
    }

    override fun onPause() {
        handler.removeCallbacks(refrescoAuto)
        super.onPause()
    }

    private fun armarChips() {

        val centros = todas
            .mapNotNull { it.centroSalud }
            .distinct()
            .sorted()

        if (centros == centrosMostrados) return

        centrosMostrados = centros

        if (
            centroSeleccionado != null &&
            centroSeleccionado !in centros
        ) {
            centroSeleccionado = null
        }

        chipsCentros.setOnCheckedStateChangeListener(null)
        chipsCentros.removeAllViews()

        val opciones = listOf<String?>(null) + centros

        for (centro in opciones) {

            val chip = layoutInflater.inflate(
                R.layout.item_chip_centro,
                chipsCentros,
                false
            ) as Chip

            chip.id = View.generateViewId()
            chip.text = centro ?: "Todos"
            chip.tag = centro
            chip.isChecked = centro == centroSeleccionado

            chipsCentros.addView(chip)
        }

        chipsCentros.setOnCheckedStateChangeListener { grupo, ids ->

            val chip = ids.firstOrNull()?.let {
                grupo.findViewById<Chip>(it)
            }

            centroSeleccionado = chip?.tag as? String

            aplicarFiltros()
        }

        chipsCentros.visibility =
            if (centros.size > 1) View.VISIBLE else View.GONE
    }

    private fun aplicarFiltros() {

        val q = etBuscar.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val lista = todas.filter { bomba ->

            (
                    centroSeleccionado == null ||
                            bomba.centroSalud == centroSeleccionado
                    ) && (
                    q.isEmpty() ||
                            bomba.nombre.contains(q, true) ||
                            bomba.codigo.contains(q, true) ||
                            (bomba.centroSalud?.contains(q, true) == true)
                    )
        }

        adapter?.actualizar(lista)

        tvEnMarcha.text =
            lista.count { it.funcionando }.toString()

        tvTotal.text = lista.size.toString()

        tvVacio.visibility =
            if (lista.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun cargarBombas(silencioso: Boolean = false) {

        val token = sessionManager.obtenerToken()

        if (token == null) {
            irALogin()
            return
        }

        if (!silencioso) {
            progressBar.visibility = View.VISIBLE
        }

        lifecycleScope.launch {

            try {

                val response = RetrofitClient.apiService.getBombas(
                    "Bearer $token"
                )

                if (response.isSuccessful) {

                    // Laravel devuelve directamente una lista JSON.
                    val listaBombas = response.body() ?: emptyList()

                    todas = listaBombas

                    // Los permisos dependen del rol del usuario.
                    val rol = sessionManager.obtenerRol()
                        ?.lowercase()
                        .orEmpty()

                    val puedeControlar =
                        rol == "administrador" ||
                                rol == "tecnico"

                    if (adapter == null) {

                        adapter = BombaAdapter(
                            listaBombas,
                            puedeControlar,
                            { bomba, encender ->
                                pedirControl(
                                    bomba.id,
                                    bomba.nombre,
                                    encender
                                )
                            },
                            { bomba ->
                                // Aquí se conserva el espacio para
                                // abrir el detalle de la bomba.
                            }
                        )

                        recyclerBombas.adapter = adapter

                    } else {
                        adapter?.actualizar(listaBombas)
                    }

                    armarChips()
                    aplicarFiltros()

                    cargarConteoAlertas(token)

                    if (listaBombas.isEmpty() && !silencioso) {
                        Toast.makeText(
                            this@BombasActivity,
                            "No hay bombas registradas.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {

                    val mensaje = when (response.code()) {
                        401 -> "Tu sesión ha expirado. Inicia sesión nuevamente."
                        403 -> "No tienes permiso para consultar las bombas."
                        else -> "Error al cargar las bombas: ${response.code()}"
                    }

                    if (!silencioso) {
                        Toast.makeText(
                            this@BombasActivity,
                            mensaje,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                }

            } catch (e: Exception) {

                if (!silencioso) {
                    Toast.makeText(
                        this@BombasActivity,
                        "Error de conexión: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private suspend fun cargarConteoAlertas(token: String) {

        try {

            val response = RetrofitClient.apiService.getAlertas(
                "Bearer $token",
                1
            )

            tvAlertasPend.text =
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.alertas.size.toString()
                } else {
                    "–"
                }

        } catch (e: Exception) {

            tvAlertasPend.text = "–"
        }
    }

    private fun confirmarCerrarSesion() {

        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Quieres salir de tu cuenta?")
            .setPositiveButton("Cerrar sesión") { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cerrarSesion() {

        val token = sessionManager.obtenerToken()

        btnLogout.isEnabled = false

        lifecycleScope.launch {

            try {

                if (token != null) {
                    RetrofitClient.apiService.logout("Bearer $token")
                }

            } catch (e: Exception) {
                // Si no hay conexión, se cierra la sesión localmente.
            } finally {

                sessionManager.cerrarSesion()
                irALogin()
            }
        }
    }

    private fun irALogin() {

        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    private fun pedirControl(
        idBomba: Int,
        nombre: String,
        encender: Boolean
    ) {

        val accion = if (encender) "encender" else "apagar"

        AlertDialog.Builder(this)
            .setTitle("Confirmar orden")
            .setMessage("¿Quieres $accion la bomba \"$nombre\"?")
            .setPositiveButton(
                accion.replaceFirstChar { it.uppercase() }
            ) { _, _ ->

                controlarBomba(idBomba, encender)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun controlarBomba(
        idBomba: Int,
        encender: Boolean
    ) {

        val token = sessionManager.obtenerToken()

        if (token == null) {
            irALogin()
            return
        }

        lifecycleScope.launch {

            try {

                val response = if (encender) {

                    RetrofitClient.apiService.encenderBomba(
                        "Bearer $token",
                        idBomba
                    )

                } else {

                    RetrofitClient.apiService.apagarBomba(
                        "Bearer $token",
                        idBomba
                    )
                }

                if (response.isSuccessful && response.body() != null) {

                    Toast.makeText(
                        this@BombasActivity,
                        response.body()!!.mensaje,
                        Toast.LENGTH_LONG
                    ).show()

                    cargarBombas()

                } else {

                    val mensaje = when (response.code()) {
                        401 -> "Sesión expirada. Inicia sesión nuevamente."
                        403 -> "No tienes permiso para controlar esta bomba."
                        404 -> "No se encontró la bomba solicitada."
                        429 -> "Demasiadas órdenes. Espera un momento."
                        else -> "Error al controlar la bomba (${response.code()})."
                    }

                    Toast.makeText(
                        this@BombasActivity,
                        mensaje,
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@BombasActivity,
                    "No se pudo enviar la orden: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
