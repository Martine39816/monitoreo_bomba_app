package com.example.monitoreobombaapp

import android.graphics.Color
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** Pantalla de borde a borde (Android 10+), con iconos claros sobre la cabecera oscura. */
fun AppCompatActivity.configurarPantalla() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    )
}

/** Suma al padding original los márgenes de barras del sistema / notch / teclado. */
fun View.aplicarInsets(arriba: Boolean = false, abajo: Boolean = false) {
    val l = paddingLeft
    val t = paddingTop
    val r = paddingRight
    val b = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val tipos = WindowInsetsCompat.Type.systemBars() or
            WindowInsetsCompat.Type.displayCutout() or
            WindowInsetsCompat.Type.ime()
        val i = insets.getInsets(tipos)
        v.setPadding(
            l + i.left,
            t + if (arriba) i.top else 0,
            r + i.right,
            b + if (abajo) i.bottom else 0
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

/** Columnas de la lista según el ancho: 1 en celular, 2 en pantallas anchas, 3 en tablets grandes. */
fun AppCompatActivity.columnasSegunAncho(): Int =
    (resources.configuration.screenWidthDp / 380).coerceIn(1, 3)
