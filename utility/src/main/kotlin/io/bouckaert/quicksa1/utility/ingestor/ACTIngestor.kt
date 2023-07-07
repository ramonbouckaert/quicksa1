package io.bouckaert.quicksa1.utility.ingestor

import com.zaxxer.hikari.HikariDataSource
import io.bouckaert.jts2geojson.JtsFeatureMetadata
import io.bouckaert.quicksa1.shared.db.enums.BlockType
import io.bouckaert.quicksa1.shared.processInParallel
import io.ktor.client.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.WKBWriter
import java.io.ByteArrayInputStream

class ACTIngestor(
    private val hikariDataSource: HikariDataSource,
    private val httpClient: HttpClient,
    private val blocksGeoJsonUrl: String,
    private val roadReservesGeoJsonUrl: String
) {
    private val srid = 4326
    private var featuresProcessed = 0

    private val przRegex = Regex("PRZ")
    private val nuzRegex = Regex("NUZ")
    private val cfRegex = Regex("CF")
    private val tszRegex = Regex("TSZ")
    private val czRegex = Regex("CZ")
    private val izRegex = Regex("IZ")
    private val rzRegex = Regex("(^|[^P])RZ")
    private val desRegex = Regex("DES")
    suspend fun load() {
        val geometryFactory = GeometryFactory(PrecisionModel(PrecisionModel.FLOATING), srid)

        coroutineScope {
            awaitAll(
                async {
                    FeatureServerGeoJsonReader(
                        httpClient,
                        blocksGeoJsonUrl,
                        2000,
                        geometryFactory
                    ).asFlow().chunked(16).collect { it.processInParallel { block -> insertBlock(block) } }
                },
                async {
                    FeatureServerGeoJsonReader(
                        httpClient,
                        roadReservesGeoJsonUrl,
                        2000,
                        geometryFactory
                    ).asFlow().chunked(16).collect { it.processInParallel { road -> insertRoad(road) } }
                }
            )
        }

        println("\rFeatures added: $featuresProcessed")
    }

    private fun insertBlock(block: Geometry) {
        val blockData = block.userData as JtsFeatureMetadata
        val type: BlockType = parseBlockType(
            blockData.properties?.get("LAND_USE_POLICY_ZONES").toString(),
            (blockData.properties?.get("WATER_FLAG")?.toString() ?: "0") == "1"
        )
        val streetNumber: Int? = (blockData.properties?.get("ADDRESSES")?.toString() ?: "")
            .split(" ").firstOrNull()?.toIntOrNull()

        hikariDataSource.connection.use { conn ->
            val statement = conn.prepareStatement(blockInsertSql())
            statement.setString(1, type.toString())
            if (streetNumber != null) statement.setInt(2, streetNumber) else statement.setNull(
                2,
                java.sql.Types.INTEGER
            )
            statement.setBinaryStream(3, ByteArrayInputStream(WKBWriter().write(block)))
            statement.setInt(4, srid)
            statement.executeUpdate()
            conn.close()
        }
        featuresProcessed++
        if (featuresProcessed % 50 == 0) print("\rFeatures added: $featuresProcessed")
    }

    private fun insertRoad(road: Geometry) {
        val roadData = road.userData as JtsFeatureMetadata
        val name: String? = roadData.properties?.get("ROAD_NAME")?.toString()

        hikariDataSource.connection.use { conn ->
            val statement = conn.prepareStatement(roadInsertSql())
            if (name != null) statement.setString(1, name) else statement.setNull(2, java.sql.Types.VARCHAR)
            statement.setBinaryStream(2, ByteArrayInputStream(WKBWriter().write(road)))
            statement.setInt(3, srid)
            statement.executeUpdate()
            conn.close()
        }
        featuresProcessed++
        if (featuresProcessed % 50 == 0) print("\rFeatures added: $featuresProcessed")
    }

    private fun parseBlockType(input: String?, waterFlag: Boolean?): BlockType {
        if (input == null) return BlockType.RESIDENTIAL
        if (waterFlag != null && waterFlag) return BlockType.WATER
        if (input.matches(przRegex) || input.matches(nuzRegex)) return BlockType.PARK
        if (input.matches(cfRegex) || input.matches(tszRegex)) return BlockType.COMMUNITY
        if (input.matches(czRegex) || input.matches(izRegex)) return BlockType.COMMERCIAL
        if (input.matches(rzRegex)) return BlockType.RESIDENTIAL
        if (input.matches(desRegex)) return BlockType.PARK
        return BlockType.RESIDENTIAL
    }

    private fun blockInsertSql(numberOfGeometries: Int = 1) = """
        WITH ins1 AS ( 
                    INSERT INTO blocks (id, polygon, type, street_number)
                    VALUES (uuid_generate_v4(), uuid_generate_v4(), ?, ?)
                    RETURNING polygon
                )
                INSERT INTO multipolygons (id, geometry)
                SELECT polygon,
    """.trimIndent() + " " +
            (if (numberOfGeometries == 1) "ST_Multi(ST_Transform(ST_GeomFromWKB(?,?),3857))" else {
                "ST_Multi(ST_Transform(ST_Union(ARRAY[" + List(numberOfGeometries) { "ST_GeomFromWKB(?,?)" }.joinToString(
                    ","
                ) + "]),3857))"
            }) +
            " FROM ins1;"

    private fun roadInsertSql(numberOfGeometries: Int = 1) = """
        WITH ins1 AS ( 
                    INSERT INTO roads (id, polygon, name)
                    VALUES (uuid_generate_v4(), uuid_generate_v4(), ?)
                    RETURNING polygon
                )
                INSERT INTO multipolygons (id, geometry)
                SELECT polygon,
    """.trimIndent() + " " +
            (if (numberOfGeometries == 1) "ST_Multi(ST_Transform(ST_GeomFromWKB(?,?),3857))" else {
                "ST_Multi(ST_Transform(ST_Union(ARRAY[" + List(numberOfGeometries) { "ST_GeomFromWKB(?,?)" }.joinToString(
                    ","
                ) + "]),3857))"
            }) +
            " FROM ins1;"
}