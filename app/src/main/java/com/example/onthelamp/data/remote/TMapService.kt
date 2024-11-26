package com.example.onthelamp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface TMapService {
    @GET("tmap/pois")
    fun searchPOI(
        @Query("version") version: Int = 1,
        @Query("searchKeyword") keyword: String,
        @Query("appKey") appKey: String,
        @Query("resCoordType") resCoordType: String = "WGS84GEO"
    ): Call<SearchResponse>
}
