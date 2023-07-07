package io.bouckaert.quicksa1.utility

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import java.util.*

object Main {
    private val config: AppConfig.Config = ConfigLoaderBuilder.default().apply {
        addResourceSource("/config.json")
    }.build().loadConfigOrThrow<AppConfig.Config>()

    private val keyboard = Scanner(System.`in`)

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

        App(config, databaseUrl, databaseDriver, keyboard).run()
    }
}