package com.example.monitoreobombaapp

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.monitoreobombaapp.data.BombaAdapter
import com.example.monitoreobombaapp.data.RetrofitClient
import com.example.monitoreobombaapp.data.SessionManager
import kotlinx.coroutines.launch

class BombasActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var tvUsuario: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerBombas: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bombas)

        sessionManager = SessionManager(this)

        tvUsuario = findViewById(R.id.tvUsuario)
        progressBar = findViewById(R.id.progressBar)
        recyclerBombas = findViewById(R.id.recyclerBombas)

        recyclerBombas.layoutManager = LinearLayoutManager(this)

        tvUsuario.text = "${sessionManager.obtenerNombre()} (${sessionManager.obtenerRol()})"

        cargarBombas()
    }

    private fun cargarBombas() {
        val token = sessionManager.obtenerToken()
        if (token == null) {
            Toast.makeText(this, "Sesión no válida, vuelve a iniciar sesión.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getBombas("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val bombas = response.body()!!.bombas
                    recyclerBombas.adapter = BombaAdapter(bombas)
                } else if (response.code() == 401) {
                    // Token inválido o expirado: forzamos nuevo login.
                    sessionManager.cerrarSesion()
                    Toast.makeText(this@BombasActivity, "Sesión expirada, inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@BombasActivity, "Error al cargar bombas (código ${response.code()}).", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@BombasActivity, "No se pudo conectar: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = android.view.View.GONE
            }
        }
    }
}