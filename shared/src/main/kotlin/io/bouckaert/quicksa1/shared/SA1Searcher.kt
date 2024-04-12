package io.bouckaert.quicksa1.shared

import io.bouckaert.quicksa1.shared.db.ilike
import io.bouckaert.quicksa1.shared.db.tables.SA1s
import io.bouckaert.quicksa1.shared.db.tables.SA2s
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

class SA1Searcher(
    private val database: Database,
    private val log: (log: String) -> Unit
) {
    suspend fun searchSA1s(searchTerm: String): Map<String, Long> {
        log("Attempting to search for SA1s that match the search term")
        return suspendedTransactionAsync(db = database) {
            SA1s.leftJoin(SA2s).select {
                (SA2s.name ilike "%$searchTerm%")
            }
                .orderBy(SA1s.id)
                .groupBy { it[SA2s.name] }
                .entries
                .map {
                    it.value.mapIndexed { index, result -> "${result[SA2s.name]} ${index+1}" to result[SA1s.id].value }
                }.flatten().toMap()
        }.await()
    }
}