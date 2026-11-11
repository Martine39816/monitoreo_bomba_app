package com.example.monitoreobombaapp.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import com.google.gson.JsonObject

interface ApiService {

    @POST("api/mobile/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/mobile/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<LogoutResponse>
    @GET("api/mobile/bombas")
    suspend fun getBombas(
        @Header("Authorization") token: String
    ): Response<List<Bomba>>
    @GET("api/mobile/alertas")
    suspend fun getAlertas(
        @Header("Authorization") token: String,
        @Query("pendientes") pendientes: Int? = null
    ): Response<AlertasResponse>

    @POST("api/mobile/alertas/{id}/solucionar")
    suspend fun solucionarAlerta(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<AlertaResponse>


    @POST("api/mobile/bombas/{id}/encender")
    suspend fun encenderBomba(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<ControlBombaResponse>

    @POST("api/mobile/bombas/{id}/apagar")
    suspend fun apagarBomba(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<ControlBombaResponse>



    @GET("api/mobile/bombas/{bomba}/consumo")
    suspend fun getConsumo(
        @Header("Authorization") token: String,
        @Path("bomba") bomba: Int,
        @Query("periodo") periodo: String
    ): Response<JsonObject>



}