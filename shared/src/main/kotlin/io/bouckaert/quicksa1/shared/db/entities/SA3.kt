package io.bouckaert.quicksa1.shared.db.entities

import io.bouckaert.quicksa1.shared.db.tables.SA1s
import io.bouckaert.quicksa1.shared.db.tables.SA2s
import io.bouckaert.quicksa1.shared.db.tables.SA3s
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SA3(id: EntityID<Int>): Entity<Int>(id) {
    companion object : EntityClass<Int, SA3>(SA3s)
    var name by SA3s.name
    var polygon by MultiPolygon referencedOn SA3s.polygon
    val sa1s by SA1 referrersOn SA1s.sa3
    val sa2s by SA2 referrersOn SA2s.sa3
    var sa4 by SA4 referencedOn SA3s.sa4
}