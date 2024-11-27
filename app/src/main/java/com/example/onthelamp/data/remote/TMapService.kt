package com.example.onthelamp

import com.example.onthelamp.data.model.PedestrianRouteRequest
import com.example.onthelamp.data.model.PedestrianRouteResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TMapService {
    // 키워드 검색 API
    @GET("tmap/pois")
    fun searchPOI(
        @Query("version") version: Int = 1,
        @Query("searchKeyword") keyword: String,
        @Query("appKey") appKey: String,
        @Query("resCoordType") resCoordType: String = "WGS84GEO"
    ): Call<SearchResponse>

    // 보행자 경로 탐색 API
    @POST("tmap/routes/pedestrian")
    fun findPedestrianRoute(
        @Query("version") version: Int = 1,
//        @Query("format") format: String = "json",
        @Query("appKey") appKey: String,
        @Body requestBody: PedestrianRouteRequest
    ): Call<PedestrianRouteResponse>
}
