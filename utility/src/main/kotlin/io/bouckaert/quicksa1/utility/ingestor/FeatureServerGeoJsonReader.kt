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
    val geometryFactory: GeometryFactory,
    val outFields: List<String>
) {
    suspend fun asFlow(): Flow<Geometry> {
        val featureCount = getFeatureCount()

        return channelFlow {
            // For the first request, we don't know the record count. We sum it here
            var recordCount = 0
            val firstGeometryCollection = GeoJSONReader.read(
                httpClient.get(baseUrl) {
                    url {
                        requestAllFeatures()
                        outFields(outFields)
                        geoJson()
                        resultOffset(0)
                    }
                }.bodyAsChannel().toInputStream(), geometryFactory
            ) as GeometryCollection
            for (i in 0 until firstGeometryCollection.numGeometries) {
                recordCount++
                send(firstGeometryCollection.getGeometryN(i))
            }

            // For subsequent requests, we use the record count from the first request
            val requestCount = (featureCount + recordCount - 1) / recordCount
            (1..requestCount).processInParallel { requestNumber ->
                val geometryCollection = GeoJSONReader.read(
                    httpClient.get(baseUrl) {
                        url {
                            requestAllFeatures()
                            outFields(outFields)
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
    private fun URLBuilder.outFields(fields: List<String>) = parameters.append("outFields", fields.joinToString(","))
    private fun URLBuilder.resultOffset(offset: Int) = parameters.append("resultOffset", offset.toString())

    @Serializable
    private data class CountResponse(val count: Int)
}