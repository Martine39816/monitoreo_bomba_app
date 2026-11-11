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
// Laravel devuelve directamente un arreglo de bombas.
// Ya no necesitamos la clase BombasResponse.

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

    val sensores: List<Sensor> = emptyList()
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

// ---- Alertas ----

data class AlertasResponse(
    val alertas: List<Alerta>
)

data class AlertaResponse(
    val alerta: Alerta
)

data class Alerta(
    val id: Int,
    val tipo: String,
    val descripcion: String?,
    @SerializedName("valor_detectado")
    val valorDetectado: String?,
    @SerializedName("valor_permitido")
    val valorPermitido: String?,
    @SerializedName("fecha_hora")
    val fechaHora: String?,
    val atendida: Boolean,
    @SerializedName("fecha_atencion")
    val fechaAtencion: String?,
    @SerializedName("atendida_por")
    val atendidaPor: String?,
    val bomba: BombaResumen?,
    val sensor: String?
)

data class BombaResumen(
    val id: Int,
    val codigo: String,
    val nombre: String
)


data class ControlBombaResponse(
    val mensaje: String,
    val encendido: Boolean,
    @SerializedName("bloqueo_remoto")
    val bloqueoRemoto: Boolean,
    val funcionando: Boolean
)
