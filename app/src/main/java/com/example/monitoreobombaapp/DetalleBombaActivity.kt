package com.example.monitoreobombaapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.monitoreobombaapp.data.Alerta
import com.example.monitoreobombaapp.data.AlertaAdapter
import com.example.monitoreobombaapp.data.Bomba
import com.example.monitoreobombaapp.data.EstadoUi
import com.example.monitoreobombaapp.data.RetrofitClient
import com.example.monitoreobombaapp.data.Salud
import com.example.monitoreobombaapp.data.SessionManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Detalle de una bomba: estado, control, salud, consumo estimado, alertas y sensores.
 */
class DetalleBombaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "bomba_id"
    }

    private lateinit var sessionManager: SessionManager
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: View

    private var bombaId = -1
    private var bomba: Bomba? = null

    private val handler = Handler(Looper.getMainLooper())
    private val refrescoAuto = object : Runnable {
        override fun run() {
            cargar(silencioso = true)
            handler.postDelayed(this, 15_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurarPantalla()
        setContentView(R.layout.activity_detalle_bomba)

        sessionManager = SessionManager(this)
        bombaId = intent.getIntExtra(EXTRA_ID, -1)

        if (bombaId < 0) {
            finish()
            return
        }

        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)

        findViewById<View>(R.id.headerDetalle).aplicarInsets(arriba = true)
        findViewById<View>(R.id.scrollDetalle).aplicarInsets(abajo = true)

        findViewById<View>(R.id.btnVolver).setOnClickListener { finish() }
        swipeRefresh.setOnRefreshListener { cargar() }

        val rol = sessionManager.obtenerRol()?.lowercase()
        val puedeControlar = rol == "administrador" || rol == "tecnico"
        findViewById<View>(R.id.filaControl).visibility =
            if (puedeControlar) View.VISIBLE else View.GONE

        findViewById<View>(R.id.btnEncender).setOnClickListener { confirmar(true) }
        findViewById<View>(R.id.btnApagar).setOnClickListener { confirmar(false) }

        findViewById<View>(R.id.btnVerAlertas).setOnClickListener {
            startActivity(
                Intent(this, AlertasActivity::class.java)
                    .putExtra(AlertasActivity.EXTRA_BOMBA_ID, bombaId)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        cargar()
        handler.removeCallbacks(refrescoAuto)
        handler.postDelayed(refrescoAuto, 15_000)
    }

    override fun onPause() {
        handler.removeCallbacks(refrescoAuto)
        super.onPause()
    }

    private fun cargar(silencioso: Boolean = false) {
        val token = sessionManager.obtenerToken()
        if (token == null) {
            irALogin()
            return
        }

        if (!swipeRefresh.isRefreshing && !silencioso) progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val respuesta = RetrofitClient.apiService.getBombas("Bearer $token")

                if (respuesta.isSuccessful && respuesta.body() != null) {
                    val encontrada = respuesta.body()!!.size.firstOrNull { it.id == bombaId }

                    if (encontrada == null) {
                        Toast.makeText(this@DetalleBombaActivity, "No se encontró la bomba.", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        bomba = encontrada
                        mostrar(encontrada)
                        val pendientes = cargarAlertas(token)
                        mostrarSalud(encontrada, pendientes)
                        cargarConsumo(token)
                    }
                } else if (respuesta.code() == 401) {
                    sessionManager.cerrarSesion()
                    irALogin()
                } else {
                    Toast.makeText(
                        this@DetalleBombaActivity,
                        "Error al cargar (código ${respuesta.code()}).",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                if (!silencioso) {
                    Toast.makeText(
                        this@DetalleBombaActivity,
                        "No se pudo conectar a ${RetrofitClient.urlActual()}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun mostrar(b: Bomba) {
        findViewById<TextView>(R.id.tvDetNombre).text = "${b.codigo} - ${b.nombre}"
        findViewById<TextView>(R.id.tvDetCentro).text = b.centroSalud ?: "Sin centro asignado"

        EstadoUi.aplicarEstado(findViewById(R.id.tvDetEstado), b)

        findViewById<TextView>(R.id.tvDetFuncionamiento).text =
            "Funcionamiento: " + if (b.funcionando) "en marcha" else "detenida"
        findViewById<TextView>(R.id.tvDetOrden).text =
            "Última orden: " + if (b.encendido) "encender" else "apagar"
        findViewById<TextView>(R.id.tvDetEstadoApi).text =
            "Estado reportado: ${b.estado.replace("_", " ")}"

        EstadoUi.dibujarSensores(
            findViewById<LinearLayout>(R.id.contenedorSensores),
            b.sensores,
            detallado = true
        )

        findViewById<TextView>(R.id.tvDetActualizado).text =
            "Actualizado " + SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()) +
                " · se actualiza solo cada 15 s · desliza para refrescar"
    }

    private fun mostrarSalud(b: Bomba, pendientes: Int) {
        val salud = Salud.evaluar(b, pendientes)
        val chip = findViewById<TextView>(R.id.tvDetSalud)
        chip.text = "● ${salud.titulo}"
        chip.setBackgroundResource(salud.fondo)
        chip.setTextColor(ContextCompat.getColor(this, salud.color))

        findViewById<TextView>(R.id.tvDetSaludDetalle).text = salud.detalle
        findViewById<TextView>(R.id.tvDetSaludSensores).text =
            "Sensores fuera de rango: ${salud.sensoresFuera} de ${salud.sensoresTotal}"
        findViewById<TextView>(R.id.tvDetSaludAlertas).text =
            "Alertas pendientes: ${salud.alertasPendientes}"
    }

    /** Alertas pendientes de ESTA bomba (se filtran de la lista del servidor). Devuelve cuántas hay. */
    private suspend fun cargarAlertas(token: String): Int {
        val contenedor = findViewById<LinearLayout>(R.id.contenedorAlertasBomba)
        val sinAlertas = findViewById<TextView>(R.id.tvSinAlertas)
        return try {
            val r = RetrofitClient.apiService.getAlertas("Bearer $token", 1)
            if (r.isSuccessful && r.body() != null) {
                val propias = r.body()!!.alertas.filter { it.bomba?.id == bombaId && !it.atendida }
                contenedor.removeAllViews()
                for (a in propias.take(5)) contenedor.addView(filaAlerta(a, contenedor))
                sinAlertas.visibility = if (propias.isEmpty()) View.VISIBLE else View.GONE
                propias.size
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    private fun filaAlerta(a: Alerta, padre: LinearLayout): View {
        val v = layoutInflater.inflate(R.layout.item_alerta_mini, padre, false)
        v.findViewById<TextView>(R.id.tvMiniTipo).text = AlertaAdapter.etiquetaDe(a.tipo)
        val valor = a.valorDetectado?.let { "Valor: $it · " } ?: ""
        v.findViewById<TextView>(R.id.tvMiniDetalle).text =
            valor + com.example.monitoreobombaapp.data.Formato.fechaLocal(a.fechaHora)
        v.findViewById<MaterialButton>(R.id.btnMiniSolucionar).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Marcar como solucionada")
                .setMessage("Esta acción quedará registrada a tu nombre. ¿Confirmas?")
                .setPositiveButton("Sí, solucionada") { _, _ -> solucionar(a) }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        return v
    }

    private fun solucionar(a: Alerta) {
        val token = sessionManager.obtenerToken() ?: return irALogin()
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.apiService.solucionarAlerta("Bearer $token", a.id)
                when {
                    r.isSuccessful -> {
                        Toast.makeText(this@DetalleBombaActivity, "Alerta solucionada.", Toast.LENGTH_SHORT).show()
                        cargar()
                    }
                    r.code() == 403 -> Toast.makeText(this@DetalleBombaActivity, "No tienes permiso sobre esta alerta.", Toast.LENGTH_LONG).show()
                    else -> Toast.makeText(this@DetalleBombaActivity, "No se pudo actualizar (código ${r.code()}).", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DetalleBombaActivity, "No se pudo conectar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** El consumo lo calcula Laravel; la app solo lo muestra. */
    private suspend fun cargarConsumo(token: String) {
        val destinos = mapOf(
            "hoy" to R.id.tvConsumoDia,
            "semana" to R.id.tvConsumoSemana,
            "mes" to R.id.tvConsumoMes
        )
        for ((periodo, idVista) in destinos) {
            val tv = findViewById<TextView>(idVista)
            tv.text = try {
                val r = RetrofitClient.apiService.getConsumo("Bearer $token", bombaId, periodo)
                val litros = if (r.isSuccessful) {
                    r.body()?.getAsJsonObject("resultado")?.getAsJsonObject("total")?.get("litros")
                } else null

                if (litros == null || litros.isJsonNull) "N/D"
                else "${"%,.0f".format(Locale("es", "BO"), litros.asDouble)} L"
            } catch (e: Exception) {
                "N/D"
            }
        }
    }

    private fun confirmar(encender: Boolean) {
        val accion = if (encender) "encender" else "apagar"
        AlertDialog.Builder(this)
            .setTitle("Confirmar orden")
            .setMessage("¿Quieres $accion esta bomba?")
            .setPositiveButton(accion.replaceFirstChar { it.uppercase() }) { _, _ -> controlar(encender) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun controlar(encender: Boolean) {
        val token = sessionManager.obtenerToken()
        if (token == null) {
            irALogin()
            return
        }

        lifecycleScope.launch {
            try {
                val respuesta = if (encender) {
                    RetrofitClient.apiService.encenderBomba("Bearer $token", bombaId)
                } else {
                    RetrofitClient.apiService.apagarBomba("Bearer $token", bombaId)
                }

                if (respuesta.isSuccessful && respuesta.body() != null) {
                    Toast.makeText(this@DetalleBombaActivity, respuesta.body()!!.mensaje, Toast.LENGTH_LONG).show()
                    cargar()
                } else {
                    val mensaje = when (respuesta.code()) {
                        401 -> "Sesión expirada. Inicia sesión nuevamente."
                        403 -> "No tienes permiso para controlar esta bomba."
                        404 -> "No se encontró la bomba solicitada."
                        429 -> "Demasiadas órdenes. Espera un momento."
                        else -> "Error al controlar la bomba (${respuesta.code()})."
                    }
                    Toast.makeText(this@DetalleBombaActivity, mensaje, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DetalleBombaActivity, "No se pudo enviar la orden: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun irALogin() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
