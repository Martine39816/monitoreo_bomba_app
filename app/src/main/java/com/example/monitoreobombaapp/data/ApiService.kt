package com.example.monitoreobombaapp.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("api/mobile/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/mobile/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<LogoutResponse>

    @GET("api/mobile/bombas")
    suspend fun getBombas(@Header("Authorization") token: String): Response<BombasResponse>
}