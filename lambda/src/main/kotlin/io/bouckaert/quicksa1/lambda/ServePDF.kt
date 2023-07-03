package io.bouckaert.quicksa1.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import io.bouckaert.quicksa1.shared.PDFRenderer
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class ServePDF: RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    override fun handleRequest(input: APIGatewayProxyRequestEvent?, context: Context?): APIGatewayProxyResponseEvent {
        val logger = context?.logger ?: return APIGatewayProxyResponseEvent().apply {
            statusCode = 500
            body = "Could not retrieve logger"
        }

        if (input == null) return APIGatewayProxyResponseEvent().apply {
            statusCode = 500
            body = "No input to function"
        }

        val sa1 = input.pathParameters?.get("sa1")?.toLong() ?: return APIGatewayProxyResponseEvent().apply {
            statusCode = 400
            body = "No SA1 was requested"
        }

        val dbUrl = System.getenv("DB_URL")
        val dbDriver = System.getenv("DB_DRIVER")

        logger.log("Attempting to connect to $dbUrl using $dbDriver")

        val database = Database.connect(dbUrl, dbDriver)

        logger.log("Database instantiated")
        logger.log("Attempting to generate PDF for SA1 $sa1")

        val pdfOutputStream = runBlocking { PDFRenderer(database) { logger.log(it) }.renderPdf(sa1) }
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