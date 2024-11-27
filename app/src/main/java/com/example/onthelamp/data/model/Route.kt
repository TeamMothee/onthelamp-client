package com.example.onthelamp.data.model

data class PedestrianRouteRequest(
    val startX: String,
    val startY: String,
    val endX: String,
    val endY: String,
    val reqCoordType: String = "WGS84GEO",
    val resCoordType: String = "EPSG3857",
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
    val coordinates: List<List<Double>>
)

data class Properties(
    val pointType: String?,
    val totalDistance: Int?,
    val totalTime: Int?
)
