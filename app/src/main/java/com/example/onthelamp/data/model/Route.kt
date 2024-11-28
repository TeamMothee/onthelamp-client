package com.example.onthelamp.data.model

data class PedestrianRouteRequest(
    val startX: Double,
    val startY: Double,
    val endX: Double,
    val endY: Double,
    val reqCoordType: String = "WGS84GEO",
    val resCoordType: String = "WGS84GEO",
    val startName: String,
    val endName: String
)

data class PedestrianRouteResponse(
    val features: List<RouteFeature>
)

data class RouteFeature(
    val geometry: Geometry,
    val properties: Properties
)

data class Geometry(
    val type: String,
    val coordinates: Any
)

data class Properties(
    val pointType: String?,
    val totalDistance: Int?,
    val totalTime: Int?
)
