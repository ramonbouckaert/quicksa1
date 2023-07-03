package io.bouckaert.quicksa1.shared.db.entities

import io.bouckaert.quicksa1.shared.db.tables.SA1s
import io.bouckaert.quicksa1.shared.db.tables.SA2s
import io.bouckaert.quicksa1.shared.db.tables.SA3s
import io.bouckaert.quicksa1.shared.db.tables.SA4s
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SA4(id: EntityID<Short>): Entity<Short>(id) {
    companion object : EntityClass<Short, SA4>(SA4s)
    var name by SA4s.name
    var polygon by MultiPolygon referencedOn SA4s.polygon
    val sa1s by SA1 referrersOn SA1s.sa4
    val sa2s by SA2 referrersOn SA2s.sa4
    val sa3s by SA3 referrersOn SA3s.sa4
}