package com.example.monitoreobombaapp

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.monitoreobombaapp.data.RetrofitClient
import com.google.android.material.textfield.TextInputEditText

/** Diálogo para cambiar la dirección del servidor Laravel sin recompilar la app. */
fun AppCompatActivity.mostrarDialogoServidor(alGuardar: (() -> Unit)? = null) {
    val vista = layoutInflater.inflate(R.layout.dialog_servidor, null)
    val campo = vista.findViewById<TextInputEditText>(R.id.etServidor)
    campo.setText(RetrofitClient.urlActual())
    campo.setSelection(campo.text?.length ?: 0)

    AlertDialog.Builder(this)
        .setTitle("Servidor")
        .setView(vista)
        .setPositiveButton("Guardar") { _, _ ->
            if (RetrofitClient.cambiarUrl(this, campo.text.toString())) {
                Toast.makeText(this, "Servidor: ${RetrofitClient.urlActual()}", Toast.LENGTH_LONG).show()
                alGuardar?.invoke()
            } else {
                Toast.makeText(this, "Dirección no válida.", Toast.LENGTH_LONG).show()
            }
        }
        .setNeutralButton("Restablecer") { _, _ ->
            RetrofitClient.restablecer(this)
            Toast.makeText(this, "Servidor: ${RetrofitClient.urlActual()}", Toast.LENGTH_LONG).show()
            alGuardar?.invoke()
        }
        .setNegativeButton("Cancelar", null)
        .show()
}
