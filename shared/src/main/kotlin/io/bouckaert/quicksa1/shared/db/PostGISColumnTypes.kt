package io.bouckaert.quicksa1.db

import net.postgis.jdbc.PGbox2d
import net.postgis.jdbc.PGgeometry
import net.postgis.jdbc.geometry.MultiPolygon
import org.jetbrains.exposed.sql.*

fun Table.multiPolygon(name: String, srid: Int = 3857): Column<MultiPolygon>
        = registerColumn(name, MultiPolygonColumnType(srid))

infix fun ExpressionWithColumnType<*>.within(box: PGbox2d) : Op<Boolean>
        = WithinOp(box)

private class MultiPolygonColumnType(val srid: Int = 3857): ColumnType() {
    override fun sqlType() = "GEOMETRY(MultiPolygon, $srid)"
    override fun valueFromDB(value: Any): Any = if (value is PGgeometry) value.geometry else value
    override fun notNullValueToDB(value: Any): Any {
        if (value is MultiPolygon) {
            if (value.srid == MultiPolygon.UNKNOWN_SRID) value.srid = srid
            return PGgeometry(value)
        }
        return value
    }

    override fun valueToDB(value: Any?): Any? {
        if (value is MultiPolygon) {
            if (value.srid == MultiPolygon.UNKNOWN_SRID) value.srid = srid
            return PGgeometry(value)
        }
        return value
    }
}

private class WithinOp(val box: PGbox2d) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        append("&& ST_MakeEnvelope(${box.llb.x}, ${box.llb.y}, ${box.urt.x}, ${box.urt.y}, 3857)")
    }
}
