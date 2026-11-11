package com.example.monitoreobombaapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.monitoreobombaapp.data.LoginRequest
import com.example.monitoreobombaapp.data.RetrofitClient
import com.example.monitoreobombaapp.data.SessionManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var etCorreo: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurarPantalla()
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.loginRoot).aplicarInsets(arriba = true, abajo = true)

        sessionManager = SessionManager(this)

        etCorreo = findViewById(R.id.etCorreo)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)

        // Si ya hay sesión activa, saltamos directo a la lista de bombas.
        if (sessionManager.haySesionActiva()) {
            irABombas()
            return
        }

        findViewById<View>(R.id.btnServidor).setOnClickListener { mostrarDialogoServidor() }

        btnLogin.setOnClickListener {
            intentarLogin()
        }
        etPassword.setOnEditorActionListener { _, accion, _ ->
            if (accion == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                intentarLogin()
                true
            } else false
        }
    }

    private fun intentarLogin() {
        val correo = etCorreo.text.toString().trim()
        val password = etPassword.text.toString()

        tvError.visibility = View.GONE

        if (correo.isEmpty() || password.isEmpty()) {
            mostrarError("Completa correo y contraseña.")
            return
        }

        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.login(LoginRequest(correo, password))

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    sessionManager.guardarSesion(loginResponse.token, loginResponse.usuario)
                    irABombas()
                } else {
                    // Errores 422 (validación) o 401 vienen aquí.
                    mostrarError("Credenciales incorrectas o cuenta inactiva.")
                }
            } catch (e: Exception) {
                // Errores de red: servidor apagado, sin conexión, timeout, etc.
                mostrarError("No se pudo conectar a ${RetrofitClient.urlActual()}\n${e.message}\nToca \"Configurar servidor\" si la IP cambió.")
            } finally {
                mostrarCargando(false)
            }
        }
    }

    private fun irABombas() {
        startActivity(Intent(this, BombasActivity::class.java))
        finish()
    }

    private fun mostrarCargando(cargando: Boolean) {
        progressBar.visibility = if (cargando) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !cargando
    }

    private fun mostrarError(mensaje: String) {
        tvError.text = mensaje
        tvError.visibility = View.VISIBLE
    }
}