package io.bouckaert.quicksa1.utility

import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import java.io.StringWriter
import java.sql.Connection

object SchemaUpdater {

    private fun createLiquibase (connection: Connection) =
        Liquibase(
            "db/changelog/changelog-master.xml",
            ClassLoaderResourceAccessor(),
            DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
        )

    fun update(connection: Connection, keepConnectionOpen: Boolean = false) {
        if (keepConnectionOpen) {
            createLiquibase(connection).update(Contexts())
        } else {
            connection.use { createLiquibase(it).update(Contexts()) }
        }
    }

    fun showSql(connection: Connection, keepConnectionOpen: Boolean = false): String =
        if (keepConnectionOpen) {
            val liquibase = createLiquibase(connection)
            val writer = StringWriter()
            liquibase.update(Contexts(), writer)
            println(liquibase.databaseChangeLog.toString())
            writer.toString()
        } else {
            connection.use {
                val liquibase = createLiquibase(it)
                val writer = StringWriter()
                liquibase.update(Contexts(), writer)
                println(liquibase.databaseChangeLog.toString())
                writer.toString()
            }
        }

}