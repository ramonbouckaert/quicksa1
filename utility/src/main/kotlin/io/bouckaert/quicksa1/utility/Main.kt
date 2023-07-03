package io.bouckaert.quicksa1.utility

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import io.bouckaert.quicksa1.shared.PDFRenderer
import io.bouckaert.quicksa1.utility.ingestor.ABSIngestor
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import java.io.FileOutputStream
import java.security.cert.X509Certificate
import java.sql.Connection
import java.util.*
import javax.net.ssl.X509TrustManager

object Main {

    private val config = ConfigLoaderBuilder.default().apply {
        addResourceSource("/config.json")
    }.build().loadConfigOrThrow<AppConfig.Config>()

    private val keyboard = Scanner(System.`in`)

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
        }
    }
    @JvmStatic
    fun main(args: Array<String>) {
        println("- QuickSA1 Command Line Utility -")

        val databaseUrl = if (config.database.url == null) {
            print("Please provide the JDBC connection string for your database: ")
            keyboard.nextLine()
        } else {
            config.database.url
        }

        val databaseDriver = if (config.database.driver == null) {
            print("Please provide the JDBC driver for your database: ")
            keyboard.nextLine()
        } else config.database.driver

        val database = getDatabase(databaseUrl, databaseDriver)
        println("Connecting to database...")
        try {
            val vendor = database.vendor
            val version = database.version
            println("Connected to $vendor $version")
        } catch (e: RuntimeException) {
            println("Failed to connect to database at: ${config.database.url}")
            throw e
        }

        print("Update database schema? (y/n): ")
        if (keyboard.nextLine().lowercase().startsWith("y")) {
            println("Updating database schema...")
            (database.connector().connection as Connection).use { SchemaUpdater.update(it) }
        }

        print("Ingest boundary data from ABS? (y/n): ")
        if (keyboard.nextLine().lowercase().startsWith("y")) {

            val absStructuresGeoPackageUrl = if (config.absStructuresGeoPackageUrl == null) {
                print("Please provide the URL for the ABS Main Structure & Greater Capital City Statistical Areas GeoPackage: ")
                keyboard.nextLine()
            } else config.absStructuresGeoPackageUrl

            println("Ingesting boundary data from ABS...")
            runBlocking {
                ABSIngestor(
                    database,
                    httpClient,
                    absStructuresGeoPackageUrl
                ).load()
            }
        }

        print("Produce test PDF? (y/n): ")
        if (keyboard.nextLine().lowercase().startsWith("y")) {
            val pdfOutputStream = runBlocking { PDFRenderer(database, ::println).renderPdf(80106106801) }
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

    private fun getDatabase(
        url: String,
        driver: String
    ) = Database.connect(url, driver)

}