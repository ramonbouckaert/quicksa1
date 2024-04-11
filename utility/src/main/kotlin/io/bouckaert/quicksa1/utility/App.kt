package io.bouckaert.quicksa1.utility

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.bouckaert.quicksa1.shared.PDFRenderer
import io.bouckaert.quicksa1.utility.ingestor.ABSIngestor
import io.bouckaert.quicksa1.utility.ingestor.ACTIngestor
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import java.io.FileOutputStream
import java.security.cert.X509Certificate
import java.sql.Connection
import java.util.Scanner
import javax.net.ssl.X509TrustManager

class App(
    val config: AppConfig.Config,
    val dbUrl: String,
    val dbDriver: String,
    val keyboard: Scanner
) {
    private val hikariDataSource by lazy {
        HikariDataSource(HikariConfig().apply {
            jdbcUrl = dbUrl
            driverClassName = dbDriver
            maximumPoolSize = 64
            connectionTimeout = 120_000
            idleTimeout = 0
        })
    }
    private val database: Database get() = Database.connect(hikariDataSource)

    private val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            engine {
                requestTimeout = Long.MAX_VALUE
                https {
                    if (!config.httpClient.verifyCertificates) {
                        trustManager = object : X509TrustManager {
                            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                            override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                        }
                    }
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = config.httpClient.timeoutSeconds * 1000L
            }
            install(ContentNegotiation) {
                json()
            }
        }
    }
    fun run() {
        println("Connecting to database...")
        try {
            val vendor = database.vendor
            val version = database.version
            println("Connected to $vendor $version")
        } catch (e: RuntimeException) {
            println("Failed to connect to database at: $dbUrl")
            throw e
        }

        print("Update database schema? (y/n): ")
        if (keyboard.nextLine().lowercase().startsWith("y")) {
            println("Updating database schema...")
            (database.connector().connection as Connection).use { SchemaUpdater.update(it) }
        }

        print("Ingest boundary data from ABS? (y/n): ")
        if (keyboard.nextLine().lowercase().startsWith("y")) {

            val absStructuresGeoPackageUrl = if (config.abs?.absStructuresGeoPackageUrl == null) {
                print("Please provide the URL for the ABS Main Structure & Greater Capital City Statistical Areas GeoPackage: ")
                keyboard.nextLine()
            } else config.abs.absStructuresGeoPackageUrl

            println("Ingesting boundary data from ABS...")
            runBlocking {
                ABSIngestor(
                    hikariDataSource,
                    httpClient,
                    absStructuresGeoPackageUrl
                ).load()
            }
        }

        print("Ingest block and road data from ACT Government? (y/n): ")
        if (keyboard.nextLine().lowercase().startsWith("y")) {

            val blocksGeoJsonUrl = if (config.act.blocksGeoJsonUrl == null) {
                print("Please provide the URL for the ACT Blocks GeoJSON: ")
                keyboard.nextLine()
            } else config.act.blocksGeoJsonUrl

            val roadReservesGeoJsonUrl = if (config.act.roadReservesGeoJsonUrl == null) {
                print("Please provide the URL for the ACT Road Reserves GeoJSON: ")
                keyboard.nextLine()
            } else config.act.roadReservesGeoJsonUrl

            println("Ingesting block and road data from ACT Government...")
            runBlocking {
                ACTIngestor(
                    hikariDataSource,
                    httpClient,
                    blocksGeoJsonUrl,
                    roadReservesGeoJsonUrl
                ).load()
            }
        }

        print("Produce test PDF? (y/n): ")
        if (keyboard.nextLine().lowercase().startsWith("y")) {
            val pdfOutputStream = runBlocking { PDFRenderer(database, ::println).renderPdf(80105106006) }
            if (pdfOutputStream == null) {
                println("SA1 could not be found in the database")
                return
            }
            println("Saving PDF")
            FileOutputStream("output.pdf").apply {
                pdfOutputStream.writeTo(this)
            }.close()
        }
        println("All done!")
    }
}