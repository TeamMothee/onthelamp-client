package com.example.onthelamp

data class SearchResponse(
    val searchPoiInfo: SearchPoiInfo?
)

data class SearchPoiInfo(
    val pois: POIs?
)

data class POIs(
    val poi: List<POI>?
)

data class POI(
    val name: String?,
    val frontLat: Double?,  // 입구 위도
    val frontLon: Double?,   // 입구 경도
    val upperAddrName: String?,
    val middleAddrName: String?,
    val lowerAddrName: String?
)
