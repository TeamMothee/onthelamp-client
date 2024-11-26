package com.example.onthelamp
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TMapRetrofitClient {
    private const val BASE_URL = "https://apis.openapi.sk.com/" // TMap API Base URL

    fun getInstance(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
