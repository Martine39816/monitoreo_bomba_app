package com.example.monitoreobombaapp.data

import com.google.gson.annotations.SerializedName

// ---- Login ----

data class LoginRequest(
    val correo: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val usuario: Usuario
)

data class Usuario(
    val id: Int,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val rol: String,
    @SerializedName("centros_salud_id")
    val centrosSaludId: Int?
)

data class LogoutResponse(
    val mensaje: String
)

// ---- Bombas ----

data class BombasResponse(
    val bombas: List<Bomba>
)

data class Bomba(
    val id: Int,
    val codigo: String,
    val nombre: String,
    val encendido: Boolean,
    val funcionando: Boolean,
    @SerializedName("modo_operacion")
    val modoOperacion: Boolean,
    val estado: String,
    @SerializedName("centro_salud")
    val centroSalud: String?,
    val sensores: List<Sensor>
)

data class Sensor(
    val id: Int,
    val tipo: String,
    val nombre: String,
    @SerializedName("unidad_medida")
    val unidadMedida: String?,
    @SerializedName("valor_minimo")
    val valorMinimo: String,
    @SerializedName("valor_maximo")
    val valorMaximo: String,
    @SerializedName("ultima_lectura")
    val ultimaLectura: String?,
    @SerializedName("fecha_hora")
    val fechaHora: String?
)