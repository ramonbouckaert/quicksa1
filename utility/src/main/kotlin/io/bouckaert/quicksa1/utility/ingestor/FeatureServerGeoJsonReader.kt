package io.bouckaert.quicksa1.utility.ingestor

import io.bouckaert.jts2geojson.GeoJSONReader
import io.bouckaert.quicksa1.shared.processInParallel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.Serializable
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFactory

class FeatureServerGeoJsonReader(
    val httpClient: HttpClient,
    val baseUrl: String = "https://services1.arcgis.com/E5n4f1VY84i0xSjy/arcgis/rest/services/ACTGOV_BLOCK_CURRENT/FeatureServer/0/query",
    val recordCount: Int = 2000,
    val geometryFactory: GeometryFactory
) {
    suspend fun asFlow(): Flow<Geometry> {
        val featureCount = getFeatureCount()
        val requestCount = (featureCount + recordCount - 1) / recordCount

        return channelFlow {
            (0..requestCount).processInParallel { requestNumber ->
                val geometryCollection = GeoJSONReader.read(
                    httpClient.get(baseUrl) {
                        url {
                            requestAllFeatures()
                            allOutFields()
                            geoJson()
                            resultOffset(requestNumber * recordCount)
                        }
                    }.bodyAsChannel().toInputStream(), geometryFactory
                ) as GeometryCollection
                for (i in 0 until geometryCollection.numGeometries) {
                    send(geometryCollection.getGeometryN(i))
                }
            }
        }
    }

    suspend fun getFeatureCount() =
        httpClient.get(baseUrl) {
            url {
                requestAllFeatures()
                esriJson()
                count()
            }
        }.body<CountResponse>().count

    private fun URLBuilder.requestAllFeatures() = parameters.append("where", "1=1")
    private fun URLBuilder.esriJson() = parameters.append("f", "json")
    private fun URLBuilder.geoJson() = parameters.append("f", "geojson")
    private fun URLBuilder.count() = parameters.append("returnCountOnly", "true")
    private fun URLBuilder.allOutFields() = parameters.append("outFields", "*")
    private fun URLBuilder.resultOffset(offset: Int) = parameters.append("resultOffset", offset.toString())

    @Serializable
    private data class CountResponse(val count: Int)
}