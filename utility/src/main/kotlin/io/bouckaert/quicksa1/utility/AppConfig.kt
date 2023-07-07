package io.bouckaert.quicksa1.utility

class AppConfig {

    data class Config(
        val database: Database,
        val httpClient: HttpClient,
        val abs: ABS?,
        val act: ACT
    )
    data class Database(
        val url: String?,
        val driver: String?
    )

    data class HttpClient(
        val verifyCertificates: Boolean = true,
        val timeoutSeconds: Int = Int.MAX_VALUE
    )

    data class ABS(
        val absStructuresGeoPackageUrl: String?
    )

    data class ACT(
        val blocksGeoJsonUrl: String?,
        val roadReservesGeoJsonUrl: String?
    )
}