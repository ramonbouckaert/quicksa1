package io.bouckaert.quicksa1.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import io.bouckaert.quicksa1.shared.PDFRenderer
import io.bouckaert.quicksa1.shared.PDFRendererDeps
import io.bouckaert.quicksa1.shared.SA1Searcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class ServePDF : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private val dbDriver = System.getenv("DB_DRIVER")
    private val dbUrl = System.getenv("DB_URL")
    private val deps = PDFRendererDeps()
    private var _database: Database? = null
    private val database: Database get() {
        if (_database != null && transaction {
                try {
                    !connection.isClosed
                } catch (e: Exception) {
                    false
                }
            }) {
            return _database as Database
        } else {
            _database = Database.connect(dbUrl, dbDriver)
            return _database as Database
        }
    }

    override fun handleRequest(input: APIGatewayProxyRequestEvent?, context: Context?): APIGatewayProxyResponseEvent {

        val logger = context?.logger ?: return APIGatewayProxyResponseEvent().apply {
            statusCode = 500
            body = "Could not retrieve logger"
        }

        if (input == null) return APIGatewayProxyResponseEvent().apply {
            statusCode = 500
            body = "No input to function"
        }

        val inputArg = input.pathParameters?.get("sa1") ?: return APIGatewayProxyResponseEvent().apply {
            statusCode = 400
            body = "No SA1 was requested"
        }

        val sa1 = try {
            inputArg.toLong()
        } catch (e: NumberFormatException) {
            val results = runBlocking { SA1Searcher(database) { logger.log(it) }.searchSA1s(inputArg) }
            return APIGatewayProxyResponseEvent().apply {
                statusCode = 200
                headers = mapOf(
                    "Content-Type" to "text/html"
                )
                body = """
                    <html><head><title>QuickSA1</title><head>
                    <body>
                    <ul>
                    ${results.map {
                        """
                        <li>
                        ${it.key} - <a href="https://quicksa1.com/${it.value}">https://quicksa1.com/${it.value}</a>
                        </li>
                        """.trimIndent()
                    }.joinToString("")}
                    </ul>
                    </body>
                    </html>
                """.trimIndent()
            }
        }

        logger.log("Attempting to generate PDF for SA1 $sa1")

        val pdfOutputStream = runBlocking {
            async(Dispatchers.Default) {
                PDFRenderer(deps, database) { logger.log(it) }.renderPdf(sa1)
            }.await()
        }
            ?: return APIGatewayProxyResponseEvent().apply {
                statusCode = 404
                body = "SA1 $sa1 could not be found"
            }

        logger.log("PDF Generated, resolving request")

        return APIGatewayProxyResponseEvent().apply {
            headers = mapOf(
                "Content-Type" to "application/pdf"
            )
            statusCode = 200
            body = Base64.encode(pdfOutputStream.toByteArray())
            isBase64Encoded = true
        }
    }

}