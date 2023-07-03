package io.bouckaert.quicksa1.utility.ingestor

import io.bouckaert.quicksa1.shared.db.enums.State
import io.bouckaert.quicksa1.shared.filterNotNull
import io.bouckaert.quicksa1.shared.processInParallel
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import mil.nga.geopackage.GeoPackageManager
import mil.nga.geopackage.features.user.FeatureRow
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

class ABSIngestor(
    private val database: Database,
    private val httpClient: HttpClient,
    private val absStructuresGeoPackageUrl: String?
) {

    companion object {
        val stateCodeMapping = mapOf(
            1 to State.NSW,
            2 to State.VIC,
            3 to State.QLD,
            4 to State.SA,
            5 to State.WA,
            6 to State.TAS,
            7 to State.NT,
            8 to State.ACT
        )
    }

    suspend fun load() {
        if (absStructuresGeoPackageUrl == null) throw Error("No URL provided for ABS: Main Structure and Greater Capital City Statistical Areas GeoPackage")

        val absFile = runCatching { File.createTempFile("abs", ".gpkg").apply { this.deleteOnExit() } }.getOrThrow()

        try {
            httpClient.prepareGet {
                url {
                    takeFrom(absStructuresGeoPackageUrl)
                }
            }.execute { httpResponse ->
                val inputStream: InputStream = httpResponse.bodyAsChannel().toInputStream()
                val zipInputStream = ZipInputStream(inputStream)

                var entry = zipInputStream.nextEntry

                while (entry !== null) {
                    if (entry.name.endsWith(".gpkg")) {
                        zipInputStream.transferTo(absFile.outputStream())
                    } else kotlin.runCatching { zipInputStream.closeEntry() }

                    entry = zipInputStream.nextEntry
                }
            }
            val absGeoPackage = GeoPackageManager.open(absFile)
            try {
                val sa1TableName = absGeoPackage.tables.find { it.startsWith("SA1") }
                val sa2TableName = absGeoPackage.tables.find { it.startsWith("SA2") }
                val sa3TableName = absGeoPackage.tables.find { it.startsWith("SA3") }
                val sa4TableName = absGeoPackage.tables.find { it.startsWith("SA4") }

                var stateCodeColumnName: String? = null
                var sa1CodeColumnName: String? = null
                var sa2CodeColumnName: String? = null
                var sa2NameColumnName: String? = null
                var sa3CodeColumnName: String? = null
                var sa3NameColumnName: String? = null
                var sa4CodeColumnName: String? = null
                var sa4NameColumnName: String? = null

                val sa4Features = absGeoPackage.getFeatureDao(sa4TableName).queryForAll()
                    .filter { sa4Feature ->
                        if (sa4CodeColumnName === null) sa4Feature.columnNames?.find { it.startsWith("SA4_CODE") }
                            ?.let { sa4CodeColumnName = it }
                        if (sa4NameColumnName === null) sa4Feature.columnNames?.find { it.startsWith("SA4_NAME") }
                            ?.let { sa4NameColumnName = it }
                        if (stateCodeColumnName === null) sa4Feature.columnNames?.find { it.startsWith("STATE_CODE") }
                            ?.let { stateCodeColumnName = it }
                        if (sa4CodeColumnName !== null) {
                            val sa4Code = sa4Feature.getValueString(sa4CodeColumnName)
                            when {
                                sa4Code.startsWith("Migratory - Offshore - Shipping") ||
                                        sa4Code.startsWith("No usual address") ||
                                        sa4Code.startsWith("Outside Australia") ||
                                        sa4Code.startsWith("Unclassified")
                                -> false

                                else -> sa4Feature.geometry !== null && !sa4Feature.geometry.isEmpty
                            }
                        } else false
                    }.associateBy { it.getValueString(sa4CodeColumnName).toShortOrNull() }.filterNotNull()
                    .filterInvalidState(stateCodeColumnName)

                val sa3Features = absGeoPackage.getFeatureDao(sa3TableName).queryForAll()
                    .filter { sa3Feature ->
                        if (sa3CodeColumnName === null) sa3Feature.columnNames?.find { it.startsWith("SA3_CODE") }
                            ?.let { sa3CodeColumnName = it }
                        if (sa3NameColumnName === null) sa3Feature.columnNames?.find { it.startsWith("SA3_NAME") }
                            ?.let { sa3NameColumnName = it }
                        if (sa4CodeColumnName === null) sa3Feature.columnNames?.find { it.startsWith("SA4_CODE") }
                            ?.let { sa4CodeColumnName = it }
                        if (sa4NameColumnName === null) sa3Feature.columnNames?.find { it.startsWith("SA4_NAME") }
                            ?.let { sa4NameColumnName = it }
                        if (stateCodeColumnName === null) sa3Feature.columnNames?.find { it.startsWith("STATE_CODE") }
                            ?.let { stateCodeColumnName = it }
                        if (sa3CodeColumnName !== null) {
                            val sa3Code = sa3Feature.getValueString(sa3CodeColumnName)
                            when {
                                sa3Code.startsWith("Migratory - Offshore - Shipping") ||
                                        sa3Code.startsWith("No usual address") ||
                                        sa3Code.startsWith("Outside Australia") ||
                                        sa3Code.startsWith("Unclassified")
                                -> false

                                sa3Feature.getValueString(sa4CodeColumnName)
                                    .toShortOrNull() !in sa4Features.keys -> false

                                else -> sa3Feature.geometry !== null && !sa3Feature.geometry.isEmpty
                            }
                        } else false
                    }.associateBy { it.getValueString(sa3CodeColumnName).toIntOrNull() }.filterNotNull()
                    .filterInvalidState(stateCodeColumnName)

                val sa2Features = absGeoPackage.getFeatureDao(sa2TableName).queryForAll()
                    .filter { sa2Feature ->
                        if (sa2CodeColumnName === null) sa2Feature.columnNames?.find { it.startsWith("SA2_CODE") }
                            ?.let { sa2CodeColumnName = it }
                        if (sa2NameColumnName === null) sa2Feature.columnNames?.find { it.startsWith("SA2_NAME") }
                            ?.let { sa2NameColumnName = it }
                        if (sa3CodeColumnName === null) sa2Feature.columnNames?.find { it.startsWith("SA3_CODE") }
                            ?.let { sa3CodeColumnName = it }
                        if (sa3NameColumnName === null) sa2Feature.columnNames?.find { it.startsWith("SA3_NAME") }
                            ?.let { sa3NameColumnName = it }
                        if (sa4CodeColumnName === null) sa2Feature.columnNames?.find { it.startsWith("SA4_CODE") }
                            ?.let { sa4CodeColumnName = it }
                        if (sa4NameColumnName === null) sa2Feature.columnNames?.find { it.startsWith("SA4_NAME") }
                            ?.let { sa4NameColumnName = it }
                        if (stateCodeColumnName === null) sa2Feature.columnNames?.find { it.startsWith("STATE_CODE") }
                            ?.let { stateCodeColumnName = it }
                        if (sa2CodeColumnName !== null) {
                            val sa2Code = sa2Feature.getValueString(sa2CodeColumnName)
                            when {
                                sa2Code.startsWith("Migratory - Offshore - Shipping") ||
                                        sa2Code.startsWith("No usual address") ||
                                        sa2Code.startsWith("Outside Australia") ||
                                        sa2Code.startsWith("Unclassified")
                                -> false

                                sa2Feature.getValueString(sa4CodeColumnName)
                                    .toShortOrNull() !in sa4Features.keys -> false

                                sa2Feature.getValueString(sa3CodeColumnName).toIntOrNull() !in sa3Features.keys -> false
                                else -> sa2Feature.geometry !== null && !sa2Feature.geometry.isEmpty
                            }
                        } else false
                    }.associateBy { it.getValueString(sa2CodeColumnName).toIntOrNull() }.filterNotNull()
                    .filterInvalidState(stateCodeColumnName)

                val sa1Features = absGeoPackage.getFeatureDao(sa1TableName).queryForAll()
                    .filter { sa1Feature ->
                        if (sa1CodeColumnName === null) sa1Feature.columnNames?.find { it.startsWith("SA1_CODE") }
                            ?.let { sa1CodeColumnName = it }
                        if (sa2CodeColumnName === null) sa1Feature.columnNames?.find { it.startsWith("SA2_CODE") }
                            ?.let { sa2CodeColumnName = it }
                        if (sa3CodeColumnName === null) sa1Feature.columnNames?.find { it.startsWith("SA3_CODE") }
                            ?.let { sa3CodeColumnName = it }
                        if (sa4CodeColumnName === null) sa1Feature.columnNames?.find { it.startsWith("SA4_CODE") }
                            ?.let { sa4CodeColumnName = it }

                        if (sa2NameColumnName === null) sa1Feature.columnNames?.find { it.startsWith("SA2_NAME") }
                            ?.let { sa2NameColumnName = it }
                        if (sa3NameColumnName === null) sa1Feature.columnNames?.find { it.startsWith("SA3_NAME") }
                            ?.let { sa3NameColumnName = it }
                        if (sa4NameColumnName === null) sa1Feature.columnNames?.find { it.startsWith("SA4_NAME") }
                            ?.let { sa4NameColumnName = it }


                        if (stateCodeColumnName === null) sa1Feature.columnNames?.find { it.startsWith("STATE_CODE") }
                            ?.let { stateCodeColumnName = it }
                        if (sa1CodeColumnName !== null) {
                            val sa1Code = sa1Feature.getValueString(sa2CodeColumnName)
                            when {
                                sa1Code.startsWith("Migratory - Offshore - Shipping") ||
                                        sa1Code.startsWith("No usual address") ||
                                        sa1Code.startsWith("Outside Australia") ||
                                        sa1Code.startsWith("Unclassified")
                                -> false

                                sa1Feature.getValueString(sa4CodeColumnName)
                                    .toShortOrNull() !in sa4Features.keys -> false

                                sa1Feature.getValueString(sa3CodeColumnName).toIntOrNull() !in sa3Features.keys -> false
                                sa1Feature.getValueString(sa2CodeColumnName).toIntOrNull() !in sa2Features.keys -> false
                                else -> sa1Feature.geometry !== null && !sa1Feature.geometry.isEmpty
                            }
                        } else false
                    }.associateBy { it.getValueString(sa1CodeColumnName).toLongOrNull() }.filterNotNull()
                    .filterInvalidState(stateCodeColumnName)

                suspendedTransactionAsync(db = database) {
                    TransactionManager.current().connection.let { conn ->
                        // Add each SA4
                        sa4Features.processInParallel { sa4Feature ->
                            val statement = conn.prepareStatement(sa4InsertSql(), false)

                            statement.fillParameters(
                                listOf(
                                    ShortColumnType() to sa4Feature.key,
                                    VarCharColumnType() to sa4Feature.value.getValueString(sa4NameColumnName),
                                    VarCharColumnType() to sa4Feature.value.getValueString(stateCodeColumnName)
                                        .toInt().let(stateCodeMapping::get)?.name,
                                    BlobColumnType() to sa4Feature.value.geometry.wkb,
                                    IntegerColumnType() to sa4Feature.value.geometry.srsId
                                )
                            )

                            statement.executeUpdate()
                        }

                        // Add each SA3
                        sa3Features.processInParallel { sa3Feature ->
                            val statement = conn.prepareStatement(sa3InsertSql(), false)

                            statement.fillParameters(
                                listOf(
                                    IntegerColumnType() to sa3Feature.key,
                                    VarCharColumnType() to sa3Feature.value.getValueString(sa3NameColumnName),
                                    ShortColumnType() to sa3Feature.value.getValueString(sa4CodeColumnName)
                                        .toShort(),
                                    VarCharColumnType() to sa3Feature.value.getValueString(stateCodeColumnName)
                                        .toInt().let(stateCodeMapping::get)?.name,
                                    BlobColumnType() to sa3Feature.value.geometry.wkb,
                                    IntegerColumnType() to sa3Feature.value.geometry.srsId
                                )
                            )

                            statement.executeUpdate()
                        }

                        // Add each SA2
                        sa2Features.processInParallel { sa2Feature ->
                            val statement = conn.prepareStatement(sa2InsertSql(), false)

                            statement.fillParameters(
                                listOf(
                                    IntegerColumnType() to sa2Feature.key,
                                    VarCharColumnType() to sa2Feature.value.getValueString(sa2NameColumnName),
                                    IntegerColumnType() to sa2Feature.value.getValueString(sa3CodeColumnName)
                                        .toInt(),
                                    ShortColumnType() to sa2Feature.value.getValueString(sa4CodeColumnName)
                                        .toShort(),
                                    VarCharColumnType() to sa2Feature.value.getValueString(stateCodeColumnName)
                                        .toInt().let(stateCodeMapping::get)?.name,
                                    BlobColumnType() to sa2Feature.value.geometry.wkb,
                                    IntegerColumnType() to sa2Feature.value.geometry.srsId
                                )
                            )

                            statement.executeUpdate()
                        }

                        // Add each SA1
                        sa1Features.processInParallel { sa1Feature ->
                            val statement = conn.prepareStatement(sa1InsertSql(), false)

                            statement.fillParameters(
                                listOf(
                                    LongColumnType() to sa1Feature.key,
                                    IntegerColumnType() to sa1Feature.value.getValueString(sa2CodeColumnName)
                                        .toInt(),
                                    IntegerColumnType() to sa1Feature.value.getValueString(sa3CodeColumnName)
                                        .toInt(),
                                    ShortColumnType() to sa1Feature.value.getValueString(sa4CodeColumnName)
                                        .toShort(),
                                    VarCharColumnType() to sa1Feature.value.getValueString(stateCodeColumnName)
                                        .toInt().let(stateCodeMapping::get)?.name,
                                    BlobColumnType() to sa1Feature.value.geometry.wkb,
                                    IntegerColumnType() to sa1Feature.value.geometry.srsId
                                )
                            )

                            statement.executeUpdate()
                        }
                    }

                }.await()
            } finally {
                absGeoPackage.close()
            }
        } finally {
            absFile.delete()
        }
    }

    private fun sa1InsertSql(numberOfGeometries: Int = 1) = """
        WITH ins1 AS ( 
                    INSERT INTO sa1s (id, polygon, sa2, sa3, sa4, state)
                    VALUES (?, uuid_generate_v4(), ?, ?, ?, ?)
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

    private fun sa2InsertSql(numberOfGeometries: Int = 1) = """
        WITH ins1 AS ( 
                    INSERT INTO sa2s (id, name, polygon, sa3, sa4, state)
                    VALUES (?, ?, uuid_generate_v4(), ?, ?, ?)
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

    private fun sa3InsertSql(numberOfGeometries: Int = 1) = """
        WITH ins1 AS ( 
                    INSERT INTO sa3s (id, name, polygon, sa4, state)
                    VALUES (?, ?, uuid_generate_v4(), ?, ?)
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

    private fun sa4InsertSql(numberOfGeometries: Int = 1) = """
        WITH ins1 AS ( 
                    INSERT INTO sa4s (id, name, polygon, state)
                    VALUES (?, ?, uuid_generate_v4(), ?)
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
    private fun Map<*, FeatureRow>.filterInvalidState(stateCodeColumnName: String?) =
        this.filterValues { it.isStateValid(stateCodeColumnName) }

    private fun List<FeatureRow>.filterInvalidState(stateCodeColumnName: String?) =
        this.filter { it.isStateValid(stateCodeColumnName) }

    private fun FeatureRow.isStateValid(stateCodeColumnName: String?): Boolean =
        if (stateCodeColumnName !== null) {
            val stateCode = this.getValueString(stateCodeColumnName).toInt()
            stateCode in stateCodeMapping.keys
        } else false
}