package com.example.monitoreobombaapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.monitoreobombaapp.data.SessionManager

/** Pantalla de bienvenida con el logo; después decide si va al login o a las bombas. */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val avanzar = Runnable {
        val destino = if (SessionManager(this).haySesionActiva()) BombasActivity::class.java
        else MainActivity::class.java
        startActivity(Intent(this, destino))
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurarPantalla()
        setContentView(R.layout.activity_splash)
        findViewById<View>(R.id.splashRoot).aplicarInsets(arriba = true, abajo = true)

        val logo = findViewById<View>(R.id.logoContenedor)
        logo.alpha = 0f
        logo.scaleX = 0.8f
        logo.scaleY = 0.8f
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).start()

        listOf(R.id.tvSplashTitulo, R.id.tvSplashSub).forEachIndexed { i, id ->
            val v = findViewById<View>(id)
            v.alpha = 0f
            v.translationY = 24f
            v.animate().alpha(1f).translationY(0f).setStartDelay(350L + i * 150L).setDuration(600).start()
        }

        handler.postDelayed(avanzar, 2200)
    }

    override fun onDestroy() {
        handler.removeCallbacks(avanzar)
        super.onDestroy()
    }
}
